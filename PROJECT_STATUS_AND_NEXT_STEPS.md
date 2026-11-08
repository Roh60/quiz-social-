# QuizPoll Community – source update report

This is a source-code update based on the uploaded ZIP. It is **not** a claim that the app has been built, deployed or approved for Google Play.

## Implemented in this update

### Advertising
- Selected ad networks reduced to **Google AdMob + InMobi only**.
- Added Google Mobile Ads SDK 25.5.0, InMobi AdMob mediation adapter 11.5.0.0 and UMP 4.0.0.
- Added UMP consent refresh on app launch and a privacy choices entry point.
- Added real AdMob Banner, Interstitial and Rewarded ad loading using official test ad units by default.
- Added InMobi adapter presence check and AdMob mediation setup documentation.
- Added build-time AdMob app/ad-unit properties. Safe test mode remains the default.
- Disabled fake sponsored feed cards. Native ad rendering is not enabled.
- Rewarded ads do not grant points or cash; the UI explicitly says reward points are not enabled yet.

### Backend and security source
- Added Firebase Cloud Functions callable source for poll voting, quiz attempt scoring, answer review, like toggling, follow/unfollow, referral claims and account deletion.
- Android repository now calls the backend for those operations.
- Added Firebase App Check initialization with Debug provider for debug builds and Play Integrity for release builds.
- Hardened Firestore client writes for poll votes, quiz attempts, leaderboards, private answer keys, likes and follow relationships.
- New quiz writes store public questions separately from private answer-key documents.
- Added a one-time migration script for old quiz documents that still embed `correctAnswers` in public question documents.
- Improved account deletion cleanup for owned posts/quizzes/polls, attempts, referrals, leaderboard rows, comments/replies/likes, follows, blocks and authentication identity.

### Community data integrity
- Removed fabricated leaderboard users and synthetic per-quiz leaderboard entries.
- Disabled automatic creation of sample quizzes and polls.
- Replaced fake Explore educator cards with real Firestore user suggestions.
- Replaced the fake daily question and fake poll with real community quiz/poll content.
- Removed fabricated Cloud Run share URLs; share text now directs users to find content in the app.

### Release and documentation
- Updated GitHub workflows to install Gradle directly (the ZIP does not contain Gradle wrapper scripts/JAR).
- Added keystore-secret decoding to release APK/AAB workflows.
- Added Firebase Hosting privacy and deletion pages.
- Updated privacy policy, terms, Play Console notes and deployment instructions to mention only AdMob and InMobi.

## Blocking issues – do these before Firebase testing

1. **Firebase package check passes:** `app/google-services.json` contains a QuizPoll client matching `com.aistudio.quizpoll.qpxmrk` (it is one of several Android clients in the file). The `scripts/check-firebase-config.js` check passes.
2. Confirm the named Firestore database `ai-studio-android-quizpoll-bedc2cc0-96aa-403c-a3e2-c286721cd11b` exists in Firebase project `gen-lang-client-0484678628`.
3. Select that exact Firebase project with `firebase use --add` before deploying anything.
4. Register the QuizPoll app in Firebase App Check, register the debug token for debug builds, and configure Play Integrity/SHA-256 for release. Callable Functions currently enforce App Check.
5. Take a Firestore backup and run `node functions/scripts/migrate-legacy-answer-keys.js` once before public release.

## Still incomplete

- Real InMobi/AdMob publisher approval, AdMob mediation group setup, InMobi app/placement mapping and live ad testing.
- Native feed ad renderer.
- Persistent, server-verified rewarded points/rewards.
- Daily five-question challenge, daily streak, weak-topic analytics and retry-mistakes feature.
- Friend challenges and study groups.
- Separate secure owner/admin moderation dashboard and report review workflow.
- More complete rate-limiting and abuse controls for comments, notifications and some client-updated activity counters.
- Public hosting deployment and final HTTPS privacy/account-deletion URLs.
- Signed release keystore creation and GitHub Secrets setup.
- Full Android build, emulator tests, real-device tests, Firebase Emulator rule tests and deployed backend tests.
- Google Play Data Safety verification, store screenshots, content rating and production submission.

## Validation performed here

- JavaScript syntax checks passed for Cloud Functions and the legacy answer-key migration script.
- JSON, TOML and GitHub Actions YAML files parsed successfully.
- Static source checks performed for the updated callable references, ad dependencies, package IDs and removal of fake share URLs from runtime code.
- **No Gradle build or Android test was run** because this environment does not have Gradle installed and the source ZIP has no `gradlew` wrapper script/JAR.
- No Firebase deployment, AdMob/InMobi dashboard connection, APK generation or AAB signing was performed.

## Stage 1 – hardening (done, not yet compiled or deployed)

- Added Gradle wrapper (`gradlew` + `gradle-wrapper.jar`, Gradle 9.3.1); CI workflows now call `./gradlew`.
- Release build: R8 minify + resource shrinking enabled, with keep rules for Firestore models, Room, Firebase, AdMob and InMobi in `app/proguard-rules.pro`. **Test the release build on a real device before publishing.**
- Firestore rules: notification creates are validated (allowed fields, allowed types, no self-notify, message size cap, `isRead=false`); post `commentCount` and comment `likeCount`/`replyCount` can only change by one step.
- Firestore rules were not run against the emulator in this environment. Run `firebase emulator:exec` with `firestore.test.js` before deploying.
