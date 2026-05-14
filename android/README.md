# HealthBot Android App

Android app for the **AIA Health** insurance chatbot system.

## Open in Android Studio

1. Open Android Studio (Hedgehog or later)
2. **File → Open** → select `/path/to/healthbot/android`
3. Wait for Gradle sync to complete
4. Run on emulator (AVD, API 26+) or a physical device

## Requirements

- Android Studio Hedgehog+
- Android SDK 34
- Backend running on `http://localhost:8080` (the emulator maps this to `10.0.2.2:8080` automatically)

## Build Notes

- `gradle.properties` must be present at the project root — it sets `android.useAndroidX=true` and `android.enableJetifier=true`. The file is included in the repo; do not delete it.
- App theme uses `Theme.MaterialComponents.DayNight.NoActionBar` so that each screen can host its own `Toolbar` via `setSupportActionBar()`.

## Screens

| Screen | Description |
|--------|-------------|
| `MainActivity` | AIA Health home — animated AI banner, quick-access feature cards |
| `ChatbotActivity` | AI health chatbot conversation |
| `DoctorListActivity` | Browse available doctors |
| `VideoConsultationActivity` | Mock video consultation with timer |
| `AiConsultationActivity` | Structured AI doctor consultation → prescription |
| `PharmacyActivity` | View prescription and complete purchase |
| `AppointmentActivity` | Book a hospital appointment |

## Language

Toggle between English and Chinese using the language button in the top-right corner of each screen. The preference is persisted in `SharedPreferences`.

## Demo User

The app uses **userId = 1 (Alice Chen)** by default for all API calls.
