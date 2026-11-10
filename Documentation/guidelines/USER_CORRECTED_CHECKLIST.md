# User-corrected checklist (verification)

> **Для ИИ-ревьюера:** проверь проект по пунктам ниже. Каждый пункт — реальное требование или замечание пользователя, которое уже должно быть реализовано.
> **Для агента-разработчика:** после каждого требования или замечания пользователя — добавь или обнови пункт здесь **в том же ходе**. Перед правками прочитай весь файл.
> **Для восстановления:** разделы идут в порядке сборки проекта с нуля (P → A → U → D → B → R).

Формат пункта:

    - [ ] ID — что должно быть
      Verify: где смотреть / ожидаемое поведение

Отмечай `[x]` только после проверки в коде/на устройстве.

---

## P. Продукт

- [x] P1 — Исследование сервиса `https://nos.lds.online/osm` (карта OSM через `https://maps.lds.online`): зафиксированы используемые HTTP/API endpoints, форматы данных маркеров/точек и вывод, нужен ли доступ к репозиторию NOS для нативной интеграции.
  Verify: `Documentation/research/NOS_OSM_API_RECON.md` содержит endpoints (`/ajax/ajaxmap.php` modes/types, тайлы `maps.lds.online/hot/...`), объёмы markgen и вердикт по репо; секреты входа не хранятся в репозитории.
- [x] P6 — Бриф оптимизации сервера markgen: объёмы 9 type (**≈15 MB**, не 9; лидеры `mft`~8.1 + `sw`~5.5), причина (`popupContent` HTML), приоритеты P0–P5 (lean list, detail endpoint, bbox, delta) — файл для передачи бэку.
  Verify: есть `Documentation/research/NOS_MARKGEN_SERVER_OPTIMIZATION.md` с таблицей размеров и чеклистом серверных изменений.
- [x] P2 — Нативный Android UI карты поверх NOS API (удобнее WebView `/osm`): login + `PHPSESSID`, тайлы `maps.lds.online` через **osmdroid**, lean markgen полного набора веб-`map_osm.js` (`DEFAULT_MAP_TYPES`: sw, uzel, pon_new, mft, wifi, usil, ktv_amp, line, mft_line) на общем CookieJar; без WebView `/osm`.
  Verify: tab «Карта»; osmdroid + LDS tiles; легенда Коммутаторы/Узлы/PON/КТВ/WIFI; типы = все loaders из `map_osm.js`, не ручной subset.
  Изм. 2026-11-10: убрали ручные «урезанные» списки — полный паритет markgen types с вебом.
  Изм. 2026-11-10: зафиксирован osmdroid + expect/actual (не MapLibre, не KMP-osmdroid).
- [x] P3 — Нативная авторизация = session-login как у веба: GET `/login` → POST `login`/`password`/`next`/`submit-login` → успех только при **302 на next** (или страница карты без формы входа); cookie `PHPSESSID` в Ktor CookieJar. Не считать успехом выдачу PHPSESSID и не использовать `get_count` как proof (он публичный). Неверный пароль не пускает в приложение.
  Verify: `NosAuthRepository.login` бросает на «Неверное имя…» / форме входа; после успеха открывается `/osm` без redirect на login; wrong-password тест/ручная проверка не вызывает `onAuthenticated`.
  Изм. 2026-11-10: было → успех по PHPSESSID+`get_count` (ложный пропуск) → стало → web 302/страница карты.
- [ ] P5 — Сессия переживает перезапуск приложения: `PHPSESSID` в russhwolf через универсальный `AppPreferences` (ключ `AppPreferences.KEY_PHPSESSID` = `pref_nos_phpsessid`); backend prefs — `SharedPreferencesSettings` файл `nos` (тот же instance, что у `ThemeSettings`); при старте `restoreSession` + проверка `/osm`; протухшая сессия — `remove(KEY_PHPSESSID)` и снова login. Preview — `AppPreferences(InMemorySettings())`.
  Verify: `MainActivity` → `AppPreferences(prefs)`; нет класса `NosSessionStore`; войти → убить процесс → открыть снова без формы входа (пока серверная сессия жива); после clear prefs или invalid cookie — login снова.
  Изм. 2026-11-10: было `NosSessionStore` → стало `AppPreferences` + ключ в companion (паттерн ldsonline).
- [ ] P4 — Первый продуктовый экран NOS на compose-cupertino (кит из `Начальный промпт.md`): форма авторизации только API Cupertino (section/textField/button/dialog/activityIndicator), без Material.
  Verify: `LoginScreen.kt` использует Cupertino + `ScreenList`; нет material3 виджетов на экране входа; сборка `:app:shared:compileKotlinJvm` ок; on-device smoke login UI.

## A. Архитектура

- [x] A1 — Payload маркеров lean: в UI-стейте нет сырого HTML `popupContent`; при parse HTML разбирается в поля `LeanMapMarker` (ip, pingType, siteId, place, model, maintenanceId, …) и отбрасывается.
  Verify: `parsePopupFields` / `LeanMapMarker.ip` и др.; в `MapUiData.markers` нет строки HTML попапа.
  Изм. 2026-11-10: «детали позже» → structured extract из popup HTML при markgen.
- [x] A2 — Загрузка карты = **полный** набор markgen из веб `map_osm.js` (`NosMapRepository.DEFAULT_MAP_TYPES`: sw, uzel, pon_new, mft, wifi, usil, ktv_amp, line, mft_line). Список сверять с JS, не выкидывать PON/MFT «для удобства» без явной просьбы. Checkbox/bbox — дальше.
  Verify: `DEFAULT_MAP_TYPES` содержит все 9 type; `MapViewModel` → `markgenMany(DEFAULT_MAP_TYPES)`; легенда включает PON; `markgenTypesInLegend` покрывает эти type.
  Изм. 2026-11-10: subset без PON → полный паритет с `map_osm.js`.
- [ ] A3 — В проекте есть UDF как в ldsonline: модуль `:kmp:udf` (`BaseViewModel`, `UiState`, `ScreenState`, `BaseScreenLayout` / effects) и зависимости `:kmp:network` (`ApiException`), `:kmp:ldscore` (notifications enums).
  Verify: `settings.gradle.kts` include `:kmp:udf`; `app/shared` зависит от `:kmp:udf`; login/экраны наследуют `BaseViewModel`.
- [x] A4 — Prefs: один универсальный `AppPreferences(Settings)` с generic get/put/remove и ключами в `companion`; не плодить доменные одноключевые `*SessionStore` / `*Store`. In-memory для Preview/тестов — `InMemorySettings`, не `MapSettings` на проде.
  Verify: есть `data/AppPreferences.kt` и `data/InMemorySettings.kt`; нет `NosSessionStore.kt`; call sites (`App`, `LoginViewModel`, `MainActivity`) используют `AppPreferences` + `KEY_*`.

## U. UI / UX

- [ ] U1 — Responsive layout-токены из ldsonline (`:kmp:ui`: `WindowWidth`, `WebViewportBp`, auth metrics); визуал приложения и login — **Cupertino** (не Material UI с референса). Форма авторизации масштабируется phone/tablet portrait/landscape.
  Verify: `LoginScreen` на Cupertino (`CupertinoSection`/`CupertinoButton`/`CupertinoText`); `:kmp:udf` ErrorView/loading без Material3; на ≥768 form+aside row, &lt;480 кнопка full width.
- [ ] U2 — Не копировать Material/Compose UI-скин с ldsonline; источник виджетов — Cupertino в `:app:shared` / `:kmp:udf`.
  Verify: в login и udf UI нет `material3` Button/TextField/OutlinedButton; `app/shared` не тянет `compose-material3` ради экранов.
- [x] U3 — Веб-«Профиль» NOS (= тема) не дублировать отдельным bottom-tab: настройки уже в тулбаре. В `SettingsScreen` активны блоки «Оформление» и «О приложении»; демо-«Уведомления» закомментированы (не удалены).
  Verify: открыть шестерёнку в тулбаре — видны Оформление + О приложении, секции уведомлений нет; закомментированный блок «Уведомления» остаётся в `SettingsScreen.kt`.
- [x] U4 — Bottom nav: tabs **Карта** (`MainTab.Map`, дефолт), **Свичи** (`MainTab.Switches`, markgen), **Узлы** (`MainTab.Sites`, веб `/sw` left menu). «Профиль» / демо Desktop/Requests убраны из bar (файлы экранов не удалять, entries закомментированы). Тема — Settings overlay.
  Verify: bottom bar показывает «Карта», «Свичи», «Узлы»; нет «Профиль»/«Рабочий стол»/«Заявки»; `ProfileScreen.kt` / `DesktopScreen.kt` / `RequestsScreen.kt` на месте; `MainTab` entries в `CorporateShell.kt`.
  Изм. 2026-11-10: добавлен tab Карта; демо Desktop/Requests тоже закомментированы в nav.
  Изм. 2026-11-10: добавлен tab «Свичи» (markgen).
  Изм. 2026-11-10: добавлен tab «Узлы» (веб `#s_sw`); markgen-tab **Свичи** не удалять.
- [x] U9 — Экран **Свичи** (оставить): список lean markgen `type=sw` (`NosMapRepository.markgen(TYPE_SW)`); фильтр сегментами Все/OK/DN/LD/NV по `layer_*`; поиск по имени/статусу; иконки как на карте (`resolvedIconUrl`); refresh в тулбаре (ArrowClockwise). Не заменяет веб-меню `/sw`.
  Verify: tab «Свичи» → список коммутаторов с карты; смена сегмента DN показывает только `layer_dn`; поиск сужает список; иконки зел/красн/жёлт/серые PNG; кнопка обновить в top bar; файлы `ui/switches/*` на месте.
- [x] U10 — Экран **Узлы** = веб left menu `#s_sw`: список с `GET /sw` (`NosSwRepository.sites`); тап → деталь `GET /sw/{id}` (таблица устройств: IP/адрес/тип/место/vlan, down=`red`, **mrtg** thumb как веб); поиск по имени; back из детали; refresh списка в тулбаре. Без хардкода имён узлов и без WebView.
  Verify: tab «Узлы» → ~сотни имён вроде `pumba`/`yablochnaya44a` с `/sw/{id}`; открыть узел (напр. `/sw/89`) — список IP + PNG график `/inc/other/rrd/{ip}/nolegend.png` под строкой **целиком** (не обрезан сверху/снизу); красные = down; имена не зашиты в коде.
  Изм. 2026-11-10: под каждым устройством mrtg-график как колонка веб-таблицы.
  Изм. 2026-11-10: mrtg без crop — `aspectRatio(647/182)` + `ContentScale.Fit`, не фиксированная высота/`FillWidth`.
- [x] U11 — Клик по устройству/mrtg на детали узла открывает экран как веб `/sw/pic/{ip}` (`NosSwRepository.picPage`): список day-графиков; тап по linked-графику → `/sw/pic/{ip}/{n}` (hour/day/week/month/year). Back по стеку. Без WebView.
  Verify: Узлы → сайт → тап строку/график → страница с `*.day.png`; тап day → набор `*.hour/day/week/month/year.png`; данные с `GET /sw/pic/...`, не хардкод списка PNG.
- [x] U5 — Сворачиваемая легенда как веб `#legend`: иконка + подпись + count; **чекбоксы** вкл/выкл слои на карте (`visibleLayers` / `ToggleLayer` / `ToggleGroup`) без перезагрузки markgen; данные из OOP-каталога. Панели легенды/обновления — **TopStart**, не перекрывают zoom +/- снизу.
  Verify: `MapLegendPanel` + `CupertinoCheckBox`; снять «Свич доступен» → зелёные свичи пропадают; zoom кнопки снизу справа свободны; нет краша `clickable`/`CupertinoIndication`.
  Изм. 2026-11-10: checkbox show/hide; панели снизу → TopStart (не кроют зум).
- [x] U6 — Иконки маркеров на карте совпадают с легендой/markgen: `LeanMapMarker.resolvedIconUrl()` (`icon` с сервера, иначе catalog по `layer`) → osmdroid `Marker.icon` через `MarkerIconCache` (не дефолтный пин).
  Verify: свичи/узлы/wifi на карте цветными PNG с `nos.lds.online/images/map/...`, как в легенде; уникальные URL кэшируются.
- [x] U7 — Клик по маркеру (все типы sw/uzel/pon/wifi/…) открывает попап паритета вебу: поля IP/узел/место/модель; **Ping** (`ajaxping`); **Графики** → `/sw/pic/{ip}`; **Узел** → `/sw/{id}`; **SNMP** → браузер `/sw/snmp/{ip}`; **техработы** (`maintenance` + комментарий).
  Verify: тап маркер → нижний `MapMarkerPopup`; Ping показывает диалог с ответом; Графики/Узел уходят в существующие overlay; техработы ставятся/снимаются.
  Изм. 2026-11-10: было bare InfoWindow name/type → полный попап как веб.
- [x] U8 — Блок **Обновление** как веб `#intval` / `#more_intval`: интервалы 10 секунд / 1 минута / 5 минут (default 5 мин = 300000); `setInterval` → `loadData(isRefreshing)`; UI `MapRefreshPanel` рядом с легендой.
  Verify: свернуть/развернуть «Обновление»; выбрать «10 секунд» — markgen перезапрашивается ~каждые 10с; после смены интервала таймер перезапускается; дефолт 5 минут.
- [x] U12 — Поле пароля на login маскирует ввод (`PasswordVisualTransformation` + `KeyboardType.Password`), не показывает символы открытым текстом.
  Verify: `LoginScreen` password `textField` с `visualTransformation = PasswordVisualTransformation()`; на устройстве/desktop точки/буллеты вместо символов.
- [x] U13 — Смена bottom-tab не перезагружает markgen/списки: `MapViewModel` / `SwitchesViewModel` / `SitesViewModel` живут в `CorporateShell` (`remember`), не внутри `AnimatedContent` tab content.
  Verify: Карта → Узлы → Карта без повторного полного markgen; список маркеров/легенда на месте; то же для Свичи/Узлы.
- [x] U14 — Системный Back как ldsonline: **один** `NavigationBackHandler` — sheet → overlay pop → tab≠Карта→Карта; на Карте без overlay — double-back (<2с) выход (`MainActivity` + toast `press_again_to_exit`). Не три отдельных handler’а.
  Verify: Узлы → Back → Карта (иконка Карта сразу selected); Карта → Back → toast; второй Back <2с → `finish()`; из детали узла Back сначала pop overlay.
- [x] U15 — Main tabs keep-alive: Карта/Свичи/Узлы остаются в composition (`TabKeepAlive` alpha/zIndex), не через `AnimatedContent` на tab. Смена tab/Back не пересобирает osmdroid-маркеры и не тормозит подсветку bottom-nav.
  Verify: Узлы → Back → иконка «Карта» активна сразу, без ожидания «прогрузки» карты; уход/возврат на Карту без полной пересборки overlays.

## D. Данные

- [ ] D1 — Маркер — одна сущность со сменным статусом/слоем (`layer`/`state`/`icon`, напр. wifi ok↔dn); UI-группы — фильтр по полям, не отдельные копии объекта. Офлайн Room хранит сущности; синк не обязан «пересчитывать глобальный JSON», а upsert’ит пришедший снимок типа (пока API только full markgen по type; delta — отдельное улучшение бэка).
  Verify: в модели/доке зафиксировано: группа = атрибут; Refresh веба = `deleteOverlays` + полный markgen всех type по интервалу; натив фильтрует Room по `layer`, не дублирует точки по группам.
- [x] D2 — Легенда карты — статический OOP-каталог (`MapLegendItem` / `MapLegendGroup` / `NosMapLegendCatalog`, паритет веб) **плюс** авто-дополнение: неизвестные `layer` из markgen → `discoverUnknownLayers` (title/icon с маркера), в свою группу по `type` или в «Другие»; `snapshotFor` даёт counts. Не парсить `/osm`.
  Verify: `NosMapLegendCatalog.snapshotFor` / `discoverUnknownLayers`; известные слои из каталога; новый `layer_*` в ответе появляется в легенде без правки каталога; `MapViewModel` кладёт `legend` в `MapUiData`.
  Изм. 2026-11-10: было только static catalog → static + discovery из markgen.
- [x] D3 — Узлы `/sw`: нет JSON-списка — `NosSwRepository` тянет HTML `GET /sw`, `GET /sw/{id}`, `GET /sw/pic/{path}` (тот же CookieJar/сессия), парсит `#s_sw` → `SwSite`, таблицу → `SwDevice`/`SwSitePage`, pic → `SwPicPage`/`SwPicGraph`. Не хардкодить меню/список PNG из HTML-снимка чата.
  Verify: `sites` / `sitePage` / `picPage`; `NosEndpoints.swPic`; парсеры `parseSwSites` / `parseSwSitePage` / `parseSwPicPage`; `SwDevice.mrtgUrl` → `nolegend.png`; UI только через репозиторий.

## B. Сборка

- [x] B1 — Подпись APK debug **и** release через **глобальный** `~/.gradle/gradle.properties`: `KEYSTORE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS_LDS` / `KEY_PASSWORD` (тот же store, что Element `signing.element.*` → `C:/keystore/lds.keystore`). Паттерн как ldsonline: `signingConfigs.release` + оба buildType на него. Секреты/пути **не** в репо и не в `local.properties`.
  Verify: `app/androidApp/build.gradle.kts` читает `hasProperty("KEYSTORE")`; `assembleDebug`/`assembleRelease` SUCCESS; в репо нет паролей keystore.
- [x] B2 — Android app id: `namespace` + `applicationId` = `online.lds.nos` (reverse DNS `nos.lds.online`). Исходники `:app:*` перенесены `git mv` `dev/walhalla/lazagne` → `online/lds/nos`, package/import обновлены. `:kmp:*` / `core` / `server` не трогать.
  Verify: `app/androidApp/build.gradle.kts`; пути `app/**/kotlin/online/lds/nos/**`; манифест `.MainActivity`; установка APK `online.lds.nos`.
  Изм. 2026-11-10: убран хардкод Desktop `debug.keystore` / `lds.signing.dir` — только global gradle props.

## X. Процесс

- [x] X1 — Учётные данные NOS (логин/пароль) не коммитить и не писать в документацию репозитория; для исследований использовать только локальную сессию.
  Verify: поиск по репо не находит пароль; в `Documentation/` только плейсхолдеры/`<username>`.
