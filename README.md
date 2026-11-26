# AnyAuto (форк dak1ll/AnyVAGAuto)

[![Download APK](https://img.shields.io/github/v/release/dak1ll/AnyVAGAuto?label=download%20APK&logo=android&color=success)](https://github.com/dak1ll/AnyVAGAuto/releases/latest)

<p align="center"><img src="docs/icon.svg" alt="Иконка AnyAuto" width="128"></p>

**AnyAuto** выводит **обычные приложения телефона на экран Android Auto**: браузер, видео, навигацию, настройки и любые другие приложения с иконкой в лаунчере. Сами приложения не модифицируются — AnyAuto запускает их на виртуальном дисплее размером с экран автомобиля и рисует его напрямую в поверхность Android Auto. Поддерживаются сенсорные экраны, а также автомобили с поворотной шайбой или тачпадом.

Для работы используется [Shizuku](https://shizuku.rikka.app/) (или его форк вроде [Shevery](https://github.com/HmnDev-Tech/shevery)), поэтому **root-права и компьютер в автомобиле не нужны**. В приложении нет аккаунтов и аналитики; единственный сетевой запрос — проверка обновлений на GitHub при открытии главного экрана (при желании отключается в меню).

Это русскоязычный и расширенный форк проекта [GitMag/AnyAuto](https://github.com/GitMag/AnyAuto) под лицензией **GPL-3.0** (см. раздел [Лицензия и благодарности](#лицензия-и-благодарности)).

**[Скачать последний релиз (`AnyAuto-*.apk`)](https://github.com/dak1ll/AnyVAGAuto/releases/latest)**

<p align="center"><img src="docs/screenshots/car-video.png" alt="Воспроизведение видео на экране Android Auto через AnyAuto с панелью навигации слева" width="800"></p>

<p align="center">
  <img src="docs/screenshots/phone-home.png" alt="Главный экран AnyAuto на телефоне" width="260">
  &nbsp;
  <img src="docs/screenshots/phone-picker.png" alt="Экран выбора приложений" width="260">
</p>

---

## Перед установкой (ВАЖНО)

1. **Удалите оригинальные версии от `GitMag`, если они были установлены ранее.**
   Перед установкой этого форка обязательно удалите старые сборки `AnyAuto` и `UnknownAuto` от `GitMag`. У форка **другой ключ подписи**, поэтому Android отклонит обновление поверх оригинала.
2. **Схема установки без root (2 APK + Shizuku):**
   - Установите **[Shizuku](https://shizuku.rikka.app/)** ([GitHub Releases](https://github.com/RikkaApps/Shizuku/releases/latest)) **или** форк **[Shevery](https://github.com/HmnDev-Tech/shevery)** (рекомендуется для автозапуска после перезагрузки). *APK Shizuku/Shevery в этот репозиторий не включается — скачивайте только из официальных источников.*
   - Установите **[UnknownAuto (`dak1ll/VAGAuto`)](https://github.com/dak1ll/VAGAuto/releases/latest)** как обычное приложение.
   - Скачайте **`AnyAuto-<версия>.apk`** со страницы [релизов `dak1ll/AnyVAGAuto`](https://github.com/dak1ll/AnyVAGAuto/releases/latest) и установите его **СТРОГО ЧЕРЕЗ `UnknownAuto`** (`Выбрать APK или XAPK…` → `Установить для Android Auto`), чтобы Android Auto видел метку установки из Google Play (`com.android.vending`).
   - В реальном автомобиле обычный `adb install` или установка из проводника не сработают: Android Auto скрывает приложения без метки Google Play. (Эмулятор DHU работает при любой установке.)

Подробное руководство по настройке автозапуска Shizuku / Shevery без root (включая MacroDroid / Tasker и чек-лист Xiaomi / HyperOS):
👉 **[docs/shizuku-autostart-ru.md](docs/shizuku-autostart-ru.md)**

---

## Улучшения форка по сравнению с оригинальным GitMag/AnyAuto

1. **Персональные профили для каждого приложения:**
   - **Масштаб экрана (DPI):** `Авто` (по экрану авто) или фиксированные значения `120`, `160`, `200`, `240`, `320` dpi.
   - **Отступ от краёв:** `0 dp`, `16 dp` или `32 dp` (удобно, если интерфейс приложения обрезается закруглениями экрана авто).
   - **Ориентация:** `Фиксировать альбомную` или `По приложению`.
2. **Голосовой ввод без мерцания экрана (`PhonePower`):**
   - Опция `⋮ → Клавиатура → Кратко будить экран телефона для голоса` временно пробуждает экран телефона при открытии клавиатуры / микрофона (для корректной диктовки в Gboard, Яндекс Клавиатуре и др.), не вызывая ложного срабатывания защиты от мерцания (`fights`).
3. **Быстрое переключение и разделение экрана:**
   - Кнопка **`Пред.`** (`Last`) в боковой панели навигации на экране авто — мгновенно переключает между двумя последними открытыми приложениями.
   - Экспериментальный режим **`⋮ → Разделение экрана (Freeform)`** (Android 11+) — вывод второго приложения рядом с основным (60/40).
4. **Диагностика Xiaomi / HyperOS / MIUI и батареи (`OemHealth`):**
   - На главном экране автоматически проверяются ограничения батареи, необходимость фонового автозапуска и перехват ошибок `INJECT_EVENTS` (с подсказкой включить `Отладка по USB (Настройки безопасности)` и быстрыми кнопками перехода в нужные разделы настроек).
5. **Полная русская локализация (`res/values-ru/strings.xml`):**
   - Переведены главный экран, выбор приложений, настройки профилей, справка, карточки диагностики, уведомления и экран автомобиля (включая адаптацию ширины боковой панели навигации).
6. **Обновления из форка (`Updater`):**
   - Проверка новых версий в `Updater` направлена на релизы `dak1ll/AnyVAGAuto` (`AnyAuto-*.apk`) с сохранением проверки совпадающей подписи и переходом на `dak1ll/VAGAuto` для установки через `UnknownAuto`.

---

## Что требуется

- Смартфон с **Android 10 или новее** (на Android 10 без root Shizuku запускается по проводу от ПК после каждой перезагрузки; на **Android 11–17** всё настраивается на самом телефоне через *Беспроводную отладку*).
- **[Shizuku](https://shizuku.rikka.app/)** или **[Shevery](https://github.com/HmnDev-Tech/shevery)**.
- **[UnknownAuto (`dak1ll/VAGAuto`)](https://github.com/dak1ll/VAGAuto/releases/latest)**.

---

## Быстрая настройка и использование

1. Установите **Shizuku** (или **Shevery**) и запустите его через *Беспроводную отладку* (инструкция в [docs/shizuku-autostart-ru.md](docs/shizuku-autostart-ru.md)).
2. Установите **UnknownAuto**, затем установите **AnyAuto** через UnknownAuto.
3. Откройте **AnyAuto**, на запросе Shizuku выберите **Разрешать всегда**.
4. Нажмите **Выбрать приложения** и отметьте нужные программы. При необходимости настройте DPI, ориентацию и отступы иконкой профиля рядом с приложением.
5. Проверьте работу кнопкой **Попробовать** на телефоне.
6. Если AnyAuto не появился в меню Android Auto, добавьте его в *Настройки Android Auto → Настроить панель запуска*.

### Управление в автомобиле

Боковая панель навигации (слева или справа — настраивается в `⋮ → Сторона панели навигации`):
- **Назад** — кнопка «Назад» внутри открытого приложения.
- **Прил.** — возврат к сетке выбранных приложений.
- **Пред.** — мгновенное переключение на предыдущее приложение.
- **Клавиа.** — открыть или скрыть клавиатуру для активного поля ввода.
- **Закрыть** — выгрузить текущее приложение.
- **Скрыть** — скрыть панель навигации на весь экран (возвращается кнопкой в правом верхнем углу).

Для автомобилей с **поворотной шайбой / тачпадом** поддерживается как штатный режим панорамирования Android Auto (`✥`), так и встроенный режим `⋮ → Кнопки указателя (шайба)` (для беспроводных адаптеров, скрывающих `✥`).

---

## Известные ограничения без root

- **Чёрный экран в некоторых приложениях (`FLAG_SECURE`):** банковские клиенты, стриминги с DRM (Netflix, Кинопоиск и др.) и вкладки инкогнито защищают свои окна от захвата/отрисовки на стороннем виртуальном дисплее и отображаются чёрным экраном. YouTube и обычные браузеры работают.
- **Звук:** AnyAuto транслирует видео/интерфейс, но не перехватывает аудиопоток. Звук воспроизводится через Bluetooth-подключение к ГУ автомобиля или через аудиоканал Android Auto отдельно.
- **Кнопка клавиатуры:** принудительное открытие/скрытие экранной клавиатуры кнопкой `Клавиа.` требует **Android 11 или новее**.
- **Xiaomi / HyperOS / MIUI:** без включённого пункта **`Отладка по USB (Настройки безопасности)`** в *Для разработчиков* нажатия на экране автомобиля работать не будут (`SecurityException: INJECT_EVENTS`). Подробнее — в [docs/shizuku-autostart-ru.md](docs/shizuku-autostart-ru.md).
- **GrapheneOS и некоторые кастомные прошивки с AOSP-установщиком:** Android Auto принимает приложение, только если установку запустил системный установщик Google (`com.google.android.packageinstaller`). Если в прошивке только AOSP-установщик (`com.android.packageinstaller`), метка Google Play игнорируется и без root/DHU/Google Play Internal Testing Android Auto не покажет AnyAuto.
- **Автозапуск Shizuku после перезагрузки:** Android отключает Беспроводную отладку при ребуте и требует Wi-Fi для её включения — подробности и решения см. в [docs/shizuku-autostart-ru.md](docs/shizuku-autostart-ru.md).

**Безопасность за рулём:** AnyAuto не блокирует приложения во время движения. Используйте видео и отвлекающие приложения только на стоянке и соблюдайте ПДД.

---

## Сборка и тестирование из исходников (без Gradle)

Используются только стандартные инструменты Android SDK (`platforms;android-35` и `build-tools;35.0.0`) и JDK 17.

### Linux / macOS
```sh
./build.sh            # собрать anyauto.apk
./build.sh verify     # собрать, проверить подпись и дамп манифеста
./build.sh install    # собрать, установить через adb install -r и запустить
```

### Windows (PowerShell, из корня `aa-mod`)
```powershell
python build-win.py anyauto --verify
```

### Модульные тесты (JUnit 4)
Тесты расположены в `test/com/mag/anyauto/` (`UpdaterTest`, `AppProfilePrefsTest`, `PhonePowerWakePolicyTest`, `MirrorHistoryTest`, `OemHealthTest`):
```sh
# После ./build.sh verify (или python build-win.py anyauto --verify):
# В Linux/macOS (при наличии junit-4.13.2.jar и hamcrest-core-1.3.jar в build/test-libs/):
. libs/deps.env
LIB_CP=$(echo "$LIB_JARS" | tr ' ' ':')
javac --release 17 -encoding UTF-8 \
  -classpath "build/classes:$LIB_CP:$ANDROID_HOME/platforms/android-35/android.jar:build/test-libs/junit-4.13.2.jar:build/test-libs/hamcrest-core-1.3.jar" \
  -d build/test test/com/mag/anyauto/*.java
java -cp "build/test:build/classes:$LIB_CP:$ANDROID_HOME/platforms/android-35/android.jar:build/test-libs/junit-4.13.2.jar:build/test-libs/hamcrest-core-1.3.jar" \
  org.junit.runner.JUnitCore \
  com.mag.anyauto.UpdaterTest \
  com.mag.anyauto.AppProfilePrefsTest \
  com.mag.anyauto.PhonePowerWakePolicyTest \
  com.mag.anyauto.MirrorHistoryTest \
  com.mag.anyauto.OemHealthTest
```

### GitHub Actions CI и релизы (`.github/workflows/release.yml`)

Для публикации релизов по тегам `v*` (ассет **`AnyAuto-${{ github.ref_name }}.apk`**, требуемый `Updater.isReleaseApkAsset`) необходимо настроить в настройках репозитория **4 секрета GitHub Actions**:
- `KEYSTORE_B64` — keystore-файл в Base64 (например, `base64 -w0 release.keystore`).
- `KS_PASS` — пароль хранилища ключей.
- `KEY_ALIAS` — alias ключа подписи.
- `KEY_PASS` — пароль ключа подписи.

На PR и push в ветки при отсутствии секретов CI собирает и проверяет APK с временным dev-ключом. На тегах `refs/tags/v*` сборка **намеренно завершается ошибкой, если `KEYSTORE_B64` не задан**, чтобы никогда не публиковать релиз с одноразовым ключом (иначе встроенный `Updater` не сможет обновлять приложение).

---

## Лицензия и благодарности

- Оригинальный проект: **[GitMag/AnyAuto](https://github.com/GitMag/AnyAuto)** (Copyright © 2026 GitMag).
- Форк распространяется на условиях **[GNU General Public License v3.0 (GPL-3.0)](LICENSE)** — все модификации и производные работы должны распространяться с открытым исходным кодом под той же лицензией.
- Включаемые библиотеки сохраняют свои лицензии: AndroidX, Car App Library и Shizuku API (Apache-2.0).
