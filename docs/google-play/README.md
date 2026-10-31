# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `Dayquest-v<версия>.aab`.
  Первая версия для Play — **v1.6.2**: AAB добавляется к уже вышедшему релизу повторным запуском
  Actions → Release → Run workflow → `1.6.2`, без нового релиза. Секреты `SIGNING_*` в репозитории заданы;
  без них сборка подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках приложения перестаёт работать.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.dayquest`.
  2. **Select key** → ключ с SHA-256 `adc70f75ef38d2b30d0bc71382bba9468b917a22eacb0aa7de45ef3a40b8c148`
     (сертификат CN=Dayquest, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке оставьте вариант «Google создаст ключ подписи».
  Ваш keystore станет ключом загрузки (upload key) — храните его.
  ⚠️ Из-за этого APK из GitHub и версия из Play будут подписаны разными ключами: поверх друг друга они
  не обновятся. Если нужно, чтобы совпадали, выберите «Использовать свой ключ» и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге).
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория. После push ссылка:
  `https://github.com/bigdevwhale/dayquest/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Dayquest: Habit RPG` |
| Default language | English (United States) – en-US |
| App or game | **App** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Habitica в текстах карточки не упоминается намеренно: чужой бренд в метаданных — повод для отказа
(политика Metadata / Impersonation). В README репозитория ссылка остаётся.

Скриншоты 1080×1920 собраны `tools/play.py` из снимков экрана демо-сборки на телефоне 1080×2340:
Play не берёт картинки длиннее двух ширин, поэтому статус-бар и жестовая панель срезаются, а экран
стоит карточкой под подписью на фиолетовом фоне значка. Значок 512 рисуется из `docs/logo.svg`,
баннер — из сцены пустыни с огненным драконом, которую рендерит превью спрайтов. Как пересобрать:

```bash
./gradlew installDebug -Pdemo   # app.dayquest.demo с героем 21 уровня, рядом с настоящей установкой
# снять экраны на английском (<имя>.png) и русском (<имя>.ru.png):
# tasks, quests, stable, shop, home, card, history, hero
./gradlew testDebugUnitTest -Ppreview=<папка превью>
python tools/play.py <папка со снимками> <папка превью>
```

Демо-герой заводится на языке приложения при первом запуске: для второго языка очистите данные
`app.dayquest.demo` и запустите его уже с другим языком. Подписи к скриншотам — в `SCREENS` в `tools/play.py`.

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | App → **Productivity** |
| Tags (до 5) | **Productivity**, **Self-help**, **Calendar**, **Lifestyle**, **Health & fitness** |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/dayquest/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Это трекер привычек и список дел; игровой слой (герой, боссы, питомцы) работает на дела,
а не сам по себе, поэтому это App, а не Game — так же публикуются и похожие приложения. Если хочется
в игры — Game → **Role Playing**, но тогда карточку будут сравнивать с настоящими RPG.

**Теги.** Productivity — сама категория, задачи и сроки; Self-help — привычки и добрые утра без
наказаний за болезнь; Calendar — ежедневные дела по дням недели и напоминания; Lifestyle — дневник,
настроение, фокус; Health & fitness — привычки вроде воды, зарядки и медитации в демо-наборе.
Тегов «Habit tracker» и «RPG» для приложений в Play нет.

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types** (если анкета предложит игровую — **Game**, ответы те же).
- **Violence** — **Yes**, только фантазийная и мультяшная: пиксельный герой «бьёт» слизней, гоблинов
  и драконов, когда вы выполняете дела, летят цифры урона и искры. Крови, расчленения и насилия над
  людьми нет; реалистичного оружия нет.
- Кровь, страх, сексуальный контент, ненормативная лексика, наркотики, алкоголь, табак — **No**.
  (В таверне только отдых и обмен кодами героев, напитков нет.)
- **Gambling** — **No**: сундуки и карты дня даются за выполненные дела, купить их нельзя, денег в игре нет.
- **Users interact / exchange content** — **No**: чата, профилей и сервера нет; коды героев друзья
  пересылают сами через мессенджеры.
- **Shares user's current location with other users** — **No**.
- Цифровые покупки — **No**: золото зарабатывается только делами.
- Ожидаемый рейтинг — Everyone 10+ / PEGI 7 из-за мультяшных боёв (может выйти и Everyone).

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over** (политика: приложение не для детей младше 13).
- *Could your store listing unintentionally appeal to children?* → **No**: это трекер привычек и задач
  для взрослых и подростков. Пиксель-арт и питомцы — оформление, а не детский контент.
  Если Play всё же отнесёт к детской аудитории, понадобится соответствие политике Families —
  данных приложение не собирает, так что это только лишняя проверка.

**News apps** — **No.**

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**
- *Does your app collect or share any of the required user data types?* → **No.**
  У приложения нет разрешения `INTERNET`: ничего не может покинуть телефон само. Коды героев, файл переноса
  и картинки уходят только через выбранное пользователем приложение по его нажатию; ссылки на сообщество
  в настройках открывают Telegram или браузер — это не сбор данных разработчиком.
- *Is all of the user data collected by your app encrypted in transit?* и вопрос об удалении данных
  после ответа «No» не появятся.
- Системный бэкап Android (`allowBackup="true"`) и ручной перенос в файл в Data safety не декларируются.
- В карточке будет «No data collected».

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — **No**: привычки вроде «выпить воды» — обычные задачи, медицинских функций и данных
о здоровье нет.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Permissions** — специальных деклараций не нужно: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`,
`VIBRATE` — обычные разрешения без форм. Точных будильников (`SCHEDULE_EXACT_ALARM`, напоминания
ставятся через `setAndAllowWhileIdle`) и foreground-сервисов нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Dayquest-v1.6.2.aab` из GitHub Release.
3. Release name: `1.6.2`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран должен
   переключиться сразу; напоминание задачи приходит, разрешение на уведомления спрашивается,
   фокус-сессия звонит при закрытом приложении.

## Следующие версии

- `release_notes.txt` переписать под новую версию (для первой там «First release on Google Play»).
- Скриншоты пересобирать, только если поменялись показанные экраны.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.dayquest`.
