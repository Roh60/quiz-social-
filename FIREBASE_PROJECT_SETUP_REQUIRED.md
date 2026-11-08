# Firebase project configuration – verification status

The uploaded `app/google-services.json` contains a matching Android client for this app's Gradle `applicationId`:

- QuizPoll Android package: `com.aistudio.quizpoll.qpxmrk`
- Firebase project ID in the config: `gen-lang-client-0484678628`
- Named Firestore database configured in the app: `ai-studio-android-quizpoll-bedc2cc0-96aa-403c-a3e2-c286721cd11b`

The JSON contains several Android clients for different apps in the same Firebase project. The QuizPoll client is not the first entry, but the package-check script confirms it is present. Do not replace this file solely because the first client belongs to another app.

Before deploying or testing the new backend, verify in Firebase Console that:

1. The named Firestore database ID above exists in project `gen-lang-client-0484678628`.
2. Email/Password and Google providers are enabled in Firebase Authentication.
3. Cloud Functions are enabled and the selected project is correct (`firebase use --add`).
4. App Check has the QuizPoll app registered. Register the debug token for local testing and configure Play Integrity with the release signing SHA-256 before production.
5. The Firebase Functions region `asia-south1` is available for the project.

The source contains a package consistency check at `scripts/check-firebase-config.js`. Run it before building. It currently passes for the uploaded Firebase configuration.
