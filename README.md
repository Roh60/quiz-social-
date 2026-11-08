<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/bedc2cc0-96aa-403c-a3e2-c286721cd11b

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.


## Advertising (AdMob + InMobi)

The Android source now uses Google Mobile Ads SDK + the InMobi mediation adapter and UMP consent flow. It remains in test mode. Complete the account-side mapping steps in `ADS_SETUP_ADMOB_INMOBI.md` before production. Native feed ads and persistent rewarded points are intentionally disabled until their real renderer and server verification are implemented.


## Firebase project verification

Read `FIREBASE_PROJECT_SETUP_REQUIRED.md` for the matching QuizPoll client and required Firebase Console checks. Run `node scripts/check-firebase-config.js` before building.


## Backend deployment

See `BACKEND_DEPLOYMENT.md` for Cloud Functions, Firestore Rules, App Check and the one-time legacy quiz answer-key migration. The backend functions are source code only until deployed to the correct Firebase project.
