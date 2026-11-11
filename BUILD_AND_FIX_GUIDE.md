# QuizPoll: build and fix guide

## What is set up
- Firebase project quizpoll-e600e, package com.aistudio.quizpoll.qpxmrk, Firestore (default).
  Rules come from firestore.rules (publish by copy and paste in Firebase, Firestore, Rules).
- Signing: GitHub secret DEBUG_KEYSTORE_BASE64. Its SHA-1 E8:75:D0:DA:BB:9C:7E:98:CD:87:0E:F8:15:94:C5:E0:1E:10:C5:BB
  is registered in Firebase (SHA-256 too).
- AdMob app ca-app-pub-5674862555631304~5080207259. Debug builds show test ads, release builds show real ads.
- Coins: check-in, posts, daily quiz challenge, correct answers. 1000 coins = Rs 5. Min withdrawal Rs 10.
  Payouts are manual, see PAYOUT_AND_ADMIN_GUIDE.md.

## Make a new APK
1. Push to main, or GitHub, Actions, Build Debug APK, Re-run all jobs.
2. When green, open the run page. Annotations must say "Signing SHA-1 OK".
3. Artifacts, app-debug, download, unzip, install. Same key means it installs over the old app.

## If something fails
- Build red: open the red step, copy the last 25 lines.
- Annotation says SHA-1 does NOT match: secret DEBUG_KEYSTORE_BASE64 is missing or wrong. Re-add it.
- Google sign-in error: the SHA-1 above must be in Firebase, Google must be Enabled, and the APK must be
  built after that. Uninstall the old app first.
- Post, wallet or profile errors: publish the latest firestore.rules.
- Admin button missing: email must be verified (use Forgot Password, or Google sign-in).

## Still to do
- App open ad unit, InMobi account and placements, then put the IDs in AdConstants.kt.
- app-ads.txt on a website, and Play Store release (release keystore and signed_aab workflow secrets).
- Real protection for coins: move the wallet to Cloud Functions (needs the Firebase Blaze plan).
