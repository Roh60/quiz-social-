# Privacy Policy – QuizPoll Community

**Effective date:** 8 November 2026  
**Application ID:** `com.aistudio.quizpoll.qpxmrk`  
**Developer contact:** rohitpandit55970@gmail.com  
**Public policy URL:** PENDING Firebase Hosting deployment  
**Account deletion URL:** PENDING Firebase Hosting deployment

## 1. About the app
QuizPoll Community is an educational community app for quizzes, polls and discussions. The app uses Firebase Authentication and Cloud Firestore for account and community metadata. It also uses a local Room database for device-side caching.

## 2. Information processed
Depending on features used, the app may process:
- Firebase account identifier (UID), email and display name from the sign-in provider.
- Profile details such as username, display name and bio.
- User-created quiz, poll, post, comment and reply text and related metadata.
- Poll selections, quiz attempts, scores, bookmarks, follows and notification metadata.
- Device/app diagnostics and push notification token where enabled.

Do not submit passwords, government IDs, payment credentials or other highly sensitive information in public posts.

## 3. Storage policy
QuizPoll Community does not provide user photo, video, audio or document uploads to cloud media storage. Community content is structured text and metadata stored in Cloud Firestore. Room/DataStore are used for local cache and settings. This does not mean that Firebase or advertising SDKs process no data.

## 4. Advertising partners
The selected advertising setup is limited to:
1. Google AdMob.
2. InMobi through Google AdMob Mediation.

The app integrates Google Mobile Ads SDK, the InMobi mediation adapter and Google User Messaging Platform (UMP). These providers may process advertising identifiers, IP/network information, device/app information, ad request and interaction data, and consent signals as described in their own privacy documentation. Actual ad delivery depends on publisher account approval, mediation configuration, consent and regional availability.

The app is currently configured with test ad unit IDs in source. Production ad serving is not enabled until the developer configures approved publisher IDs and mediation mappings.

## 5. Consent and privacy choices
UMP consent information is refreshed when the app launches. Ads are requested only when UMP reports that ad requests are allowed. Users can open the privacy options form when the configured message type requires it. The developer must configure consent messages and mediation partners in the AdMob dashboard before public release.

## 6. Sharing and disclosure
Public quizzes, polls, discussions, comments and profile fields may be visible to other signed-in users according to the app's community features. Advertising SDKs may share data with Google and InMobi for ad delivery, measurement and fraud prevention. The developer must complete Google Play Data Safety disclosures using the actual SDK behavior and enabled features.

## 7. Retention and deletion
Users can request account deletion from the app settings. The app calls a Firebase Cloud Function that deletes the Firebase Authentication identity and associated user-owned Firestore data. A public web deletion-request page must be deployed before Play Store release; its final HTTPS URL is not yet assigned.

Some records may be retained where required by law, security or abuse-prevention obligations. Do not state that all data is erased instantly until the deployed deletion workflow has been tested against the live Firebase project.

## 8. Children's privacy
The developer must configure the target audience, content rating, consent and ad treatment in Google Play and the ad network dashboards. Do not target children or serve personalized ads to children unless the required legal and platform requirements are met.

## 9. Security
The app uses Firebase Authentication, Firestore Security Rules and Firebase App Check integration. Security depends on the correct Firebase project configuration, deployed rules, enabled App Check provider and backend deployment. No independent security certification is claimed.

## 10. Contact
For privacy questions or deletion requests, contact: **rohitpandit55970@gmail.com**.

This policy must be reviewed and updated after Firebase, AdMob and InMobi dashboard configuration. It is a project draft, not legal advice.
