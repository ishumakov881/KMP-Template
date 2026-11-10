# NOS OSM — разведка API (2026-11-10)

Целевой UI: `https://nos.lds.online/osm`  
Тайлы: `https://maps.lds.online/hot/{z}/{x}/{y}.png` (отдельный сервис «LDS OSM»)  
Клиент карты: Leaflet (`/js/leaflet.js` + `/js/map_osm.js`), без MarkerCluster.

Авторизация: форма `/login?next=/osm` → cookie `PHPSESSID` (session). Без сессии карта/ajax недоступны.

**Нативный login возможен:** тот же POST логин/пароль + CookieJar (OkHttp) → дальше все ajax с этой cookie. Это не «нет нативной авторизации», а «нет token API» (JWT/OAuth). WebView для входа не обязателен.

Проверено (curl, 2026-11-10):

- `POST /login` form: `login`, `password`, `next` (hidden, напр. `/osm`)
- Ответ выставляет `Set-Cookie: PHPSESSID=...; path=/`
- С этой cookie `POST /ajax/ajaxmap.php` `mode=get_count` → JSON `sw_ok` / `sw_dn` / …
- В приложении: `NosAuthRepository` + Ktor `HttpCookies` / `AcceptAllCookiesStorage`

---

## Главный endpoint данных

`POST https://nos.lds.online/ajax/ajaxmap.php`  
`Content-Type: application/x-www-form-urlencoded`  
`X-Requested-With: XMLHttpRequest`  
Тело: `mode=...&type=...` (+ поля для ping/maintenance)

Ответ: JSON-массив, но `Content-Type: text/html; charset=UTF-8`.  
В маркерах поле `popupContent` — **готовый HTML** (ссылки на snmp/zabbix, ping, maintenance), не структурированные поля.

### Modes (из `/js/map_osm.js`)

| mode | Назначение |
|------|------------|
| `markgen` | Выгрузка маркеров/линий по `type` |
| `get_count` | Счётчики свичей по статусам |
| `ajaxping` | Ping устройства (`ip`, `t`) |
| `maintenance` | Техработы (`id`, `state`, `message`) |
| `getimage_url` | URL картинок для MFT (`id`) |
| `getimage` | Отдача изображения (`src`) |

### markgen types (загрузка при старте `/osm`)

Страница **сразу** дергает все типы (слои checkbox только показывают/прячут уже созданные LayerGroup):

| type | Объектов (замер) | Размер ответа ≈ | Назначение |
|------|------------------|-----------------|------------|
| `sw` | 3351 | ~5.5 MB | Коммутаторы |
| `uzel` | 118 | ~0.2 MB | Узлы |
| `pon_new` | 130 | ~0.2 MB | PON OLT |
| `mft` | **14048** | **~8.1 MB** | MFT / сплиттеры / муфты |
| `wifi` | 781 | ~0.6 MB | Wi‑Fi AP |
| `usil` | 268 | ~0.2 MB | КТВ усилители |
| `ktv_amp` | 176 | ~0.2 MB | КТВ amp |
| `line` | 3270 | ~0.2 MB | Линии связей (`lat1,lng1,lat2,lng2`) |
| `mft_line` | 10 | ~1 KB | Линии MFT |

**Итого маркеров ≈ 19k**, суммарно JSON ≈ **15 MB** (без тайлов), плюс тысячи DOM-маркеров Leaflet.

`get_count` пример:  
`{"sw_ok":"576","sw_dn":"2723","sw_ld":"9","sw_ds":"0","sw_nv":"52","sw_mc":"10"}`

### Схема маркера (типично)

```json
{
  "name": "...",
  "lat": "48.55...",
  "lng": "39.38...",
  "state": 0,
  "layer": "layer_dn",
  "color": "#ff1100",
  "icon": "/images/map/mm_20_red.png",
  "zindex": 9999,
  "popupContent": "<div class=\"infowindow\">...</div>"
}
```

MFT дополнительно: `id`, `child`, `plc`, `ip`.

Слои: `layer_ok|ld|dn|nv`, `layer_uzel_*`, `layer_pon_*`, `layer_pon_mft`, `layer_wifi_*`, `layer_usil_*`, `layer_amp_*`, `layer_line`, `layer_mft_line`.

---

## Тайлы / другие подложки

- OSM (по умолчанию): `https://maps.lds.online/hot/{z}/{x}/{y}.png`
- Yandex: `https://core-renderer-tiles.maps.yandex.net/tiles?...`
- Google: `https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}`

`maps.lds.online` сам по себе — публичный Leaflet viewer тайлов (без NOS-маркеров).

Связанные UI NOS: `/sw`, `/map` (вероятно Google-era), `/profile`, `/logout.php`.

---

## Почему тормозит на мобильном

1. При открытии `/osm` грузятся **все** типы сразу (~15 MB JSON), даже если checkbox слоёв выключены.
2. `popupContent` раздувает payload (сотни–1000+ символов HTML на точку).
3. Leaflet `L.marker` = DOM; `preferCanvas: true` помогает линиям/circle, но не тысячам иконок.
4. **Нет clustering** / viewport-load / pagination.
5. WebView умножает проблему (память + JS-поток).

---

## Нужен ли доступ к репо NOS?

| Задача | Достаточно сетевой разведки? | Репо NOS желателен? |
|--------|------------------------------|---------------------|
| Нативная карта + маркеры lat/lng/icon/layer | Да (ajaxmap + session login) | Нет для прототипа |
| Понять SQL/кэш markgen, урезать API | Нет | **Да** |
| Чистый REST без HTML popup | Нет (сейчас HTML с бэка) | **Да** (или договорённость с бэком) |
| Offline Room + Worker | Частично (схема полей известна) | Да для инкрементального sync / ETag / delta |
| Auth (token вместо PHPSESSID) | Сейчас только cookie session | **Да** / отдельный API-шлюз |

**Вердикт:** для spike «нативный OSM SDK + точки» репо **не обязателен** — хватает `ajaxmap.php` + login cookie.  
Для продакшен-натива (offline, lean JSON, нормальный auth, без HTML) — **доступ к репо NOS (или контракт с владельцами API) сильно желателен**, иначе придётся парсить HTML popup и жить на хрупком PHP session.

---

## Статусы / группы и Refresh

Группы в UI (`wifi_ok` / `wifi_dn`, `layer_ok` / `layer_dn`, …) — это не разные объекты, а поле **`layer`** (и связанные `state`/`icon`), которое **сервер пересчитывает при генерации** `markgen`. Один и тот же AP со временем может прийти как `layer_wifi_ok`, потом как `layer_wifi_dn`.

Как обновляет **текущий веб**:

1. Интервал по умолчанию **300000 ms** (5 мин), в меню 10с / 1м / 5м.
2. `Refresh()` → `deleteOverlays()` (сносит все LayerGroup) → снова **полный** `returnSw/Uzel/Pon/Mft/Wifi/...` (все type целиком).
3. Точечного «один маркер изменился» API **нет** — только полный снимок типа.

Для натива + Room:

- Хранить **одну строку на устройство** (ключ: type + стабильный id/name/ip — у MFT есть `id`, у других часто только name/ip из данных).
- `layer`/`state`/`icon` — обновляемые колонки; чекбоксы групп = `WHERE layer = ...`.
- Пока бэк такой: синк = скачать markgen по type → **upsert** в Room (не «пересчитать весь мир в одном JSON» локально). Глобальный JSON — артефакт транспорта, не модель БД.
- Идеал позже: delta/since или push статусов — без full dump.

---

## Согласованные первые шаги оптимизации

1. **Урезать JSON** — убрать `popupContent` из list-ответа (или срезать на клиенте сразу после parse). Детали/HTML — отдельным запросом по клику (`id` / ip).
2. **Грузить по слоям** — не дергать все `type` при старте; запрос только для включённого checkbox (сейчас клиент грузит всё, а checkbox лишь show/hide).
3. Дальше (сильнее): bbox/viewport, clustering, Room-кэш lean-полей.

Клиентский workaround без бэка: (2) легко в `map_osm.js` / нативе; (1) частично — не качать меньше с сервера, но меньше парсить/хранить в памяти, если бэк всё же шлёт HTML. Полный выигрыш по сети — только если бэк перестанет отдавать `popupContent` в markgen.

---

## Смысл натива vs WebView

- **WebView сырой `/osm`:** минимальный путь, наследует все тормоза (15 MB + 19k markers).
- **Натив (osmdroid / MapLibre + clustering + Room):** имеет смысл, если:
  - грузить по bbox / по включённым слоям;
  - хранить lean-поля без HTML;
  - кластеризовать MFT+SW;
  - кэшировать тайлы `maps.lds.online`.
- Идеально: бэк отдаёт compact JSON (`id,name,lat,lng,state,layer,icon,ip,...`) + details endpoint по клику.

---

## Безопасность

Учётные данные входа **не хранить** в репозитории. Session cookie живёт в браузере; для натива нужна явная стратегия auth.
