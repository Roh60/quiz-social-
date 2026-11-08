# QuizPoll Community – Account Deletion

**Application ID:** `com.aistudio.quizpoll.qpxmrk`  
**Developer contact:** rohitpandit55970@gmail.com  
**Public deletion URL:** PENDING Firebase Hosting deployment

## In-app deletion
1. Sign in to QuizPoll Community.
2. Open Settings & Privacy.
3. Select Delete Account.
4. Review the warning and confirm.

The app calls the `deleteUserAccount` Firebase Cloud Function. The function requires an authenticated user and App Check, deletes user-owned Firestore records and the Firebase Authentication identity. The Android client clears local Room caches after the callable succeeds.

## Web deletion request
A public deletion page is included at `public/delete-account.html`. Deploy Firebase Hosting and publish the resulting HTTPS URL in Google Play Console and inside the app before production release. The page provides a request path through the developer support email; requests must be identity-verified before execution.

Do not submit passwords by email. A request may ask for the account email/username and a reply from the registered email address to verify ownership.

## Deployment dependency
The public page is not live yet. The final URL will be `https://<your-firebase-project-id>.web.app/delete-account.html` after Hosting deployment. Do not enter a guessed URL in Play Console.
