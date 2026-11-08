# Google Play Console Store Listing & Data Safety Metadata

**App Name:** QuizPoll Community  
**Package Name:** `com.aistudio.quizpoll.qpxmrk`  
**Primary Category:** Education  
**Secondary Category:** Trivia / Social  
**Content Rating:** Complete the official Google Play content rating questionnaire before release  
**Developer Contact:** rohitpandit55970@gmail.com  
**Privacy Policy URL:** PENDING Firebase Hosting deployment  
**Account Deletion Webpage:** PENDING Firebase Hosting deployment  

---

## 1. Store Listing Copy

### App Title (20 / 30 chars)
`QuizPoll Community`

### Short Description (78 / 80 chars)
`Play interactive quizzes, vote in live polls, and learn with community members!`

### Full Description (3,280 / 4,000 chars)
Welcome to QuizPoll Community, an educational social platform for quizzes, interactive polls and discussions. Test your wits across diverse academic and popular categories, design custom multi-choice quizzes, create real-time opinion polls, and compete with learners around the globe.

KEY FEATURES:

🧠 RICH INTERACTIVE QUIZZES
- Explore community-created quizzes across available categories.
- Play with timed challenges, instant answer feedback, and detailed post-quiz answer explanations.
- Never lose your progress with our smart offline Room database caching.

📊 REAL-TIME COMMUNITY POLLS
- Cast your vote in community polls and watch dynamic percentage bars update in real time powered by Cloud Firestore transactions.
- Filter between single-choice, multiple-choice, and timed expirations.
- Complete privacy protection with anonymized community voting tallies.

🎨 INTUITIVE QUIZ & POLL CREATOR
- Build professional multiple-choice quizzes with up to 4 options, explanations, and custom timers.
- Publish instantly to the community feed or save locally as a draft.
- Full ownership: edit or unpublish your contributed content whenever you choose.

🏆 LEADERBOARDS & GAMIFICATION
- Climb global and weekly competition leaderboards with every correct answer.
- View quiz history and community leaderboard results.
- Earn in-app achievement badges where supported.

💬 DISCUSSION FEEDS & SOCIAL LEARNING
- Engage in thought-provoking discussions under every quiz and poll.
- Like, bookmark, and share questions with fellow students and study groups.
- Follow creators and receive activity notifications where enabled.

🛡️ SAFE, RESPECTFUL & STRICT STORAGE DISCIPLINE
- Community reporting and user blocking tools are available; a staffed moderation dashboard is still required before public launch.
- Strict zero-media policy: QuizPoll Community does not upload raw image or video files to cloud storage, keeping the app lightweight, fast, and battery-friendly.
- Dark and Light Material 3 design with full accessibility and dynamic theme support.

Join QuizPoll Community to share quizzes, participate in polls and learn through discussion.

---

## 2. Google Play Data Safety Declaration

### Data Collection & Purpose Summary

| Data Type | Collected | Shared with Third Parties | Purpose | Ephemeral / Stored | Can User Request Deletion? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Name & Email** | Yes | No | Account management, authentication | Stored in Cloud Firestore & Auth | Yes (In-app + Web) |
| **User IDs (UID)** | Yes | No | App functionality, user account identity | Stored in Firestore | Yes (In-app + Web) |
| **User-generated content (Quizzes, Polls, Posts, Comments)** | Yes | No (Displayed publicly in app feed) | Community educational feed & discussions | Stored in Firestore | Yes (In-app + Web) |
| **App Performance & Quiz Scores** | Yes | No | Leaderboards, streak tracking, game history | Stored in Firestore | Yes (In-app + Web) |
| **Device or other IDs (AAID)** | Yes | Yes (Google AdMob & Mediation Partners) | Advertising, analytics, fraud prevention | Handled by Google Play Services / AdMob | Yes (Opt-out via Android OS Settings) |

### Security Practices
* **Data Encrypted in Transit:** Firebase and SDK transport encryption applies; verify provider documentation and final configuration.
* **Data Encrypted at Rest:** Firebase/Google Cloud managed encryption; verify provider documentation.
* **Independent Security Review:** Not performed / not claimed.
* **Account Deletion URL Provided:** No – deploy Firebase Hosting page and verify the final HTTPS URL before production.
* **In-App Account Deletion:** Yes (Settings > Delete Account & Purge Data).

---

## 3. Advertising & Mediation Network Declarations
* App contains ads: **Yes**
* Ad types implemented in source: Banner, Interstitial and Rewarded. Native feed ads are disabled pending a real native-ad renderer.
* AdMob App ID: `ca-app-pub-3940256099942544~3347511713` (Pre-configured with official Google test units for validation).
* Mediation Partners: Google AdMob and InMobi only.
* Children's Online Privacy Protection Act (COPPA): App does not direct ads to children under 13.
