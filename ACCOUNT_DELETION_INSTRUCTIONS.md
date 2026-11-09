# Account Deletion & Data Purge Instructions

**Application:** QuizPoll Community  
**Package:** `com.aistudio.quizpoll.kxmpzq`  
**Web Endpoint:** `https://ais-pre-d4k3ad2afjp7ckt63tkur7-693313351988.asia-east1.run.app/delete-account`  
**Developer Contact:** rohitpandit55970@gmail.com  

---

## Google Play Data Safety Compliance
Under Google Play's Account Deletion policy, users have the right to request deletion of their account and all associated data both from within the mobile app and through a publicly accessible web resource without needing to reinstall the app.

---

## Method 1: Instant In-App Deletion (Recommended)

Users who have the application installed on their Android device can delete their account and purge all data instantly:

1. Open **QuizPoll Community**.
2. Tap the **Profile** icon in the bottom navigation bar.
3. Tap the **Settings** gear icon (or navigate directly to Settings & Privacy).
4. Scroll down to the **Account** section.
5. Tap the red button labeled **"Delete Account & Purge Data"**.
6. A confirmation modal will appear warning that this action is irreversible.
7. Tap **"Permanently Delete"**.

### What Happens Automatically:
* The app invokes `deleteUserAccountWorkflow()` in `QuizPollRepository.kt`.
* The following collections and records in Google Cloud Firestore are permanently deleted:
  - User profile document (`users/{uid}`)
  - Username reservation (`usernames/{username}`)
  - All quizzes created by the user (`quizzes/{quizId}`) along with their subcollections (questions, attempts, leaderboard)
  - All polls created by the user (`polls/{pollId}`) and their associated votes subcollection
  - All community discussion posts created by the user (`posts/{postId}`) and comment subcollections
  - All user quiz attempt history records (`quizAttempts`)
  - All user bookmarks (`users/{uid}/bookmarks`)
  - All voting tracking records (`users/{uid}/votedPolls`)
  - All user notifications (`users/{uid}/notifications`)
  - All competition entries (`competitionLeaderboards/{id}`)
* Local Room SQLite database caches are cleared on the client device.
* App DataStore preferences are wiped.
* The Firebase Authentication user account is deleted via `FirebaseAuth.getInstance().currentUser.delete()`.
* The user is signed out and returned to the initial onboarding screen.

---

## Method 2: Web Deletion Request (Without Reinstalling)

If you have already uninstalled the app or cannot access your Android device:

1. **Visit the public account deletion webpage:**  
   `https://ais-pre-d4k3ad2afjp7ckt63tkur7-693313351988.asia-east1.run.app/delete-account`
2. **Or send an email to:**  
   `rohitpandit55970@gmail.com`  
   - Subject: `Account Deletion Request - QuizPoll Community`  
   - Body: Specify your registered Google email address or username.
3. **Verification & Execution:**  
   - An automated verification confirmation email will be sent to the address on file to prevent fraudulent deletion.
   - Upon verification confirmation, all user data stored in Cloud Firestore and Firebase Authentication will be deleted within 7 business days.
   - An audit receipt confirming complete eradication of records will be emailed back to you.

---

## Data Retention Exemptions
No user data is retained following an account deletion. Financial transactions do not exist in the app as all features are free and ad-supported. All educational content, discussion comments, and personal identifiers are completely purged.
