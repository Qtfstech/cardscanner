# Card Scanner

An Android business card scanner in the style of CamCard: photograph a card, the app reads it
on the phone (no internet or account needed), fills in the contact fields for you to check,
and keeps a searchable list of every card you've scanned.

## Features

- **Projects**: group cards by event or batch. Start a new project any time, rename it, or delete it
  (with its cards and photos). Cards you scanned before projects existed are kept in "My cards".
- **Upload many photos at once** into a project from the gallery; each is read and saved as a card.
- **Export to Excel**: each project exports as a real .xlsx workbook with Company as the first column,
  then Name, Job title, Phone, Email, Website, Address and Notes.
- **Scan** with the camera (card-shaped guide frame, flash toggle).
- **On-device text recognition** with Google ML Kit; card photos never leave the phone.
- **Smart field detection**: name, job title, company, several phones and emails, website and address.
  Fax numbers are skipped; the company falls back to the email domain when it isn't printed.
- **Review and edit** every field before saving, company first; "Read text again" re-runs recognition.
- **Card list** per project, company first, with search across company, name, email and phone digits.
- **Card details** with one-tap call, email, website and map.
- **Save to phone contacts** (opens your Contacts app pre-filled, so no contacts permission is needed).
- **Share** a single card or a whole project as a vCard.

## Build and run

Requirements: Android Studio Ladybug (2024.2) or newer, JDK 17+, a phone or emulator on Android 8.0 (API 26)+.

1. Open this folder in Android Studio (File > Open) and let Gradle sync.
2. Press Run, or build from a terminal with `./gradlew assembleDebug`.
   The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.
3. Parser unit tests: `./gradlew testDebugUnitTest`.

If you push this folder to GitHub, the included workflow (`.github/workflows/build.yml`) builds the
APK on every push and attaches it to the run as a downloadable artifact.

## Project layout

```
app/src/main/java/com/cardscanner/app/
  MainActivity.kt          Navigation between the four screens
  data/                    Room database (Card entity, DAO, repository)
  ocr/CardParser.kt        Turns recognized lines into contact fields (pure Kotlin, unit tested)
  ocr/TextRecognizer.kt    ML Kit on-device text recognition
  ui/list, scan, edit, detail   Jetpack Compose screens
  ui/CardsViewModel.kt     View models
  ui/projects              Project list and new / rename / delete dialogs
  util/ContactExport.kt    Contacts intent, vCard and Excel export, call / email / map actions
  util/ExcelWriter.kt      Minimal .xlsx writer (unit tested)
```

## Tech

Kotlin 2.0, Jetpack Compose (Material 3), CameraX 1.4, ML Kit Text Recognition 16, Room 2.6, Coil,
Navigation Compose. minSdk 26, targetSdk 35.
