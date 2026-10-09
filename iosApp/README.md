# Apuntes for iOS

Native SwiftUI app for iPhone and iPad (iOS 16+) backed by the Kotlin `sharedKit`
framework. Android application sources and screens are unchanged.

## Feature coverage

| Android workflow | iOS implementation |
| --- | --- |
| Series setup | Two named teams, odd best-of 1-199, target 1-100000, score presets |
| Scoreboard | Positive scores up to 10000, independent team columns, games won, game and series winner sheets |
| Score correction | Edit points or team, delete a row, undo latest visible row, retained audit events |
| Round robin | Every pair, Android head-to-head/differential tie-breaks, top-two championship |
| Elimination | Seed-order pairing, byes without artificial wins, special three-team second-chance format |
| Standings | Wins, losses, matches played, league points, points against, differential, win percentage, medals |
| Recovery | Multiple unfinished series and tournaments; state persists after every action |
| Abandonment | Confirmation, history retained, further scoring disabled |
| History | Completed and abandoned events, match totals, games and scoring audit |
| Ads | Google Mobile Ads banner, UMP consent and privacy options, debug test ads |
| Languages | English and Spanish interface |

The iOS engine lives in `shared/src/commonMain/kotlin/com/apuntes/shared/mobile`.
It follows the Android Room controllers' rules, but does not replace the Android
implementation or share an on-disk database with it. Android data migration and
cross-device synchronization are not included.

## Build on a Mac

Install Xcode 26.0.1 (the CI-pinned version), its iOS simulator runtime, JDK 17+, Android SDK
platform 36 and build tools 36.0.0, and XcodeGen plus WebP (`brew install xcodegen webp`). Set
`ANDROID_HOME` to the SDK location and `JAVA_HOME` to your JDK. Preparation updates
only `sdk.dir` in the tracked `local.properties` to use the Mac's SDK path.
The preparation script rejects Xcode or iOS SDK versions below 26, as required
for App Store uploads since April 28, 2026. This does not change the app's minimum
supported device version of iOS 16. Newer Xcode versions need a separate Kotlin
compatibility check before changing the CI pin.

From the repository root:

```sh
bash iosApp/scripts/prepare.sh
open iosApp/Apuntes.xcodeproj
```

Preparation builds the framework, converts the existing Android icon for Apple's
asset catalog, and generates the Xcode project from `iosApp/project.yml`. The
generated project and icon are ignored. Run preparation again after changing
the project specification. Xcode also rebuilds the shared framework before app
builds. Simulator builds need no Apple signing team.

```sh
bash iosApp/scripts/test-simulators.sh
```

This runs persistence and UI tests on available iPhone and iPad simulators. The
UI tests use an isolated save folder and retain screenshots in `.xcresult` files.
Both Intel and Apple Silicon simulator slices are included in the framework.

## Windows / GitHub Actions

The **Build and Test iOS** workflow builds the framework and app on macOS, runs
native shared tests plus iPhone/iPad tests, and creates an unsigned device archive.
After these changes are committed and pushed, it runs on `main` and pull requests;
it can also be dispatched manually. No workflow was dispatched by the coding agent.

Artifacts include test results/screenshots, `sharedKit.xcframework`, and an unsigned
`Apuntes.xcarchive`. An unsigned archive is not an installable IPA or a TestFlight upload.

## Release configuration

Use `iosApp/Config/Local.xcconfig` (ignored by git) with values from
`Config/Local.xcconfig.example`. Set your registered iOS bundle ID and Apple team.
For ads, register the iOS app in AdMob and supply its iOS app and banner IDs;
Android IDs cannot be reused. Configure the UMP consent message in AdMob.
Debug uses Google's banner test ID. Release ads remain disabled until a real
banner ID is configured; the default app identifier is Google's sample.

Select the signing team in Xcode, run on an actual iPhone, then Archive and
Distribute through Xcode Organizer. Review the app's privacy disclosures against
the SDK configuration and add your privacy-policy URL in App Store Connect.
The project includes the shared library's UserDefaults API reason; the Google
SDKs supply their own manifests. No ATT permission is requested by this app.

## Verification status

Kotlin engine tests and Android regression tests can run on Windows. SwiftUI
compilation, Xcode linking, simulator tests, screenshots, ads and device signing
must be verified on a Mac. Passing Kotlin compilation alone does not establish
that the iOS app is ready for release. The macOS workflow has been prepared but
has not yet been run against these local changes.
