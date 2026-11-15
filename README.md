# PlayPulse

**Discover app-store opportunities, model recurring revenue, and plan what to build next.**

PlayPulse is an Android portfolio and analytics app for evaluating under-maintained mobile apps, tracking subscription business metrics, and turning market signals into practical product strategy.

> PlayPulse is an analytics and planning tool. Store availability, market data, forecasts, and AI-generated recommendations should be independently verified before making business or investment decisions.

## At a glance

- **Opportunity radar** — review curated app opportunities and store benchmarks.
- **Portfolio tracking** — manage an opportunity pipeline from scouting to launch or archive.
- **Revenue analytics** — explore MRR, projected ARR, subscriber growth, churn, retention, and forecasts.
- **Subscription planning** — compare recurring plans and model conversion, ROI, churn, and customer lifetime value.
- **AI strategy lab** — generate structured strategy briefs and predictive insights using Firebase AI Logic.
- **CSV reports** — build and export opportunity, benchmark, subscription, and executive KPI reports.
- **Google sign-in** — authenticate with Firebase Authentication and Jetpack Credential Manager.

## Screens and capabilities

| Area | What it provides |
| --- | --- |
| Dashboard | Portfolio KPIs, forecasts, retention charts, and configurable alerts |
| Opportunities | App catalog, store benchmarks, opportunity analytics, AI scouting, and portfolio stages |
| Monetization | Subscription plan management, payment gateway configuration, and revenue modeling |
| Insights | AI-generated competitive strategy briefs and portfolio forecasts |
| Reports | Configurable CSV datasets, saved report presets, and system document export |
| Authentication | Google sign-in and account-gated access to user data |

## Screenshots

Screenshots are not included yet. Add approved, privacy-safe captures under `docs/images/` and link them here. Before publishing a screenshot, remove real account names, email addresses, user-generated content, Firebase project details, debug tokens, and any other identifying information.

<!-- Example after adding sanitized images:
![PlayPulse dashboard](docs/images/dashboard.png)
-->

## Technology

- Kotlin, Java 11, Gradle Kotlin DSL
- Jetpack Compose, Material 3, Navigation Compose
- MVVM, ViewModel, and `StateFlow`
- Firebase Authentication, Cloud Firestore, Firebase AI Logic, and App Check
- Gemini models through Firebase AI Logic
- JUnit, Robolectric, Compose UI tests, and Firebase Rules tests

## Project layout

```text
app/                     Android application and tests
firebase.json            Firebase emulator and rules configuration
firestore.rules          Firestore security rules
firestore.test.js        JavaScript Firestore rules tests
firebase-blueprint.json  Firestore schema blueprint
```

Application code is organized under `app/src/main/java/com/example/` into data repositories, Firebase helpers, models, ViewModels, Compose screens, components, and theme.

## Getting started

### Requirements

- Android Studio with a compatible Android SDK (API 36)
- Java 11
- Node.js for the JavaScript Firestore rules tests
- Access to a Firebase project configured for this app if you want to run Firebase-backed features

### Configure Firebase locally

1. Open the project in Android Studio and allow Gradle sync to complete.
2. Configure a Firebase project for Android using this app's application configuration in your local development environment.
3. Set up the named Firestore database and deploy or validate the rules in `firestore.rules`.
4. Keep local credentials and configuration out of commits. Never put API keys, service-account credentials, signing keys, debug tokens, personal account details, or production user data in source files or screenshots. See [SECURITY.md](SECURITY.md).
5. For emulator-backed work, use the emulator configuration already defined in `firebase.json`; do not point test runs at production data.

Firebase setup values differ by developer and environment, so this repository intentionally does not publish private credentials or a copy-paste secrets file. Do not treat client-side Firebase configuration as an authorization control: enforce access through Firebase Security Rules and provider restrictions.

### Build and test

Run from the repository root:

```bash
# Build the debug APK
gradle :app:assembleDebug

# Run local JVM and Robolectric tests
gradle :app:testDebugUnitTest

# Run JavaScript Firestore rules tests with the Firestore emulator running
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js

# Run Kotlin Firestore repository/rules integration tests with emulators running
gradle :app:testDebugUnitTest --tests '*Rule*'
```

The repository's Firebase emulator configuration uses local Firestore and Auth services. Use only emulator or disposable test data for development and tests.

## Data and privacy

- Firestore user data is intended to be scoped under `/users/{userId}` and protected by the rules in `firestore.rules`.
- Review and test security rules whenever changing data models, collection paths, or access patterns.
- Do not commit credentials, private signing material, access tokens, debug tokens, personal data, or production exports.
- Avoid using real user data in issue reports, sample data, tests, screenshots, and AI prompts.
- AI output can be inaccurate; do not send confidential or personally identifying information to AI features.
- Review Firebase provider settings, App Check enforcement, API restrictions, and billing controls in your own Firebase project.

See [SECURITY.md](SECURITY.md) for reporting and secret-handling guidance.

## GitHub Actions

There are no workflow files documented here yet. Workflows can be added under `.github/workflows/` to build and test pull requests and the default branch. Keep CI credentials in repository or environment secrets, grant workflows only the permissions they need, and avoid printing secrets in logs.

## AI Studio

[Open PlayPulse in AI Studio](https://ai.studio/apps/e383fd19-344a-4f70-a94d-3929de17957d?fullscreenApplet=true)
