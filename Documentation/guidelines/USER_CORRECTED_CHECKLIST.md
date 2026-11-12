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
- [x] P5 — Сессия переживает перезапуск: `PHPSESSID` в `AppPreferences.KEY_PHPSESSID`; prefs файл `nos`. Старт: `restoreSession` → `SessionRestoreResult`: **Valid** (HTML карты) / **Invalid** (форма логина → `remove` + login) / **Unreachable** (нет сети/таймаут → **не** сбрасывать cookie, пустить в app). Preview — `InMemorySettings`.
  Verify: онлайн + живая сессия → сразу карта; airplane + сохранённый PHPSESSID → не login-экран, карта из Room если офлайн-кэш вкл; сервер вернул signin → login и cookie стёрт.
  Изм. 2026-11-10: было `NosSessionStore` → `AppPreferences`.
  Изм. 2026-11-12: третье состояние Unreachable — offline не разлогинивает.
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
- [x] A5 — Офлайн-кэш: модуль `:kmp:db` — **весь Room-код в `commonMain`**, таргеты только Android + iOS + Desktop JVM (`BundledSQLiteDriver`). `RoomMarkerOfflineStore` тоже в `commonMain` db (не копипаста по platform source sets app). JS/Wasm: `NoOpMarkerOfflineStore` в `:kmp:network` (заглушка API). Флаг `AppPreferences.KEY_OFFLINE_CACHE`: on = UI Flow из Room + сеть `replaceType`; off = network-only. Тумблер в Настройках.
  Verify: `RoomMarkerOfflineStore` только в `kmp/db/.../commonMain`; `NoOpMarkerOfflineStore` для js/wasm; Android/Desktop/iOS создают `RoomMarkerOfflineStore(createNosDatabase().markerDao())`; Settings → «Офлайн-кэш карты»; kill app с cache on → маркеры из Room до сети.
  Изм. 2026-11-11: не заглушка на desktop — тот же Room.
  Изм. 2026-11-11: store в db commonMain + NoOp для js/wasm; prefs-тумблер + MapViewModel wiring.

## U. UI / UX

- [ ] U1 — Responsive layout-токены из ldsonline (`:kmp:ui`: `WindowWidth`, `WebViewportBp`, auth metrics); визуал приложения и login — **Cupertino** (не Material UI с референса). Форма авторизации масштабируется phone/tablet portrait/landscape.
  Verify: `LoginScreen` на Cupertino (`CupertinoSection`/`CupertinoButton`/`CupertinoText`); `:kmp:udf` ErrorView/loading без Material3; на ≥768 form+aside row, &lt;480 кнопка full width.
- [ ] U2 — Не копировать Material/Compose UI-скин с ldsonline; источник виджетов — Cupertino в `:app:shared` / `:kmp:udf`.
  Verify: в login и udf UI нет `material3` Button/TextField/OutlinedButton; `app/shared` не тянет `compose-material3` ради экранов.
- [x] U3 — Веб-«Профиль» NOS (= тема) не дублировать отдельным bottom-tab: настройки уже в тулбаре. В `SettingsScreen` активны блоки «Оформление» и «О приложении»; демо-«Уведомления» закомментированы (не удалены). Выход — см. U19.
  Verify: открыть шестерёнку в тулбаре — видны Оформление + О приложении, секции уведомлений нет; закомментированный блок «Уведомления» остаётся в `SettingsScreen.kt`.
  Изм. 2026-11-11: logout вынесен в Settings (U19), не отдельный tab Профиль.
- [x] U4 — Bottom nav: tabs **Карта** (`MainTab.Map`, дефолт), **Свичи** (`MainTab.Switches`, markgen), **Узлы** (`MainTab.Sites`, веб `/sw` left menu). «Профиль» / демо Desktop/Requests убраны из bar (файлы экранов не удалять, entries закомментированы). Тема — Settings overlay.
  Verify: bottom bar показывает «Карта», «Свичи», «Узлы»; нет «Профиль»/«Рабочий стол»/«Заявки»; `ProfileScreen.kt` / `DesktopScreen.kt` / `RequestsScreen.kt` на месте; `MainTab` entries в `CorporateShell.kt`.
  Изм. 2026-11-10: добавлен tab Карта; демо Desktop/Requests тоже закомментированы в nav.
  Изм. 2026-11-10: добавлен tab «Свичи» (markgen).
  Изм. 2026-11-10: добавлен tab «Узлы» (веб `#s_sw`); markgen-tab **Свичи** не удалять.
- [x] U9 — Экран **Свичи** (оставить): список lean markgen `type=sw` (`NosMapRepository.markgen(TYPE_SW)`); фильтр сегментами Все/OK/DN/LD/NV по `layer_*`; поиск по имени/статусу; иконки как на карте (`resolvedIconUrl`); refresh в тулбаре (ArrowClockwise). Тап по строке → тот же `MapMarkerPopup`, что у маркера на карте (Ping/Графики/Узел/SNMP/техработы/share). Не заменяет веб-меню `/sw`.
  Verify: tab «Свичи» → список; тап строку → попап как на Карте; Графики/Узел/SNMP открывают те же overlay; сегмент DN / поиск / иконки / refresh в top bar; файлы `ui/switches/*` на месте.
  Изм. 2026-11-11: клик айтема → `MapMarkerPopup` (паритет карте).
- [x] U10 — Экран **Узлы** = веб left menu `#s_sw`: список с `GET /sw` (`NosSwRepository.sites`); тап → деталь `GET /sw/{id}` (таблица устройств: IP/адрес/тип/место/vlan, down=`red`, **mrtg** thumb как веб); поиск по имени; back из детали; refresh списка в тулбаре. Без хардкода имён узлов и без WebView.
  Verify: tab «Узлы» → ~сотни имён вроде `pumba`/`yablochnaya44a` с `/sw/{id}`; открыть узел (напр. `/sw/89`) — список IP + PNG график `/inc/other/rrd/{ip}/nolegend.png` под строкой **целиком** (не обрезан сверху/снизу); красные = down; имена не зашиты в коде.
  Изм. 2026-11-10: под каждым устройством mrtg-график как колонка веб-таблицы.
  Изм. 2026-11-10: mrtg без crop — `aspectRatio(647/182)` + `ContentScale.Fit`, не фиксированная высота/`FillWidth`.
  Изм. 2026-11-10: пустые mrtg — Coil `ImageRequest.listener` пишет в logcat `NosMrtg START|OK|ERR|CANCEL` + URL/source/throwable (диагностика, не теория).
  Изм. 2026-11-11: не подменять mrtg текстом «График временно недоступен» — как веб, всегда грузить `/inc/other/rrd/{ip}/nolegend.png` (пустой трафик = пустой RRD PNG). `MainActivity` → `httpClient` в `App` (Coil+Ktor). Text-stub запрещён.
  Изм. 2026-11-11: empty «Нет свичей на узле» по центру экрана (`fillParentMaxSize` + `Alignment.Center` в Lazy item), не прилипший к верху.
  Изм. 2026-11-11: mrtg — не залипать на закэшированном Coil HTTP 404 (`diskCachePolicy WRITE_ONLY` + clear memory/disk при создании ImageLoader); URL как веб `/inc/other/rrd/{ip}/nolegend.png` (пустой RRD PNG).
  Изм. 2026-11-12: уточнение text-stub — заглушка «График временно недоступен!» показывается **только** при реальной ошибке загрузки (файла нет, HTTP 404), как браузер рисует `<img alt>`. Пустой трафик = валидный пустой PNG (не 404) → грузится как есть, без заглушки. Общий хелпер `ui/sites/MrtgGraphImage.kt` (`SubcomposeAsyncImage` + `error`-слот), используется в `SiteDetailScreen` и `SwPicScreen`.
  Verify: открыть узел с несуществующим графиком → под строкой «График временно недоступен!» (не битая/пустая картинка); узел с реальным (в т.ч. пустым) графиком → PNG грузится.
- [x] U11 — Клик по устройству/mrtg на детали узла открывает экран как веб `/sw/pic/{ip}` (`NosSwRepository.picPage`): список day-графиков; тап по linked-графику → `/sw/pic/{ip}/{n}` (hour/day/week/month/year). Back по стеку. Без WebView.
  Verify: Узлы → сайт → тап строку/график → страница с `*.day.png`; тап day → набор `*.hour/day/week/month/year.png`; данные с `GET /sw/pic/...`, не хардкод списка PNG.
- [x] U5 — Сворачиваемая легенда как веб `#legend`: иконка + подпись + count; **чекбоксы** вкл/выкл слои на карте (`visibleLayers` / `ToggleLayer` / `ToggleGroup`) без перезагрузки markgen; данные из OOP-каталога. Панели **TopStart**, порядок сверху вниз: **Обновление** → **Легенда** (не кроют zoom +/-). Старт — обе панели **свёрнуты** (`initiallyExpanded = false`). Состояние чекбоксов переживает перезапуск: скрытые `layer` в `AppPreferences.KEY_MAP_HIDDEN_LAYERS`; новые слои по умолчанию видны.
  Verify: холодный вход на Карту — видны только заголовки «Обновление» и «Легенда» (свёрнуты), Обновление выше Легенды; развернуть → чекбоксы; снять «Свич доступен» → зелёные свичи пропадают; kill app → снова скрыты; zoom свободны.
  Изм. 2026-11-10: checkbox show/hide; панели снизу → TopStart (не кроют зум).
  Изм. 2026-11-11: persist hidden layers через `AppPreferences`.
  Изм. 2026-11-12: строка легенды «Скрытые свичи» (`item("nv", …, "layer_nv")`, серый `mm_20_grey.png`) — **наше добавление поверх веба**, в веб-легенде `#legend` её нет. Слой `layer_nv` при этом реальный (сервер отдаёт `layer_ok|ld|dn|nv`); используется и как фильтр «NV» на экране Свичи. **Не удалять** (намеренно, подтверждено пользователем).
  Изм. 2026-11-11: порядок Обновление над Легендой; старт свёрнутыми.
- [x] U6 — Иконки маркеров на карте совпадают с легендой/markgen: `LeanMapMarker.resolvedIconUrl()` (`icon` с сервера, иначе catalog по `layer`) → osmdroid `Marker.icon` через `MarkerIconCache` (не дефолтный пин). На карте отрисовка **×1.5** от пикселей PNG (`MarkerIconCache.DISPLAY_SCALE`); легенда/списки без этого scale.
  Verify: свичи/узлы/wifi на карте цветными PNG с `nos.lds.online/images/map/...`, как в легенде; уникальные URL кэшируются; маркеры заметно крупнее исходного PNG (~1.5×).
  Изм. 2026-11-11: маркеры на карту только **после** загрузки иконок (`displayMarkers`); параллельный download + disk cache `map_icons` — без вспышки стандартного osmdroid-пина.
  Изм. 2026-11-11: `DISPLAY_SCALE = 1.5f` при создании drawable для osmdroid.
- [x] U17 — Связи на карте (`type=line` / `mft_line`): сервер отдаёт **отрезки** `lat1,lng1,lat2,lng2` (числа или строки), **без** `layer`/`color` — слой выводится из типа (`line`→`layer_line`, `mft_line`→`layer_mft_line`). Отдельная модель `LeanMapLine` (не точка-маркер), парсер `NosMapRepository.markgenLines`/`parseLeanLines` (пропускать вырожденные `p1==p2`). В `MapViewModel` держатся отдельным списком `MapUiData.lines` (в памяти, грузятся параллельно маркерам; `buildData`/offline-observe их сохраняют), фильтр по `visibleLayers` (чекбокс «Связи»). На Android рисуются `org.osmdroid.views.overlay.Polyline` **под** маркерами (сперва линии, потом `Marker`); `NosMapCanvas(lines=…)` в expect + все actual. ~3270 отрезков `line` + ~10 `mft_line`. Room-персист линий — отдельный этап (пока только в памяти).
  Verify: Карта → включён слой «Связи» → синяя сеть линков (плотный клубок у хабов + длинные магистрали); снять «Связи» → линии исчезают; маркеры поверх линий; экран входа — поле пароля с иконкой глаза.
- [x] U7 — Клик по маркеру (все типы sw/uzel/pon/wifi/…) открывает попап паритета вебу: поля IP/узел/место/модель; **Ping** (`ajaxping`); **Графики** → `/sw/pic/{ip}`; **Узел** → `/sw/{id}`; **SNMP** — те же данные, что веб тянет в UI (`GET /sw/snmp/{ip}` shell + `ajax/ajaxsnmp.php`: на open как веб `LabelOff`→`mode=label_off`, `Ports`→`mode=new_port_description`), показ в **своём** Compose UI (`SnmpScreen`), **не** WebView и не браузер; **техработы** (`maintenance` + комментарий). В заголовке попапа — иконка маркера (`resolvedIconUrl`) рядом с именем. FDB/RMON/Getmac — позже, не блокер закрытия SNMP-базы.
  Verify: тап маркер / deep link → SNMP → нативный список портов + label/free из ajaxsnmp; в коде нет `openUri(.../sw/snmp/)`; Ping/Графики/Узел/техработы как раньше; в title попапа PNG как на карте/легенде.
  Изм. 2026-11-10: было bare InfoWindow name/type → полный попап как веб.
  Изм. 2026-11-11: SNMP не в браузер — `Overlay.SwSnmpDetail` + `NosSwRepository.snmpPage`.
  Изм. 2026-11-11: принцип — веб-UI только подтягивает ajax; мобильный UI подтягивает **те же** mode’ы, свой экран.
  Изм. 2026-11-11: парсер портов принимает `id="l_"` без цифры (недоступный свич / eth0), не показывает ложное «Нет данных портов».
  Изм. 2026-11-11: SNMP-база закрыта (open = label+ports + deep link); доп. mode’ы — отдельный шаг.
  Изм. 2026-11-11: иконка в title `MapMarkerPopup`.
- [x] U17 — Deep link для теста SNMP: `VIEW https://nos.lds.online/sw/snmp/{ip}` (manifest pathPrefix) или extra `snmp_ip` → после логина сразу `Overlay.SwSnmpDetail` (не браузер). `MainActivity` `singleTop` + `onNewIntent`.
  Verify: `adb shell am start -a android.intent.action.VIEW -d "https://nos.lds.online/sw/snmp/10.0.93.243"` (или `-e snmp_ip 10.0.93.243`) открывает нативный SNMP с этим IP.
- [x] U16 — Попап маркера: **Копировать** (буфер) и **Поделиться** (системный share sheet) — plain text только из реальных полей `LeanMapMarker` / `formatShareText()` (имя/тип/слой/IP/узел/место/модель/серийник/MAC/техработы/координаты + URL `/sw/{id}`, `/sw/pic/{ip}`, `/sw/snmp/{ip}` если ключи есть). Ничего не выдумывать. Фича mobile, на вебе её нет.
  Verify: тап маркер → «Копировать»/«Поделиться»; в тексте есть IP/узел если они на маркере; пустых выдуманных полей нет; Android share sheet открывается.
- [x] U8 — Блок **Обновление** как веб `#intval` / `#more_intval`: интервалы 10 секунд / 1 минута / 5 минут (default 5 мин = 300000); `setInterval` → `loadData(isRefreshing)`; UI `MapRefreshPanel` **над** легендой, старт свёрнут.
  Verify: на Карте «Обновление» выше «Легенда», оба свёрнуты при старте; развернуть → выбрать «10 секунд» — markgen ~каждые 10с; дефолт 5 минут.
  Изм. 2026-11-11: позиция выше легенды; старт свёрнут.
- [x] U12 — Login: `CupertinoLoginField` / `CupertinoPasswordField` на `BasicTextField` + CupertinoTheme; `ContentType`+`ContentDataType.Text` на BTF; форма в **не-lazy** `CupertinoSection`/`Column` (не `LazyColumn`/`section{}` item — Autofill слепнет). `commit()` после успешного логина. Цель — Android Autofill / Password Manager (сохранить + быстрый ввод). Без Material TextField / `CupertinoTextField`.
- [x] U12.1 — Поле пароля: встроенная иконка показа/скрытия пароля в `trailingIcon` (`CupertinoIcons.Default.Eye` / `EyeSlash`, tint = `secondaryLabel`), переключает `VisualTransformation` (None ↔ `PasswordVisualTransformation`). Состояние `revealed` внутри `CupertinoPasswordField`. Размер — **как кнопка очистки логина**: голая `CupertinoIcon` c `.size(CupertinoIconDefaults.MediumSize)` + `detectTapGestures` (не `CupertinoIconButton` — у него крупнее touch-таргет/иконка).
  Verify: на экране входа иконка глаза в поле пароля того же размера, что крестик-очистка в поле логина.
- [ ] U12.2 — Android Autofill работает только когда на устройстве выбрана служба автозаполнения (`settings get secure autofill_service` ≠ null). Если None — не предлагает ни в NOS, ни в примере ldsonline (это настройка устройства, не код). Включение: Настройки → Пароли и автозаполнение → Google/Samsung Pass, либо `adb shell settings put secure autofill_service "com.google.android.gms/.autofill.service.AutofillService"`.
  Verify: фокус логина/пароля → предложение из Password Manager; после успешного «Вход» — диалог сохранить пароль; `LoginScreen` без ScreenList для полей.
  Изм. 2026-11-12: BTF-поля; убрать lazy section; commit после успеха.
- [x] U13 — Смена bottom-tab не перезагружает markgen/списки: `MapViewModel` / `SwitchesViewModel` / `SitesViewModel` живут в `CorporateShell` (`remember`), не внутри `AnimatedContent` tab content.
  Verify: Карта → Узлы → Карта без повторного полного markgen; список маркеров/легенда на месте; то же для Свичи/Узлы.
- [x] U14 — Системный Back как ldsonline: **один** `NavigationBackHandler` — sheet → overlay pop → tab≠Карта→Карта; на Карте без overlay — double-back (<2с) выход (`MainActivity` + toast `press_again_to_exit`). Не три отдельных handler’а.
  Verify: Узлы → Back → Карта (иконка Карта сразу selected); Карта → Back → toast; второй Back <2с → `finish()`; из детали узла Back сначала pop overlay.
- [x] U15 — Main tabs keep-alive: Карта/Свичи/Узлы остаются в composition (`TabKeepAlive` alpha/zIndex), не через `AnimatedContent` на tab. Смена tab/Back не пересобирает osmdroid-маркеры и не тормозит подсветку bottom-nav.
  Verify: Узлы → Back → иконка «Карта» активна сразу, без ожидания «прогрузки» карты; уход/возврат на Карту без полной пересборки overlays.
- [x] U18 — Экран **Карта** не ждёт полный markgen за `BaseScreenLayout`/shimmer: сразу shell (тайлы osmdroid + заглушка легенды из каталога, чекбоксы неактивны пока нет маркеров) и стриминг типов markgen по мере ответа; индикатор у панелей пока `markersLoading`. Остальные экраны по-прежнему на `BaseScreenLayout`.
  Verify: холодный вход на Карту — карта/легенда видны до конца всех 9 запросов; маркеры появляются пачками; нет полноэкранного спиннера ~3с; `MapScreen` без `BaseScreenLayout`; `MapViewModel.loadMarkersProgressive`.
- [x] U19 — **Выйти из аккаунта** в Настройках (шестерёнка): кнопка внизу `SettingsScreen` → confirm → `remove(KEY_PHPSESSID)` + `AuthGate.NeedLogin` (экран логина). Можно убрать позже без затрагивания темы.
  Verify: Шестерёнка → «Выйти из аккаунта» → «Выйти» → форма логина; после kill app снова login (нет `pref_nos_phpsessid`); тема/легенда prefs не сбрасываются.
- [x] U20 — LeakCanary подключён **только** для debug: `debugImplementation(libs.leakcanary.android)` в `:app:androidApp` (каталог `leakcanary = "2.14"`). Инициализация автоматом через ContentProvider, кода не трогаем. В release не попадает (нет `releaseImplementation`).
  Verify: debug-запуск → logcat `D LeakCanary: LeakCanary is running and ready to detect memory leaks.`; в `assembleRelease` зависимости `leakcanary` нет (`dependencies --configuration releaseRuntimeClasspath`).
- [x] U21 — **Муфты на карте** (шаг 1): легенда «Муфты» оставляет иконку `/images/map/pon.png` как есть; на карте `layer_pon_mft` / `type=mft` **не** создают osmdroid `Marker` и **не** качают иконку — один переиспользуемый `MftCirclesOverlay` рисует кружки в `draw(Canvas)` (паритет веб `L.circleMarker` + `preferCanvas`), с viewport-куллингом. Тап по кружку → тот же `onMarkerClick`.
  Verify: легенда — по-прежнему `pon.png`; на карте при включённых Муфтах — чёрные полупрозрачные точки, не pin/`pon.png`; в overlays один `MftCirclesOverlay`, не ~14k `Marker`; `dumpsys gfxinfo` с Муфтами вкл ≈ низкий jank (не ~98% как раньше с Marker).

## D. Данные

- [ ] D1 — Маркер — одна сущность со сменным статусом/слоем (`layer`/`state`/`icon`, напр. wifi ok↔dn); UI-группы — фильтр по полям, не отдельные копии объекта. Офлайн Room: PK стабильный (`type`+`entityId`/`ip`/`lat,lng,name`); sync markgen = **replace по `type`** в одной `@Transaction` (`DELETE WHERE type=?` + `INSERT` batch), не wipe всей таблицы и не поштучный replace всего листа; точечный upsert одного маркера (maintenance/ping) не ломает остальной type. Список узлов `SwSite` — replace всего меню; детали узла — по `siteId`. API delta — позже.
  Verify: `MarkerEntity` + `replaceType(type, rows)`; Map progressive пишет type-batch в DAO → Flow обновляет UI; смена одного maintenance не чистит другие type; фильтр UI по `layer`, не дубли групп.
- [x] D2 — Легенда карты — статический OOP-каталог (`MapLegendItem` / `MapLegendGroup` / `NosMapLegendCatalog`, паритет веб) **плюс** авто-дополнение: неизвестные `layer` из markgen → `discoverUnknownLayers` (title/icon с маркера), в свою группу по `type` или в «Другие»; `snapshotFor` даёт counts. Не парсить `/osm`.
  Verify: `NosMapLegendCatalog.snapshotFor` / `discoverUnknownLayers`; известные слои из каталога; новый `layer_*` в ответе появляется в легенде без правки каталога; `MapViewModel` кладёт `legend` в `MapUiData`.
  Изм. 2026-11-10: было только static catalog → static + discovery из markgen.
- [x] D3 — Узлы `/sw`: нет JSON-списка — `NosSwRepository` тянет HTML `GET /sw`, `GET /sw/{id}`, `GET /sw/pic/{path}`, SNMP `GET /sw/snmp/{ip}` + `ajaxsnmp.php` (тот же CookieJar/сессия); парсит `#s_sw` → `SwSite`, таблицу → `SwDevice`/`SwSitePage`, pic → `SwPicPage`/`SwPicGraph`, snmp → `SwSnmpPage`. Не хардкодить меню/список PNG из HTML-снимка чата.
  Verify: `sites` / `sitePage` / `picPage` / `snmpPage`; `NosEndpoints.swPic`/`swSnmp`/`AJAX_SNMP`; парсеры; `SwDevice.mrtgUrl` → `nolegend.png`; UI только через репозиторий.

## B. Сборка

- [x] B1 — Подпись APK debug **и** release через **глобальный** `~/.gradle/gradle.properties`: `KEYSTORE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS_LDS` / `KEY_PASSWORD` (тот же store, что Element `signing.element.*` → `C:/keystore/lds.keystore`). Паттерн как ldsonline: `signingConfigs.release` + оба buildType на него. Секреты/пути **не** в репо и не в `local.properties`.
  Verify: `app/androidApp/build.gradle.kts` читает `hasProperty("KEYSTORE")`; `assembleDebug`/`assembleRelease` SUCCESS; в репо нет паролей keystore.
- [x] B2 — Android app id: `namespace` + `applicationId` = `online.lds.nos` (reverse DNS `nos.lds.online`). Исходники `:app:*` перенесены `git mv` `dev/walhalla/lazagne` → `online/lds/nos`, package/import обновлены. `:kmp:*` / `core` / `server` не трогать.
  Verify: `app/androidApp/build.gradle.kts`; пути `app/**/kotlin/online/lds/nos/**`; манифест `.MainActivity`; установка APK `online.lds.nos`.
  Изм. 2026-11-10: убран хардкод Desktop `debug.keystore` / `lds.signing.dir` — только global gradle props.
- [ ] B3 — Desktop JVM: cupertino `CupertinoBottomSheetScaffold` / defaults `WindowInsets.systemBars` дают `NoSuchMethodError` на CMP 1.12 (баг библиотеки). В `CorporateShell` снова общий `CupertinoBottomSheetScaffold` (как до split). Workaround desktop — отдельно, если понадобится.
  Verify: `CorporateShell` → `CupertinoBottomSheetScaffold` + `rememberCupertinoSheetState`; нет `NosAppScaffold`; Android ок; desktop после login может падать до фикса cupertino / отдельного desktop shell.
- [x] B4 — Имя выходного APK как в ldsonline (`archivesBaseName = applicationId-versionName`). На AGP 9: `android.base.archivesName.set("$applicationId-$versionName")` → `online.lds.nos-1.0-release.apk` (AGP добавляет `-<buildType>`).
  Verify: `app/androidApp/build.gradle.kts` → `base { archivesName.set(...) }`; `assembleRelease` → `outputs/apk/release/online.lds.nos-1.0-release.apk`, не `androidApp-release.apk`.

## X. Процесс

- [x] X1 — Учётные данные NOS (логин/пароль) не коммитить и не писать в документацию репозитория; для исследований использовать только локальную сессию.
  Verify: поиск по репо не находит пароль; в `Documentation/` только плейсхолдеры/`<username>`.
