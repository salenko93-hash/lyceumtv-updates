# LyceumMobile Android 1.0 FINAL — Windows 11 → Redmi

## 1. Android Studio

Встановіть:
- Android Studio;
- Android SDK Platform 35;
- Android SDK Build-Tools;
- Android SDK Platform-Tools;
- JDK 17.

У Gradle settings Android Studio виберіть **JDK 17**.

## 2. Перевірка мережі

```powershell
.\CHECK_NETWORK.ps1
```

Очікується HTTP 200 для JetBrains Maven cache та Google Maven.

## 3. Sync

В Android Studio:
`File → Sync Project with Gradle Files`.

Проєкт має власний Gradle wrapper і не використовує випадкову
глобальну версію Gradle.

## 4. Збірка

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

APK:
`app\build\outputs\apk\debug\app-debug.apk`

## 5. Встановлення на Redmi

Увімкніть Developer options → USB debugging, підключіть телефон,
підтвердьте RSA fingerprint і виконайте:

```powershell
.\BUILD_AND_INSTALL.ps1
```

Скрипт використовує `adb install -r`, тому оновлює застосунок без
попереднього `adb uninstall`.
