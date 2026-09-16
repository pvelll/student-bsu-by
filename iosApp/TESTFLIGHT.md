# Публикация iOS-сборки в TestFlight

Требуется активная подписка Apple Developer Program (99 $/год); сам TestFlight бесплатный.

## Один раз

1. В `Configuration/Config.xcconfig` проверить `TEAM_ID` (App Store Connect → Membership)
   и `BUNDLE_ID`. Bundle ID должен быть зарегистрирован в developer.apple.com → Identifiers
   (Xcode с Automatic signing сделает это сам при первой сборке на устройство).
2. В App Store Connect → My Apps → «+» создать приложение с этим Bundle ID
   (название «Кабинет студента БГУ», язык — русский, SKU любой).
3. В App Store Connect → App Information указать Privacy Policy URL (обязателен для
   внешнего тестирования) и контактный e-mail.

## Каждая сборка

1. Поднять номер сборки: `CURRENT_PROJECT_VERSION` в `iosApp.xcodeproj` (сейчас 25,
   совпадает с Android `versionCode`); `MARKETING_VERSION` — версия для пользователей.
2. Xcode → схема `iosApp`, устройство «Any iOS Device (arm64)» → Product → Archive.
   Kotlin-фреймворк соберётся сам на этапе «Compile Kotlin Framework».
3. Organizer → Distribute App → App Store Connect → Upload.
4. App Store Connect → TestFlight: сборка появится после обработки (10–30 минут).
   - Internal Testing: до 100 пользователей из команды, без проверки Apple.
   - External Testing: группа тестеров по e-mail или по публичной ссылке (до 10 000
     человек); первая сборка проходит Beta App Review (обычно до суток).
5. Тестеры устанавливают приложение TestFlight из App Store и открывают приглашение.
   Сборка действует 90 дней.

## Что уже подготовлено в проекте

- Иконка 1024×1024 без прозрачности (`Assets.xcassets/AppIcon.appiconset`).
- `PrivacyInfo.xcprivacy` с декларацией используемых API (UserDefaults, метки времени
  файлов, время загрузки системы) — обязателен для загрузки с 2024 года.
- `UIRequiredDeviceCapabilities = arm64`, `UIBackgroundModes = fetch` для фоновой
  синхронизации, версии берутся из настроек проекта, `ITSAppUsesNonExemptEncryption = NO`.
- Отладочные хуки симулятора активны только в Debug-бинарнике.
