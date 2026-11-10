# NOS markgen — бриф для оптимизации сервера

Дата замера: **2026-11-10**  
Endpoint: `POST https://nos.lds.online/ajax/ajaxmap.php` (`mode=markgen&type=…`)  
UI-потребители: веб `/osm` (`map_osm.js`) и нативное приложение NOS (KMP).

Цель файла: что отдать владельцам бэка NOS, чтобы урезать трафик и время первой загрузки карты.

---

## Факт по сети (первая загрузка карты)

При открытии карты клиент делает **9 параллельных** запросов `markgen` (по одному `type`).  
Checkbox слоёв на вебе/в нативе сейчас только show/hide — **все type всё равно качаются**.

| type | Объектов (замер) | Размер ответа ≈ | Комментарий |
|------|------------------|-----------------|-------------|
| `mft` | **14048** | **~8.1 MB** | Главный раздув |
| `sw` | 3351 | **~5.5 MB** | Второй по весу (тяжёлый `popupContent`) |
| `wifi` | 781 | ~0.6 MB | |
| `line` | 3270 | ~0.2 MB | Линии: `lat1,lng1,lat2,lng2` (не point) |
| `usil` | 268 | ~0.2 MB | |
| `ktv_amp` | 176 | ~0.2 MB | |
| `uzel` | 118 | ~0.2 MB | |
| `pon_new` | 130 | ~0.2 MB | |
| `mft_line` | 10 | ~1 KB | |

**Итого ≈ 19k объектов, суммарно JSON ≈ 15 MB** (без тайлов `maps.lds.online`).  
Это не «9 МБ на 9 запросов» — почти весь объём дают **`mft` + `sw`**.

`Content-Type` ответа сейчас `text/html; charset=UTF-8` при JSON-теле — мелочь, но лучше `application/json`.

Полная разведка API: `Documentation/research/NOS_OSM_API_RECON.md`.

---

## Почему так тяжело

В каждом элементе markgen часто есть поле **`popupContent`** — готовый HTML попапа Leaflet (ссылки snmp/узел, ping attrs, maintenance, адрес и т.д.), сотни–1000+ символов на точку.

Пример полей точки:

```json
{
  "name": "...",
  "lat": "...",
  "lng": "...",
  "state": 0,
  "layer": "layer_dn",
  "icon": "/images/map/mm_20_red.png",
  "popupContent": "<div class=\"infowindow\">…длинный HTML…</div>"
}
```

Натив уже **парсит HTML → lean-поля и выбрасывает HTML** из UI-стейта, но **по сети всё равно качает полный dump**. Без правки бэка клиент не сэкономит мегабайты.

Refresh веба/натива = снова **полный** markgen всех type (delta API нет).

---

## Что просим с сервера (приоритет)

### P0 — lean list без HTML (максимальный выигрыш)

`mode=markgen` (или новый `mode=markgen_lean`) отдаёт только:

- `id` (где есть; у MFT уже есть)
- `name`, `lat`, `lng`, `state`, `layer`, `icon`, `type` (или type только query)
- для устройств: `ip`, `pingType` (`t`), `siteId`, `place`, `model`, `serial`, `mac`, `maintenanceId`, флаги техработ

**Без `popupContent`.**  
Детали попапа — отдельный endpoint, например:

- `mode=marker_detail&type=sw&ip=…` или `id=…`
- ответ: те же поля + опционально ссылки SNMP/Zabbix/узел

Ожидаемый эффект: `sw`/`mft` должны сжаться на порядок (грубо: с мегабайтов HTML до компактных полей).

### P1 — не отдавать выключенные слои «за пользователя»

Контракт: клиент запрашивает только нужные `type` (уже умеет).  
Опционально серверный default «лёгкий старт»: без `mft` до явного include (MFT ~половина трафика).

### P2 — bbox / viewport

`mode=markgen&type=…&bbox=minLng,minLat,maxLng,maxLat` (+ zoom).  
Особенно критично для `mft` (~14k).

### P3 — delta / ETag

- `If-None-Match` / `since=unix` → 304 или только изменившиеся точки
- иначе каждый refresh = снова ~15 MB

### P4 — линии отдельно

`line` / `mft_line` — не маскировать под point-маркеры; явная схема polyline (`lat1,lng1,lat2,lng2` или `coordinates[]`).

### P5 — мелочи транспорта

- `Content-Type: application/json`
- gzip/br (если ещё не везде)
- стабильный numeric `id` у `sw`/`wifi`/… (сейчас часто только name/ip из HTML)

---

## Что клиент уже делает / не решает сеть

| Клиент (натив) | Сеть |
|----------------|------|
| Parallel `async` на 9 type | Всё равно ~15 MB |
| Parse `popupContent` → lean, HTML drop | Скачали HTML зря |
| Checkbox слоёв без re-fetch | Данные уже в RAM |
| VM переживает смену tab | Не уменьшает первый hit |

Полный выигрыш по трафику — **только с бэка** (P0+).

---

## Критерий успеха (для бэка)

1. Холодный старт карты (все нужные слои): **≪ 15 MB** (цель ориентир: **&lt; 1–2 MB** lean JSON без MFT-HTML, или &lt; 3–4 MB со всеми type lean).
2. Клик по маркеру: один лёгкий detail-запрос, не повтор markgen.
3. Refresh интервала 5 мин не гоняет полный HTML-dump без изменений (304/delta).

---

## Контакты по замерам

Повторить замер (авторизованная сессия):

```text
POST /ajax/ajaxmap.php
Content-Type: application/x-www-form-urlencoded
mode=markgen&type=<sw|uzel|pon_new|mft|wifi|usil|ktv_amp|line|mft_line>
```

Сравнить `Content-Length` / размер body до и после вырезания `popupContent`.
