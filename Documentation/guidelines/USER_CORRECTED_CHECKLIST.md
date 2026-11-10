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
- [ ] P2 — Нативный Android UI карты допустим поверх текущего NOS API (без ожидания нового бэка): login + `PHPSESSID`, тайлы `maps.lds.online`, маркеры через `ajax/ajaxmap.php`; lean/popup и bbox — улучшения поверх, не блокер старта.
  Verify: в приложении есть экран карты с тайлами LDS и хотя бы одним типом markgen (например `uzel` или `sw`) без WebView страницы `/osm`.
- [x] P3 — Нативная авторизация = session-login как у веба: GET `/login` → POST `login`/`password`/`next`/`submit-login` → успех только при **302 на next** (или страница карты без формы входа); cookie `PHPSESSID` в Ktor CookieJar. Не считать успехом выдачу PHPSESSID и не использовать `get_count` как proof (он публичный). Неверный пароль не пускает в приложение.
  Verify: `NosAuthRepository.login` бросает на «Неверное имя…» / форме входа; после успеха открывается `/osm` без redirect на login; wrong-password тест/ручная проверка не вызывает `onAuthenticated`.
  Изм. 2026-11-10: было → успех по PHPSESSID+`get_count` (ложный пропуск) → стало → web 302/страница карты.
- [ ] P4 — Первый продуктовый экран NOS на compose-cupertino (кит из `Начальный промпт.md`): форма авторизации только API Cupertino (section/textField/button/dialog/activityIndicator), без Material.
  Verify: `LoginScreen.kt` использует Cupertino + `ScreenList`; нет material3 виджетов на экране входа; сборка `:app:shared:compileKotlinJvm` ок; on-device smoke login UI.

## A. Архитектура

- [ ] A1 — Payload маркеров lean: в списке точек для карты нет тяжёлого `popupContent` (HTML); детали попапа подгружаются отдельно по клику/id.
  Verify: ответ list/markgen (или клиентский lean-слой) содержит координаты/слой/иконку/статус без HTML; detail-запрос отдельный.
- [ ] A2 — Загрузка данных карты по требованию слоёв (и далее по viewport/bbox), а не всех типов markgen сразу при открытии экрана.
  Verify: при старте карты не уходят параллельные `markgen` по всем type; запрос типа идёт только для включённого слоя (и/или текущего bbox).
- [ ] A3 — В проекте есть UDF как в ldsonline: модуль `:kmp:udf` (`BaseViewModel`, `UiState`, `ScreenState`, `BaseScreenLayout` / effects) и зависимости `:kmp:network` (`ApiException`), `:kmp:ldscore` (notifications enums).
  Verify: `settings.gradle.kts` include `:kmp:udf`; `app/shared` зависит от `:kmp:udf`; login/экраны наследуют `BaseViewModel`.

## U. UI / UX

- [ ] U1 — Responsive layout-токены из ldsonline (`:kmp:ui`: `WindowWidth`, `WebViewportBp`, auth metrics); визуал приложения и login — **Cupertino** (не Material UI с референса). Форма авторизации масштабируется phone/tablet portrait/landscape.
  Verify: `LoginScreen` на Cupertino (`CupertinoSection`/`CupertinoButton`/`CupertinoText`); `:kmp:udf` ErrorView/loading без Material3; на ≥768 form+aside row, &lt;480 кнопка full width.
- [ ] U2 — Не копировать Material/Compose UI-скин с ldsonline; источник виджетов — Cupertino в `:app:shared` / `:kmp:udf`.
  Verify: в login и udf UI нет `material3` Button/TextField/OutlinedButton; `app/shared` не тянет `compose-material3` ради экранов.

## D. Данные

- [ ] D1 — Маркер — одна сущность со сменным статусом/слоем (`layer`/`state`/`icon`, напр. wifi ok↔dn); UI-группы — фильтр по полям, не отдельные копии объекта. Офлайн Room хранит сущности; синк не обязан «пересчитывать глобальный JSON», а upsert’ит пришедший снимок типа (пока API только full markgen по type; delta — отдельное улучшение бэка).
  Verify: в модели/доке зафиксировано: группа = атрибут; Refresh веба = `deleteOverlays` + полный markgen всех type по интервалу; натив фильтрует Room по `layer`, не дублирует точки по группам.

## X. Процесс

- [x] X1 — Учётные данные NOS (логин/пароль) не коммитить и не писать в документацию репозитория; для исследований использовать только локальную сессию.
  Verify: поиск по репо не находит пароль; в `Documentation/` только плейсхолдеры/`<username>`.
