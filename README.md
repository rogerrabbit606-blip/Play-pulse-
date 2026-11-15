# PlayPulse — Play Store Opportunity & MRR Intelligence

PlayPulse is an Android analytics and portfolio command center for discovering abandoned or under-maintained apps across Google Play, Galaxy Store, and Amazon Appstore. It helps model subscription revenue, generate Gemini-powered competitive playbooks, track opportunities, and export custom CSV reports.

## Core capabilities

- **Executive dashboard:** MRR, projected ARR, user growth, active subscribers, churn, forecasts, retention charts, and configurable KPI alerts.
- **Market opportunity radar:** Curated dormant-app opportunities and store benchmarks, with installation, churn, and revenue diagnostics.
- **Portfolio pipeline:** Track opportunities through `scouted` → `redesigning` → `launched` → `archived`.
- **Subscriptions and payment gateways:** Manage recurring plans, normalize weekly/monthly/annual pricing, and model ROI, conversion, churn, and LTV.
- **AI strategy lab:** Generate structured Gemini strategy briefs with competitive-moat scoring, pricing guidance, redesign priorities, ASO recommendations, and retention ideas.
- **CSV reports:** Build opportunity, benchmark, subscription, and executive KPI datasets, save report presets, and export through Android's Storage Access Framework.
- **Authentication:** Google sign-in using Firebase Authentication and Jetpack Credential Manager.

## Technology

- Kotlin 2.2.10, Android Gradle Plugin 9.1.1, Java 11
- Jetpack Compose, Material 3, Navigation Compose
- MVVM and repository pattern with `StateFlow`
- Firebase Authentication, Cloud Firestore, Firebase AI Logic, and App Check
- Gemini `gemini-3.8-flash` with `gemini-3.5-flash-lite` fallback
- JUnit 4, Robolectric, Compose UI tests, and Firebase Rules tests

## Architecture

Application source is under `app/src/main/java/com/example/`:

- `MainActivity.kt` — App Check setup, auth-gated navigation, and app shell
- `data/` — Firestore repository, AI insights, market catalog, and CSV generation
- `firebase/` — App Check and Firestore error handling
- `model/` — Analytics, subscription, opportunity, and strategy models
- `ui/` — ViewModel, Compose screens, components, and theme

The app uses user-scoped Firestore collections under `/users/{userId}`. Security rules enforce user isolation, field allowlists, type/range constraints, and immutability for archived tracked opportunities. The application uses a named Firestore database rather than the unprovisioned `(default)` database.

## Requirements and configuration

- Android SDK for compile/target API 36 (compile SDK minor API 36.1)
- Java 11
- Firebase project configuration and the named Firestore database configured for this app
- Local secrets/configuration in `.env` and Firebase configuration as required by the Android Google Services plugin. Do not commit credentials or debug tokens.

The app's Android namespace is `com.example`; its application ID is `com.aistudio.playpulse.auopgd`. Firebase emulator ports are Firestore `127.0.0.1:8085` and Auth `127.0.0.1:9099`.

## Build and test

Run these commands from the repository root:

```bash
# Compile the debug APK
gradle :app:assembleDebug

# Run local JVM and Robolectric tests
gradle :app:testDebugUnitTest

# Run Firebase security rules tests (start the Firestore emulator first)
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js

# Run Kotlin repository/rules integration tests with the Firebase emulators available
gradle :app:testDebugUnitTest --tests '*Rule*'
```

The emulator configuration is in `firebase.json`. Rules are in `firestore.rules`, with the Firebase blueprint in `firebase-blueprint.json`.

## GitHub Actions

GitHub Actions workflows can be added under `.github/workflows/` as YAML files. A workflow is made up of event triggers, jobs that run on GitHub-hosted or self-hosted runners, and steps that execute commands or reusable actions. Typical automation for this project includes building and testing pull requests and the default branch. Workflows can be triggered by repository events, schedules, or manual dispatch.

## AI Studio

[Open the PlayPulse AI Studio app](https://ai.studio/apps/e383fd19-344a-4f70-a94d-3929de17957d?fullscreenApplet=true)
