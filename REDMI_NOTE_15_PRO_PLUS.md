# LyceumMobile — Redmi Note 15 Pro+ 5G / JDK 17

Ця збірка створена за функціональною моделлю **LyceumMobile iOS15 iPhone 7 Plus FULL FIXED v6** і адаптована для Redmi Note 15 Pro+ 5G.

## Цільовий телефон

- Redmi Note 15 Pro+ 5G
- Xiaomi HyperOS 2
- дисплей 6.83", 2772×1280, до 120 Гц
- застосунок адаптивний: не прив'язаний до фізичної роздільності пікселів

## Середовище збірки

- JDK 17 — обов'язково
- Gradle 8.10.2
- Android Gradle Plugin 8.8.2
- Kotlin 2.0.21
- compileSdk 35
- targetSdk 35
- minSdk 26

## Найпростіший запуск на Windows

1. Розпакуйте ZIP у короткий шлях, наприклад `C:\Android\LyceumMobile_Redmi15`.
2. Не вводьте в PowerShell команди `maven { ... }` — це фрагмент Gradle-конфігурації, а не команда Windows.
3. Для автоматичної перевірки JDK 17 і збірки APK запустіть:

```powershell
.\BUILD_AND_INSTALL.ps1 -BuildOnly
```

4. APK після успішної збірки:

`app\build\outputs\apk\debug\app-debug.apk`

5. Для встановлення через USB на Redmi увімкніть «Параметри розробника» та «Налагодження USB», підключіть телефон і запустіть:

```powershell
.\BUILD_AND_INSTALL.ps1
```

## В Android Studio

Відкривайте **корінь проєкту**, де лежать `settings.gradle.kts`, `gradlew.bat` і папка `app`.

У `Settings → Build Tools → Gradle` виберіть **Gradle JDK = 17**.

## Функціональна відповідність iOS v6

- 5 вкладок: Головна, Розклад, Оголошення, Пам'ять, Налаштування;
- 10-А…11-Г;
- чисельник / знаменник;
- звичайний і укриттєвий розклад;
- поточний і наступний урок;
- заміни / оголошення / події через Google Apps Script;
- alerts.in.ua UID 81;
- екран повітряної тривоги та 15-секундний «Відбій»;
- хвилина мовчання 09:00–09:01 і метроном 60 BPM;
- GitHub / HTTPS content_manifest.json;
- SHA-256 перевірка 4 розкладів перед активацією;
- відкат на попередній комплект;
- токен alerts.in.ua в Android Keystore;
- лише HTTPS.

Перевірка alerts.in.ua в цій версії, як і в iOS v6, активна під час відкритого застосунку. Офіційні канали оповіщення залишаються основними.
