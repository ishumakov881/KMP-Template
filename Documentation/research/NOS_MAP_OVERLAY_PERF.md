# NOS карта: почему веб не фризит, а osmdroid — да

Дата: **2026-11-23**  
Контекст замера: offline-cache **off**, все слои легенды **on** → Main `canvas REBUILD` 376–512 ms, scroll после загрузки gfx **100%** janky (50p~150 ms).  
Веб-эталон: `https://nos.lds.online/js/map_osm.js` (сессия; в репо не кладём).

## Короткий ответ на вопросы

| Вопрос | Ответ |
|--------|--------|
| Карта пересоздаётся? | **Нет.** `MapView` в `remember { }`, живёт весь экран. |
| Маркеры в том же потоке, что карта? | **Да.** `AndroidView.update` на **Main**: wipe + `Marker(view)` ×N + `Polyline(view)` ×M + `invalidate()`. |
| Можно готовить слои в фоне? | **Частично.** Данные/`GeoPoint`/списки — да (`Default`/`IO`). Трогать `MapView.overlays` / `invalidate` — **только Main**. `Marker(mapView)` завязан на View; полностью «собрать Marker off-thread» почти нельзя. Имеет смысл собирать **лёгкие Overlay без Context** (FolderOverlay / custom draw) в фоне и **одним** `add` на Main (паттерн osmdroid PR #373). |
| Почему на вебе «бага нет»? | Другая модель: **инкремент по type в LayerGroup**, mute/unmute через `addTo`/`removeFrom`, **`preferCanvas: true`** для circle/polyline; нет полного wipe всех иконок на каждый пришедший type. |

Room/сеть тут уже не главный виновник (с кэшем off `applyTypeBatch UI` ~5–100 ms).

---

## Что делает натив сейчас

Файл: `NosMapCanvas.android.kt`.

1. Progressive markgen обновляет `markers`/`lines` → Compose `update`.
2. Если видимый набор изменился → **удалить все** `Marker`+`Polyline` → заново создать **весь** текущий набор.
3. Муфты (`mft`) уже вынесены в один `MftCirclesOverlay` (canvas + viewport cull) — паритет веб `L.circleMarker`.
4. Остальное: ~**4.8k** тяжёлых `org.osmdroid.views.overlay.Marker` + ~**3.2k** отдельных `Polyline`.

Итог при «все слои»: несколько подряд full-REBUILD на Main (замер: 376 → 397 → **512 ms**), потом каждый кадр жеста гоняет тысячи Overlay → scroll мёртвый.

---

## Что делает веб (`map_osm.js`)

1. На старте: `L.map(..., { preferCanvas: true })`.
2. Постоянные **`L.layerGroup()`** на каждый legend-layer (`layer_ok`, `layer_pon_mft`, `layer_line`, …).
3. Ajax `markgen` по type → в `success` **только дописывает** в свою группу:
   - иконки: `L.marker(...).addTo(window[layer])`;
   - муфты: `L.circleMarker(...).addTo(layer_pon_mft)` (canvas renderer);
   - связи: `L.polyline(...).addTo(layer_line)`.
4. Чекбокс легенды: `layer.addTo(map)` / `layer.removeFrom(map)` — **не** пересоздаёт маркеры.
5. Полный wipe (`deleteOverlays` + новые Group) — только в **`Refresh()`**, не на каждый type.

Карта Leaflet не пересоздаётся; тайлы живут отдельно; canvas-векторы дешевле DOM-иконок при pan.

Веб тоже тянет ~19k объектов без clustering — на десктоп-Chrome это терпимо. Натив платит дороже за **N× Overlay.Marker** и за **full rebuild**.

---

## Узкие места (приоритет)

1. **Full wipe+rebuild** всех Marker/Polyline при каждом изменении видимого набора (progressive × число type).
2. **Тяжёлый Marker** как единица (hit-test, icon Drawable, overlays list) × ~5k — и при **создании**, и при **каждом draw/gesture**.
3. **~3266 Polyline** как отдельные Overlay (на вебе polyline тоже много, но canvas path + layerGroup; у нас — N объектов в `overlays`).
4. (Уже смягчено) mft 14k — `MftCirclesOverlay`; без него было бы хуже.

Фон для Room/JSON **не лечит** п.1–3.

---

## Направления (от дешёвого к системному)

### A. Инкремент как веб (FolderOverlay / слой на type или legend-layer) — P0

- Не трогать уже построенные слои, если пришёл другой type.
- Заменить слой целиком только для изменившегося type/layer.
- Toggle легенды = `overlays.add/remove` готового FolderOverlay, без recreate Marker.

Ожидание: убрать серию 300–500 ms REBUILD на старте; refresh одного type дешевле.

### B. Один custom Overlay для иконок (как `MftCirclesOverlay`) — P0/P1 для scroll

- Рисуем PNG/bitmap в `draw()` + viewport cull + hit-test.
- Osmdroid `SimpleFastPointOverlay` — до ~100k, но **один стиль**; у нас много иконок → свой multi-icon overlay или несколько (по icon url).
- Создание списка точек — в фоне; на Main только `items = …` + `invalidate()`.

Ожидание: scroll после «все слои» ближе к вебу / к режиму «только муфты».

**Клики (U30):** несколько Overlay друг на друге ≠ «верхний слой глотает все тапы».  
Osmdroid идёт top→bottom, пока кто-то не вернёт `true`. Поэтому: lines никогда не `true`; miss → `false`; при оверлапе точек — **общий hit-router** (ближайший среди иконок+mft в hitSlop, tie → z-order), а не отдельный «съел слой целиком».

### C. Один Overlay для линий — P1

- Вместо 3266 `Polyline`: один path/draw сегментов (уже пробовали `LinkLinesOverlay` — UX/сброс был по другой причине; к draw-модели можно вернуться точечно).

### D. «Готовить в фоне» — уточнение

| Можно off Main | Только Main |
|----------------|-------------|
| Парс markgen, filter, GeoPoint[], icon decode в cache | `MapView.overlays.*`, `Marker(mapView)`, `invalidate()` |
| Сборка plain `FolderOverlay` / custom Overlay **без** Context (osmdroid) | Любой touch View hierarchy |
| Свап ссылки `overlay.items = prepared` | |

Чанковый drip Marker на Main пользователь уже отвергал как UX; фон+atomic swap слоя — другой контракт.

### E. Clustering / bbox API — позже

На вебе clustering тоже нет. Имеет смысл после A/B, или с бэком (см. `NOS_MARKGEN_SERVER_OPTIMIZATION.md`).

---

## Связанные файлы

- Натив: `NosMapCanvas.android.kt`, `MftCirclesOverlay.kt`, `MapViewModel.kt`
- Веб: `/js/map_osm.js` (live)
- Замеры: checklist **U28**; Room-конспект: Project Brain `NOS/knowledge/room-offline-markers.md`
- osmdroid: `SimpleFastPointOverlay`, PR #373 (async FolderOverlay без Context)

## Вердикт

Думать нужно не «пересоздать карту» и не «унести Marker целиком в фон», а:

1. **перестать full-rebuild** (паритет LayerGroup веба);
2. **уйти от тысяч `Marker`/`Polyline` Overlay** к canvas/custom overlays с cull — иначе scroll с полной легендой останется плохим даже после инкремента.
