# QuizPoll Community – AdMob + InMobi setup

## Implemented in source

- Google Mobile Ads Android SDK integration.
- InMobi Google Mobile Ads mediation adapter dependency.
- UMP consent refresh on each app launch.
- Real Banner, Interstitial and Rewarded ad loading using Google's official demo ad unit IDs.
- InMobi adapter presence detection.
- Fake sponsored feed cards disabled (native ads are not yet rendered by a real NativeAdView).
- Test mode remains enabled. Do not switch to production until account mappings and test-device validation are complete.

## Current SDK versions

- Google Mobile Ads SDK: 25.5.0
- InMobi mediation adapter: 11.5.0.0
- Google UMP: 4.0.0

These versions are based on Google's current Android InMobi mediation and UMP setup documentation. Re-check the official compatibility page before future upgrades.

## Required dashboard setup (cannot be completed from source code)

1. Create/verify an AdMob publisher account and add the Android app using application ID `com.aistudio.quizpoll.qpxmrk`.
2. Create AdMob banner, interstitial and rewarded ad units. Keep the test IDs in source until the app is validated.
3. Create an InMobi publisher account and add the same Android app. Create placements for the formats you plan to mediate.
4. In AdMob > Mediation, create mediation groups for each ad format and add InMobi as a bidding or waterfall source. Complete the InMobi partnership agreement if bidding access is required.
5. Add the InMobi account ID and placement ID mappings in AdMob. Do not put private API keys in the APK.
6. Configure GDPR / US state privacy partner lists and UMP messages in AdMob. Verify the consent signal is passed to InMobi.
7. Enable InMobi test mode for the test placements and use AdMob test device / single ad source testing. Mediated ads may not show Google's Test Ad label.
8. Publish a valid `app-ads.txt` on the developer website and add its domain to the store listing when required.
9. Only after approval, replace test ad IDs with your own IDs via a controlled release configuration. Do not commit private account credentials.

## Not yet implemented

- Real NativeAdView rendering inside the feed. Fake sample sponsored cards have been disabled so they cannot be mistaken for paid ads.
- Server-side verification and persistent reward ledger for rewarded ads. Current callback is real SDK completion but no points/cash are granted.
- Live production IDs, AdMob mediation group mappings, InMobi placement mappings, publisher approval and actual fill-rate testing.

## Official references

- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/mediation/inmobi
- https://developers.google.com/admob/android/privacy
- https://developers.google.com/admob/android/test-ads

## Build-time ad IDs

The app defaults to test mode. For a controlled production build, supply these Gradle project properties (for example from private `~/.gradle/gradle.properties` or GitHub Actions repository variables):

- `ADS_TEST_MODE=false`
- `ADMOB_APP_ID=<your AdMob app ID>`
- `ADMOB_BANNER_AD_UNIT_ID=<your banner unit ID>`
- `ADMOB_INTERSTITIAL_AD_UNIT_ID=<your interstitial unit ID>`
- `ADMOB_REWARDED_AD_UNIT_ID=<your rewarded unit ID>`
- `ADMOB_NATIVE_AD_UNIT_ID=<your native unit ID>` (reserved; native rendering is not enabled yet)

If an ad-unit property is blank, the app falls back to Google's test unit even when test mode is disabled. Do not publish a release until all IDs are valid and tested. The InMobi publisher/placement mapping is configured in the AdMob mediation dashboard, not in these Android properties.
