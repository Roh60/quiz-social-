# QuizPoll Community backend deployment checklist

## Implemented backend source

The `functions/` directory contains callable Cloud Functions for:
- `castPollVote`: validates poll state/options and updates vote + aggregate atomically.
- `submitQuizAttempt`: calculates the score from stored answer keys and updates attempts/leaderboards on the server.
- `processReferralCode`: validates self/duplicate claims and updates referral records/points on the server.
- `deleteUserAccount`: deletes user-owned content and the Firebase Auth identity using Admin SDK.

The Android app calls these functions for poll voting, quiz result submission, referral claims and account deletion. Critical Firestore client writes to votes, quiz attempts, and leaderboards are denied in `firestore.rules`.

## Important one-time data migration

New quiz creation and updates now store public question text/options separately from private `/answerKeys/{questionId}` documents. Correct answers are not returned by `getQuizQuestions`; the app asks the backend for answer review only after a completed attempt.

Existing quizzes may still have `correctAnswers` embedded in public question documents. Run `node functions/scripts/migrate-legacy-answer-keys.js` once with Firebase Admin Application Default Credentials after taking a Firestore backup. It copies each key into the private collection and removes the public field. Verify the migrated quizzes before production rollout.

## Deployment steps (requires your Firebase project access)

1. Install Node.js 20 and Firebase CLI.
2. From the project root run `cd functions && npm install`.
3. Confirm `firebase.json` points at the intended Firebase project and named Firestore database.
4. Run `firebase emulators:start --only functions,firestore` and test the callable endpoints.
5. Register the Android app in Firebase App Check. Register the debug token printed by the debug provider for local testing.
6. Configure Play Integrity for the release app in Firebase App Check.
7. Deploy with `firebase deploy --only functions,firestore:rules`.
8. Test all callable operations with real authenticated test accounts before releasing.

Do not enable App Check enforcement in production until the Android app has valid Play Integrity configuration and debug/release tests pass.

Additional callable functions added:
- `toggleLike`: toggles a signed-in user's like and updates the post count atomically.
- `followUser` / `unfollowUser`: create or remove both sides of a follow relationship and update counters on the server.
- `getQuizAnswerReview`: returns correct answers only after the caller has a completed attempt.

## Required release checks

- The app currently has no real moderation/admin dashboard. Reports can be submitted, but a separate role-protected moderation interface and review workflow must be built before opening the community broadly.
- Post comment counts and some user-owned activity counters are still updated by client operations; they need further abuse/rate-limit hardening.
- Weekly leaderboard scores are server-calculated, but elapsed quiz time is still supplied by the client and should not be treated as cheat-proof.
- Cloud Functions deployment may require the Firebase project to be on a billing plan that supports Functions. Check current Firebase plan requirements before deploying.
