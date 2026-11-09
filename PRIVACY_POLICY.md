# Privacy Policy for QuizPoll Community

**Effective Date:** November 8, 2026  
**Last Updated:** November 8, 2026  
**Developer Contact:** rohitpandit55970@gmail.com  
**Public Policy URL:** https://ais-pre-d4k3ad2afjp7ckt63tkur7-693313351988.asia-east1.run.app/privacy-policy  
**Account Deletion Request URL:** https://ais-pre-d4k3ad2afjp7ckt63tkur7-693313351988.asia-east1.run.app/delete-account  

---

## 1. Introduction
QuizPoll Community ("we", "our", or "the App") is an interactive educational and community platform for participating in quizzes, answering polls, and sharing educational knowledge. We are committed to transparency, user privacy, and complying with Google Play Developer Program policies, GDPR, and CCPA/CPRA.

---

## 2. Information We Collect

### A. Personal Information & Authentication
* **Account Credentials:** When you sign in with Google (via Jetpack Credential Manager) or register using Email and Password, we collect your authentication UID, email address, display name, and avatar URL provided by your identity provider.
* **Profile Metadata:** Custom username, bio text, joined timestamp, and account status (active, verified, or banned).

### B. User-Generated Educational Content
* **Quizzes & Questions:** Quiz titles, categories, questions, options, explanations, and timers.
* **Polls & Votes:** Poll questions, vote options, and your anonymized vote selection.
* **Discussion Posts & Comments:** Feed questions, comments, discussion replies, and reaction counts.

### C. App Interaction & Performance Data
* **Quiz History & Attempts:** Scores, total questions answered, time spent, accuracy percentages, and daily streak counts.
* **Leaderboards & Badges:** Gamification points earned through quiz performance, badges unlocked, and weekly rankings.

### D. STRICT Storage Limitation (Zero Cloud Media Storage)
* **No Image / Video Storage:** QuizPoll Community strictly prohibits user media/video/audio cloud uploads. All user content consists solely of structured text, emojis, and educational metadata stored in Google Cloud Firestore and local Room database cache.

---

## 3. Advertising & Third-Party SDK Disclosures

To support our free educational platform, QuizPoll Community displays non-intrusive advertisements managed through **Google AdMob Mediation**. 

### Ad Networks & Partners
Our ad mediation pipeline integrates with leading ad networks:
1. **Google AdMob** (Primary Ad Server & Mediation Host)
2. **AppLovin** (Demand Partner)
3. **Unity Ads** (Demand Partner)
4. **InMobi** (Demand Partner)
5. **Meta Audience Network** (Demand Partner)
6. **Mintegral** (Demand Partner)
7. **Pangle** (Demand Partner)

### Data Handled for Advertising
* **Advertising Identifiers:** Android Advertising ID (AAID) for personalized or non-personalized ad delivery.
* **Device Information:** Device model, OS version, coarse location (country/region), and network connection status.
* **Consent Management:** We use Google User Messaging Platform (UMP) to obtain and manage GDPR/CCPA user consent. Users in the EEA/UK or California can review or revoke consent choices at any time within App Settings.

---

## 4. How We Use Your Information
* To authenticate you and securely maintain your session.
* To record quiz scores, calculate authoritative leaderboards, and update streak counts.
* To deliver interactive community polls and aggregate vote percentages.
* To moderate spam, abusive comments, and enforce Community Guidelines.
* To serve non-disruptive, policy-compliant banner, native feed, and opt-in rewarded ads.

---

## 5. Data Retention & Permanent Account Deletion

### Your Right to Erasure
You have full ownership of your data. You may request permanent deletion of your account and associated personal data at any time through either of the following methods:

1. **In-App Immediate Deletion:**
   * Open the app, navigate to **Settings & Privacy**.
   * Scroll to the **Account** section.
   * Tap **Delete Account & Purge Data** and confirm.
   * This immediately triggers `deleteUserAccountWorkflow()`, cascading the deletion of:
     - Your user profile document in Cloud Firestore
     - All user-created quizzes, questions, and leaderboards
     - All user-created polls and voting records
     - All user feed posts, comments, and replies
     - All quiz attempts, bookmarks, and notification records
     - Local device Room SQLite cache
     - The underlying Firebase Authentication account

2. **Public HTTPS Web Request:**
   * Visit: `https://ais-pre-d4k3ad2afjp7ckt63tkur7-693313351988.asia-east1.run.app/delete-account`
   * Or email your deletion request to `rohitpandit55970@gmail.com` with your registered email and username. Requests received via email are audited and purged within 7 business days.

---

## 6. Security & Subprocessors
We store your data securely in **Google Cloud Firestore** (hosted in Google Cloud Platform data centers) backed by zero-trust security rules and App Check. Data in transit is encrypted using industry-standard TLS 1.3, and data at rest is encrypted with AES-256.

---

## 7. Children's Privacy
QuizPoll Community is designed for general audiences aged 13 and older. We do not knowingly collect personal information from children under 13. If you believe a child has provided us with personal information, please contact us immediately.

---

## 8. Updates to this Policy
We may update this Privacy Policy periodically. Significant changes will be announced in the app feed or through an in-app notice.

---

## 9. Contact Us
For questions regarding this policy or your personal data:
* **Support Email:** rohitpandit55970@gmail.com
* **Application:** QuizPoll Community (com.aistudio.quizpoll.kxmpzq)
