
## v8 — пошук учителів на головному екрані

- Прибрано картку **«Безпека · UID 81»** з головного екрана.
- На її місці додано **«Пошук учителя»** за прізвищем або ПІБ.
- Пошук проходить по всіх 10–11 класах поточного розкладу та враховує активні заміни.
- Для вибраного вчителя показується: поточний урок, клас, предмет, аудиторія і час; наступний урок; повний перелік уроків на сьогодні.
- Якщо вчитель зараз не веде — показується «Зараз уроку немає»; якщо уроків сьогодні немає — «Сьогодні уроків немає».
- Дані відображають місцезнаходження **за розкладом**, а не GPS-фактичне місцезнаходження людини.

# LyceumMobile Android 1.0 — Redmi Note 15 Pro+ — JDK17 — FULL FIXED v8

Нативний Android-застосунок на Kotlin + Jetpack Compose, створений за
функціональною моделлю **LyceumMobile iOS15 iPhone 7 Plus FULL FIXED v6**
та підготовлений для **Redmi Note 15 Pro+ 5G / HyperOS 2**.

**ВАЖЛИВО:** `maven { ... }` не потрібно вводити в PowerShell. Репозиторії вже прописані у `settings.gradle.kts`.

## Зафіксоване середовище

- JDK: **17**
- Gradle: **8.10.2**
- Android Gradle Plugin: **8.8.2**
- Kotlin: **2.0.21**
- compileSdk / targetSdk: **35**
- minSdk: **26**
- package: `ua.edu.cunl.lyceummobile`
- Android versionName: **1.0-redmi15-v8**

## Репозиторії

Прямі `repo.maven.apache.org` і `repo1.maven.org` не використовуються.
Maven Central проходить через JetBrains cache redirector:

`https://cache-redirector.jetbrains.com/maven-central`

Android artifacts використовують Google Maven.

Gradle 8.10.2 також завантажується через JetBrains cache redirector:

`https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-8.10.2-bin.zip`

## Android Studio

1. Розпакуйте ZIP у нову папку.
2. Відкрийте **саме корінь**, де лежать `settings.gradle.kts`, `gradlew.bat` і `app`.
3. `File → Settings → Build, Execution, Deployment → Build Tools → Gradle`.
4. `Gradle JDK → JDK 17`.
5. Натисніть `Sync Project with Gradle Files`.

Перед Sync можна перевірити мережу:

```powershell
.\CHECK_NETWORK.ps1
```

## Збірка на Windows 11

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

Готовий APK:

`app\build\outputs\apk\debug\app-debug.apk`

Для встановлення на один підключений Redmi:

```powershell
.\BUILD_AND_INSTALL.ps1
```

## Функціональність

- 4 офлайн-розклади: звичайний/укриття × чисельник/знаменник;
- вибір 10-А…11-Г;
- поточний і наступний урок;
- Google Apps Script: оголошення, події, заміни;
- alerts.in.ua UID 81;
- збереження останнього підтвердженого стану при мережевій помилці;
- EmergencyScreen та 15-секундний All Clear;
- хвилина мовчання 09:00–09:01 + 60 BPM метроном;
- GitHub/HTTPS `content_manifest.json`;
- SHA-256 всіх 4 розкладів перед активацією;
- previous bundle + rollback;
- API-токен зберігається через Android Keystore;
- HTTP cleartext заборонено.

Застосунок є допоміжним і не замінює офіційні системи оповіщення.


## Windows: Java 25 встановлена замість JDK 17

`BUILD_AND_INSTALL.ps1` v2 не використовує Java 21/25 випадково.
Він шукає JDK 17 у Microsoft OpenJDK, Eclipse Temurin, Oracle,
Zulu, Corretto та Android Studio JBR.

Якщо JDK 17 не знайдено:

```powershell
winget install --id Microsoft.OpenJDK.17 -e --source winget
```

Після встановлення повністю закрийте PowerShell і Android Studio,
відкрийте їх знову та запустіть:

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

Нормальний `java -version` пише версію у stderr. У v2 це обробляється
через `System.Diagnostics.Process`, тому Windows PowerShell 5.1 більше
не перетворює цей вивід на `NativeCommandError`.

## v5 Windows hotfixes

- Fixed Compose import: `androidx.compose.runtime.saveable.rememberSaveable`.
- Removed UTF-8 BOM from `gradlew.bat` (`я╗┐@echo off` issue).
- `BUILD_AND_INSTALL.ps1` now has exactly one UTF-8 BOM for Windows PowerShell 5.1.
- Main build bypasses `gradlew.bat` and calls `GradleWrapperMain` directly through JDK 17.
- Script arguments are parsed manually, avoiding the `SwitchParameter` binding error.
- Easiest Windows start: double-click `START_BUILD_AND_INSTALL.cmd`.


## v7 emergency controls

If APK compilation already succeeded but installation failed, run `INSTALL_EXISTING_APK.ps1` or double-click `START_INSTALL_EXISTING_APK.cmd`. It prints the exact ADB/HyperOS error and retries with non-streaming install when useful. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, run with `-ReplaceExisting` (warning: uninstalling the old package removes its app data/settings).

## Налаштування Google Apps Script і GitHub Raw з ПК

У v9 URL можна передати в уже встановлений LyceumMobile з Windows через USB/ADB — без набору довгих адрес на телефоні і без повторної інсталяції APK.

1. Підключіть Redmi по USB, увімкніть **USB debugging** і підтвердьте RSA-доступ.
2. Двічі запустіть `START_SET_REMOTE_URLS.cmd` або в PowerShell виконайте:

```powershell
.\SET_REMOTE_URLS.ps1
```

3. Вставте по черзі:
   - Google Apps Script URL виду `https://script.google.com/macros/s/.../exec`;
   - GitHub Raw URL виду `https://raw.githubusercontent.com/.../content_manifest.json`.
4. Скрипт знайде підключений телефон, відкриє LyceumMobile і передасть обидві адреси. Застосунок збереже їх у своїх налаштуваннях та запустить оновлення.

Можна також передати URL однією командою:

```powershell
.\SET_REMOTE_URLS.ps1 -GoogleAppsScriptUrl "https://script.google.com/macros/s/.../exec" -GithubManifestUrl "https://raw.githubusercontent.com/.../content_manifest.json"
```

Для кількох підключених Android-пристроїв додайте `-Device "<adb-serial>"`. З міркувань безпеки v9 приймає для цих двох полів лише HTTPS Google Apps Script і GitHub Raw `content_manifest.json`.
