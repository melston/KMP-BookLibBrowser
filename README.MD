# KMP library

Kotlin Multiplatform Library

### Run Sample App

 - Desktop JVM: `./gradlew :sample:composeApp:run`
 - Linux/Macos/Windows native: 
   `./gradlew :sample:terminalApp:runDebugExecutable[architecture]`
 - Android: `open project in Android Studio and run the sample app`

#### Windows Pre-Requisites

Must have set up the following (Windows):

```bash
# 1. Define the Registry Path used by the Java Preferences node
$regPath = "HKCU:\Software\JavaSoft\Prefs\org\elsoft\bkdb"

# 2. Create the key path if it does not exist
if (-not (Test-Path $regPath)) {
New-Item -Path $regPath -Force | Out-Null
}

# 3. Set the Dropbox configuration properties
Set-ItemProperty -Path $regPath -Name "dropbox.app_key" -Value "YOUR_DROPBOX_APP_KEY"
Set-ItemProperty -Path $regPath -Name "dropbox.app_secret" -Value "YOUR_DROPBOX_APP_SECRET"
Set-ItemProperty -Path $regPath -Name "dropbox.refresh_token" -Value "YOUR_DROPBOX_REFRESH_TOKEN"
Set-ItemProperty -Path $regPath -Name "dropbox.root" -Value "YOUR_DROPBOX_ROOT_PATH"
```
If we ever go back to using a database, this needs to be set as well.
```bash
# 4. Set the DB configuration so ConfigManager.isConfigured() evaluates to true
Set-ItemProperty -Path $regPath -Name "db.url" -Value "jdbc:sqlite:C:/path/to/local/database.db" # or your specific DB connection URL
Set-ItemProperty -Path $regPath -Name "db.user" -Value "your_db_user"
Set-ItemProperty -Path $regPath -Name "db.password" -Value "your_db_password"
Set-ItemProperty -Path $regPath -Name "viewer_command" -Value "cmd /c start `"`"`" `"%f`""
```

Alternatively, you can use a `.reg` file (the `db.xxx` entries are for database use)
```text
Windows Registry Editor Version 5.00

[HKEY_CURRENT_USER\Software\JavaSoft\Prefs\org\elsoft\bkdb]
"dropbox.app_key"="YOUR_DROPBOX_APP_KEY"
"dropbox.app_secret"="YOUR_DROPBOX_APP_SECRET"
"dropbox.refresh_token"="YOUR_DROPBOX_REFRESH_TOKEN"
"dropbox.root"="YOUR_DROPBOX_ROOT_PATH"
"db.url"="jdbc:sqlite:C:/path/to/local/database.db"
"db.user"="your_db_user"
"db.password"="your_db_password"
"viewer_command"="cmd /c start \\\"\\\" \\\"%f\\\""
```

#### Linux Pre-Requisites
On Linux, the Java Preferences API (Preferences.userRoot().node("org.elsoft.bkdb")) 
stores user-level preferences in an XML file within the user's home directory.

Specifically, the preferences are saved at: 
`~/.java/.userPrefs/org/elsoft/bkdb/prefs.xml` 
(where ~ is your home directory)

##### Step-by-Step Configuration on Linux
To configure this on Linux, you can create the directory structure and write the 
XML file directly using the following terminal commands:
```bash
mkdir -p ~/.java/.userPrefs/org/elsoft/bkdb
```
Then populate a file (`~/.java/.userPrefs/org/elsoft/bkdb/prefs.xml`) with`:
```text
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">
<map MAP_XML_VERSION="1.0">
  <!-- Dropbox configuration -->
  <entry key="dropbox.app_key" value="YOUR_DROPBOX_APP_KEY"/>
  <entry key="dropbox.app_secret" value="YOUR_DROPBOX_APP_SECRET"/>
  <entry key="dropbox.refresh_token" value="YOUR_DROPBOX_REFRESH_TOKEN"/>
  <entry key="dropbox.root" value="YOUR_DROPBOX_ROOT_PATH"/>

  <!-- Database & System settings -->
  <entry key="db.url" value="jdbc:sqlite:/home/username/path/to/database.db"/>
  <entry key="db.user" value="your_db_username"/>
  <entry key="db.password" value="your_db_password"/>
  <entry key="viewer_command" value="xdg-open %f"/>
</map>
```
Finally secure the file permissions of the file with the following:
```bash
chmod 600 ~/.java/.userPrefs/org/elsoft/bkdb/prefs.xml
```

#### Android Pre-Requisites
For Android, the application uses **SharedPreferences** as the underlying 
preferences storage and provides an elegant build-time mechanism to 
pre-populate (seed) those settings.

Here are the two ways to manage preferences for Android:

###### Method 1: Build-Time Seeding via `local.properties` (Recommended)

The project is configured with the `com.codingfeline.buildkonfig` 
plugin in [build.gradle.kts](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/build.gradle.kts#L19-L34). This plugin automatically reads values from `local.properties` 
at the root of the project and compiles them into a generated 
`BuildKonfig` class.

1. Open (or create) the `local.properties` file at the root of your project:
   `c:\Projects\Kotlin\KMP-BookLibBrowser\local.properties`

2. Add your Dropbox credentials to this file:
   ```properties
   dropbox.app_key=YOUR_DROPBOX_APP_KEY
   dropbox.app_secret=YOUR_DROPBOX_APP_SECRET
   dropbox.refresh_token=YOUR_DROPBOX_REFRESH_TOKEN
   ```

3. When the app is built and runs on your Android device, [MainActivity.kt](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/src/androidMain/kotlin/org/elsoft/bkdb/MainActivity.kt#L35-L39) will automatically seed the settings if they are currently empty:
   ```kotlin
   if (ConfigManager.dropboxAppKey.isEmpty()) {
       ConfigManager.dropboxAppKey = BuildKonfig.DROPBOX_APP_KEY
       ConfigManager.dropboxAppSecret = BuildKonfig.DROPBOX_APP_SECRET
       ConfigManager.dropboxRefreshToken = BuildKonfig.DROPBOX_REFRESH_TOKEN
   }
   ```
   *Note: Using this method ensures you bypass the initial "empty repository" 
   check deadlock because valid credentials will already be present when the
   app tries to download the library on startup.*

---

###### Method 2: Runtime Storage (`SharedPreferences`)

At runtime, [MainActivity.kt](file:///c:/Projects/Kotlin/KMP-BookLibBrowser/composeApp/src/androidMain/kotlin/org/elsoft/bkdb/MainActivity.kt#L31-L32) 
wraps the default Android SharedPreferences framework:
```kotlin
val sharedPrefs = getSharedPreferences("ebook_prefs", MODE_PRIVATE)
appSettings = SharedPreferencesSettings(sharedPrefs)
```

If you need to view or modify these preferences on a device directly:
- **Sandbox Path**: The values are stored in a standard Android XML preference
  file under:
  `/data/data/org.elsoft.bkdb/shared_prefs/ebook_prefs.xml`
- **Accessing the File**: On a rooted device or inside a debugging/developer 
  emulator, you can access and read/write this file directly via ADB:
  ```bash
  adb shell
  run-as org.elsoft.bkdb
  cat shared_prefs/ebook_prefs.xml
  ```
- **In-App GUI Setup**: Once seed credentials have been initialized 
  (via Method 1), you can also edit these fields at runtime directly 
  inside the application's Setup Screen UI, which updates the values 
  inside `ebook_prefs.xml`.

### Publish to MavenLocal

1) Run `./gradlew :composeApp:publishToMavenLocal`
2) Open `~/.m2/repository/org/elsoft/bkdb/`

### Publish to MavenCentral

1) Create an account and a namespace on Sonatype:  
   https://central.sonatype.org/register/central-portal/#create-an-account
2) Add developer id, name, email and the project url to  
   `./composeApp/build.gradle.kts`
3) Generate a GPG key:  
   https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/#generating-a-gpg-key-pair
   ```
   gpg --full-gen-key
   gpg --keyserver keyserver.ubuntu.com --send-keys XXXXXXXX
   gpg --export-secret-key XXXXXXXX > XXXXXXXX.gpg
   ```
4) Add these lines to `gradle.properties`:
   ```
   signing.keyId=XXXXXXXX
   signing.password=[key password]
   signing.secretKeyRingFile=../XXXXXXXX.gpg
   mavenCentralUsername=[generated username]
   mavenCentralPassword=[generated password]
   ```
5) Run `./gradlew :composeApp:publishAndReleaseToMavenCentral --no-configuration-cache`
