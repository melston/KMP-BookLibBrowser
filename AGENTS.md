# Developer Agent Guide (`AGENTS.md`)

This guide is designed for AI coding agents and developers working on the **KMP-BookLibBrowser** codebase. It provides an immediate orientation to the architecture, key workflows, build processes, configuration settings, and common pitfalls.

---

## 1. Project Overview & Architecture

**KMP-BookLibBrowser** is a Kotlin Multiplatform (KMP) application and library for browsing and managing EBook libraries.

- **Primary Storage**: Rather than a local database, the active application stores data as JSON files (`books.json` and `cats.json`) in **Dropbox** (inside the `/Apps/EBookLibBrowser/` directory).
- **Core Library & UI**: The primary module is [composeApp](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp), which contains both the shared library UI and platform-specific entry points (Desktop JVM and Android).

### Directory Structure

```text
KMP-BookLibBrowser/
├── build.gradle.kts           # Root gradle configuration
├── settings.gradle.kts        # Defines included modules (currently active: :composeApp)
├── gradle/
│   └── libs.versions.toml     # Version catalog for dependencies and plugins
├── composeApp/                # Main application & library module
│   ├── build.gradle.kts       # Module build script (multiplatform configuration)
│   └── src/
│       ├── commonMain/        # Shared code (business logic, repository, ViewModels, Compose UI)
│       │   ├── kotlin/org/elsoft/bkdb/
│       │   │   ├── models/        # Data models (EBook, Category)
│       │   │   ├── repository/    # BookRepository interface, JsonBookRepository
│       │   │   ├── ui/            # Shared Compose Multiplatform UI components
│       │   │   ├── utils/         # ConfigManager, DropboxService (expect), DuplicateFinder
│       │   │   └── viewmodel/     # BookViewModel and LocalProviders
│       │   └── composeResources/  # Common resources (images, strings, icons)
│       ├── desktopMain/       # Desktop target-specific code
│       │   └── kotlin/org/elsoft/bkdb/
│       │       ├── data/          # JdbcBookRepository (legacy/inactive database implementation)
│       │       ├── utils/         # DropboxService (actual implementation using Java Dropbox SDK)
│       │       └── main.kt        # Desktop app launcher, initializes Java Preferences Settings
│       └── androidMain/       # Android target-specific code
│           └── kotlin/org/elsoft/bkdb/
│               ├── utils/         # DropboxService (actual implementation)
│               └── MainActivity.kt# Android launcher, handles SharedPreferences settings & build seeding
└── sample/                    # Inactive/sample modules (currently commented out in settings.gradle.kts)
    ├── composeApp/
    └── terminalApp/
```

---

## 2. Configuration & Pre-Requisites

The application relies on russhwolf's `multiplatform-settings` to store configuration values. Credentials and system paths must be set up per platform prior to running the app.

### A. Windows configuration (Java Preferences API)
The Desktop app saves configuration in the Windows Registry at:
`HKCU\Software\JavaSoft\Prefs\org\elsoft\bkdb`

You can seed these settings using the provided [setup.reg](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/setup.reg) or via PowerShell:
```powershell
$regPath = "HKCU:\Software\JavaSoft\Prefs\org\elsoft\bkdb"
if (-not (Test-Path $regPath)) { New-Item -Path $regPath -Force | Out-Null }
Set-ItemProperty -Path $regPath -Name "dropbox.app_key" -Value "YOUR_DROPBOX_APP_KEY"
Set-ItemProperty -Path $regPath -Name "dropbox.app_secret" -Value "YOUR_DROPBOX_APP_SECRET"
Set-ItemProperty -Path $regPath -Name "dropbox.refresh_token" -Value "YOUR_DROPBOX_REFRESH_TOKEN"
Set-ItemProperty -Path $regPath -Name "dropbox.root" -Value "YOUR_DROPBOX_ROOT_PATH"
```

### B. Linux Configuration (Java Preferences API)
On Linux, configure credentials by creating/writing to:
`~/.java/.userPrefs/org/elsoft/bkdb/prefs.xml`

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">
<map MAP_XML_VERSION="1.0">
  <entry key="dropbox.app_key" value="YOUR_DROPBOX_APP_KEY"/>
  <entry key="dropbox.app_secret" value="YOUR_DROPBOX_APP_SECRET"/>
  <entry key="dropbox.refresh_token" value="YOUR_DROPBOX_REFRESH_TOKEN"/>
  <entry key="dropbox.root" value="YOUR_DROPBOX_ROOT_PATH"/>
</map>
```
Secure permissions: `chmod 600 ~/.java/.userPrefs/org/elsoft/bkdb/prefs.xml`

### C. Android Configuration (`local.properties` & SharedPreferences)
For Android development, the project uses the `buildkonfig` plugin to seed `SharedPreferences` at build-time.
1. Add the following to your root `local.properties`:
   ```properties
   dropbox.app_key=YOUR_DROPBOX_APP_KEY
   dropbox.app_secret=YOUR_DROPBOX_APP_SECRET
   dropbox.refresh_token=YOUR_DROPBOX_REFRESH_TOKEN
   ```
2. On startup, [MainActivity](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/src/androidMain/kotlin/org/elsoft/bkdb/MainActivity.kt) reads `BuildKonfig` values to initialize Android `SharedPreferencesSettings` if empty.

---

## 3. Active Gradle & Launch Commands

Use these commands to build, run, and publish the project:

### Running the Desktop App
```bash
./gradlew :composeApp:run
```

### Publishing the Library
- **Local Maven Cache**:
  ```bash
  ./gradlew :composeApp:publishToMavenLocal
  ```
- **Maven Central (Sonatype)**:
  Requires setting developer info in [composeApp/build.gradle.kts](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/build.gradle.kts#L182-L211) and signing keys in `gradle.properties`.
  ```bash
  ./gradlew :composeApp:publishAndReleaseToMavenCentral --no-configuration-cache
  ```

---

## 4. Key Implementation Patterns

### Configuration & Settings Access
Settings are managed through the global variable `appSettings` defined in [SettingsFactory.kt](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/src/commonMain/kotlin/org/elsoft/bkdb/utils/SettingsFactory.kt) and exposed via [ConfigManager.kt](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/src/commonMain/kotlin/org/elsoft/bkdb/utils/ConfigManager.kt). 
> [!IMPORTANT]
> `appSettings` is marked `lateinit var` and **must** be initialized in platform-specific launcher code (e.g., `main()` on Desktop or `onCreate()` on Android) before any UI or business logic is instantiated.

### Data Flow & Persistence
1. Platform launcher downloads JSON files (`books.json` and `cats.json`) using the actual platform implementation of [DropboxService](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/src/commonMain/kotlin/org/elsoft/bkdb/utils/DropboxService.kt).
2. [JsonBookRepository](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/src/commonMain/kotlin/org/elsoft/bkdb/repository/JsonBookRepository.kt) is instantiated with the downloaded JSON string content and saving callbacks.
3. [BookViewModel](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/src/commonMain/kotlin/org/elsoft/bkdb/viewmodel/BookViewModel.kt) wraps the repository to supply UI state to the app components using Kotlin Coroutines `StateFlow`.
4. Any modifications (e.g., editing titles, authors, favorites, or read status) automatically serialize back to JSON and upload to Dropbox via `onSave` callbacks.

---

## 5. Developer Warnings & Common Pitfalls

- **Kotlin Version Conflicts**: The root `build.gradle.kts` enforces Kotlin version resolution to avoid compiler/metadata version conflicts (currently pinned to `2.0.21`). Do not change Kotlin plugin versions without verifying resolution strategies in `buildscript`.
- **SQLDelight & Inactive JDBC Code**: While the `sqldelight` plugin is declared and configuration for a SQLite/MySQL database is present, the app currently runs fully on JSON files using `JsonBookRepository`. The [JdbcBookRepository](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/src/desktopMain/kotlin/org/elsoft/bkdb/data/JdbcBookRepository.kt) is legacy/inactive code.
- **Empty Dropbox Library Deadlock**: If credentials are missing or the Dropbox folder is completely empty, startup connection/download checks will fail. Always ensure seeded keys are populated in your local registry/properties file.
