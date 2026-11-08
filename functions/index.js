const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");

const app = initializeApp();
const DATABASE_ID = "ai-studio-android-quizpoll-bedc2cc0-96aa-403c-a3e2-c286721cd11b";
const db = getFirestore(app, DATABASE_ID);
const authAdmin = getAuth(app);
const callableOptions = { region: "asia-south1", enforceAppCheck: true, maxInstances: 20 };

function requireUid(request) {
  if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in first.");
  return request.auth.uid;
}
function cleanString(value, max = 160) {
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

exports.toggleLike = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const postId = cleanString(request.data?.postId, 160);
  if (!postId) throw new HttpsError("invalid-argument", "Post ID is required.");
  const postRef = db.collection("posts").doc(postId);
  const likeRef = postRef.collection("likes").doc(uid);
  let liked = false;
  await db.runTransaction(async tx => {
    const [postSnap, likeSnap] = await Promise.all([tx.get(postRef), tx.get(likeRef)]);
    if (!postSnap.exists) throw new HttpsError("not-found", "Post not found.");
    if (likeSnap.exists) {
      tx.delete(likeRef);
      tx.update(postRef, { likeCount: Math.max(0, Number(postSnap.data().likeCount || 0) - 1), updatedAt: FieldValue.serverTimestamp() });
      liked = false;
    } else {
      tx.create(likeRef, { userId: uid, createdAt: FieldValue.serverTimestamp() });
      tx.update(postRef, { likeCount: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp() });
      liked = true;
    }
  });
  return { success: true, liked };
});

exports.castPollVote = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const pollId = cleanString(request.data?.pollId, 160);
  const selected = Array.isArray(request.data?.selectedOptionIndices)
    ? [...new Set(request.data.selectedOptionIndices.map(Number))] : [];
  if (!pollId || selected.length < 1 || selected.length > 6 || selected.some(n => !Number.isInteger(n) || n < 0)) {
    throw new HttpsError("invalid-argument", "Invalid poll or selected options.");
  }
  const pollRef = db.collection("polls").doc(pollId);
  const voteRef = pollRef.collection("votes").doc(uid);
  const profileVoteRef = db.collection("users").doc(uid).collection("votedPolls").doc(pollId);
  await db.runTransaction(async tx => {
    const pollSnap = await tx.get(pollRef);
    const voteSnap = await tx.get(voteRef);
    if (!pollSnap.exists) throw new HttpsError("not-found", "Poll not found.");
    const poll = pollSnap.data();
    if (poll.expiresAt && poll.expiresAt.toMillis() <= Date.now()) throw new HttpsError("failed-precondition", "This poll has expired.");
    const options = Array.isArray(poll.options) ? poll.options : [];
    if (!options.length || selected.some(i => i >= options.length)) throw new HttpsError("invalid-argument", "Selected option is invalid.");
    if (!poll.isMultipleChoice && selected.length !== 1) throw new HttpsError("invalid-argument", "Choose one option only.");
    if (voteSnap.exists && poll.allowVoteChange !== true) throw new HttpsError("already-exists", "Vote changes are disabled for this poll.");
    const counts = Array.isArray(poll.optionVotes) ? [...poll.optionVotes] : options.map(() => 0);
    while (counts.length < options.length) counts.push(0);
    const previous = voteSnap.exists && Array.isArray(voteSnap.data().selectedOptions) ? voteSnap.data().selectedOptions : [];
    previous.forEach(i => { if (Number.isInteger(i) && i >= 0 && i < counts.length) counts[i] = Math.max(0, Number(counts[i] || 0) - 1); });
    selected.forEach(i => { counts[i] = Number(counts[i] || 0) + 1; });
    const total = Number(poll.totalVotes || 0) + (voteSnap.exists ? 0 : 1);
    tx.update(pollRef, { optionVotes: counts, totalVotes: total, updatedAt: FieldValue.serverTimestamp() });
    tx.set(voteRef, { userId: uid, pollId, selectedOptions: selected, createdAt: FieldValue.serverTimestamp() });
    tx.set(profileVoteRef, { pollId, selectedOptions: selected, votedAt: FieldValue.serverTimestamp() });
  });
  return { success: true };
});

exports.submitQuizAttempt = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const quizId = cleanString(request.data?.quizId, 160);
  const submissionId = cleanString(request.data?.submissionId, 128);
  const timeTakenSeconds = Math.max(0, Math.min(3600, Number(request.data?.timeTakenSeconds || 0)));
  const answers = request.data?.answers && typeof request.data.answers === "object" ? request.data.answers : {};
  if (!quizId || !/^[A-Za-z0-9_-]{16,128}$/.test(submissionId)) throw new HttpsError("invalid-argument", "Quiz ID and valid submission ID are required.");
  const quizRef = db.collection("quizzes").doc(quizId);
  const quizSnap = await quizRef.get();
  if (!quizSnap.exists) throw new HttpsError("not-found", "Quiz not found.");
  const quiz = quizSnap.data();
  if (quiz.isPublic !== true && quiz.userId !== uid) throw new HttpsError("permission-denied", "This quiz is private.");
  const questionsSnap = await quizRef.collection("questions").orderBy("orderIndex").get();
  if (questionsSnap.empty || questionsSnap.size > 100) throw new HttpsError("failed-precondition", "Quiz has no valid questions.");
  let score = 0;
  for (let index = 0; index < questionsSnap.docs.length; index++) {
    const q = questionsSnap.docs[index].data();
    const submitted = Array.isArray(answers[String(index)]) ? [...new Set(answers[String(index)].map(Number))].sort((a,b)=>a-b) : [];
    let correct = Array.isArray(q.correctAnswers) ? q.correctAnswers.map(Number).sort((a,b)=>a-b) : [];
    // New-format private answer key is preferred; legacy embedded keys remain supported during migration.
    const keySnap = await quizRef.collection("answerKeys").doc(questionsSnap.docs[index].id).get();
    if (keySnap.exists) correct = Array.isArray(keySnap.data().correctAnswers) ? keySnap.data().correctAnswers.map(Number).sort((a,b)=>a-b) : [];
    if (correct.length && JSON.stringify(submitted) === JSON.stringify(correct)) score++;
  }
  const totalQuestions = questionsSnap.size;
  const accuracy = totalQuestions ? (score / totalQuestions) * 100 : 0;
  const profileSnap = await db.collection("users").doc(uid).get();
  const profile = profileSnap.data() || {};
  const now = Date.now();
  const attemptRef = db.collection("quizAttempts").doc(`${uid}_${submissionId}`);
  const quizLbRef = quizRef.collection("leaderboard").doc(uid);
  const compRef = db.collection("competitionLeaderboards").doc(uid);
  let wasExistingAttempt = false;
  await db.runTransaction(async tx => {
    const [attemptSnap, oldLbSnap, compSnap] = await Promise.all([tx.get(attemptRef), tx.get(quizLbRef), tx.get(compRef)]);
    if (attemptSnap.exists) { wasExistingAttempt = true; return; }
    const oldLb = oldLbSnap.data() || {};
    const comp = compSnap.data() || {};
    const better = !oldLbSnap.exists || score > Number(oldLb.score || -1) ||
      (score === Number(oldLb.score || -1) && accuracy > Number(oldLb.accuracy || -1)) ||
      (score === Number(oldLb.score || -1) && accuracy === Number(oldLb.accuracy || -1) && timeTakenSeconds < Number(oldLb.timeTakenSeconds || 999999));
    tx.create(attemptRef, { quizId, userId: uid, quizTitle: cleanString(quiz.title, 160), score, totalQuestions, accuracy, timeTakenSeconds, createdAt: FieldValue.serverTimestamp() });
    const lbUpdate = { quizId, userId: uid, displayName: cleanString(profile.displayName || request.auth.token.name || "Quiz Scholar", 80), username: cleanString(profile.username || "scholar", 40), attemptCount: Number(oldLb.attemptCount || 0) + 1, updatedAt: FieldValue.serverTimestamp() };
    if (better) Object.assign(lbUpdate, { score, totalQuestions, accuracy, timeTakenSeconds, completedAtEpochMs: now });
    tx.set(quizLbRef, lbUpdate, { merge: true });
    const oldCompleted = Number(comp.quizzesCompleted || 0);
    const newCompleted = oldCompleted + 1;
    const totalScore = Number(comp.totalScore || 0) + score;
    const oldAccuracy = Number(comp.accuracy || 0);
    const nowDate = new Date(now);
    const utcDay = nowDate.getUTCDay();
    const daysSinceMonday = (utcDay + 6) % 7;
    nowDate.setUTCDate(nowDate.getUTCDate() - daysSinceMonday);
    nowDate.setUTCHours(0, 0, 0, 0);
    const weekStartEpochMs = nowDate.getTime();
    const weeklyRecent = Number(comp.weekStartEpochMs || 0) === weekStartEpochMs;
    const weeklyCount = (weeklyRecent ? Number(comp.weeklyQuizzesCompleted || 0) : 0) + 1;
    const weeklyScore = (weeklyRecent ? Number(comp.weeklyScore || 0) : 0) + score;
    const weeklyAccuracy = (((weeklyRecent ? Number(comp.weeklyAccuracy || 0) * Number(comp.weeklyQuizzesCompleted || 0) : 0) + accuracy) / weeklyCount);
    tx.set(compRef, {
      userId: uid, displayName: lbUpdate.displayName, username: lbUpdate.username,
      totalScore, quizzesCompleted: newCompleted,
      accuracy: ((oldAccuracy * oldCompleted) + accuracy) / newCompleted,
      timeTakenSeconds: Number(comp.timeTakenSeconds || 0) + timeTakenSeconds,
      weeklyScore, weeklyQuizzesCompleted: weeklyCount, weeklyAccuracy,
      weeklyTimeTakenSeconds: (weeklyRecent ? Number(comp.weeklyTimeTakenSeconds || 0) : 0) + timeTakenSeconds,
      weekStartEpochMs, lastAttemptEpochMs: now, updatedAt: FieldValue.serverTimestamp()
    }, { merge: true });
    tx.update(quizRef, { attemptCount: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp() });
  });
  if (wasExistingAttempt) {
    const existing = (await attemptRef.get()).data();
    return { success: true, attemptId: attemptRef.id, score: existing.score, totalQuestions: existing.totalQuestions, accuracy: existing.accuracy, timeTakenSeconds: existing.timeTakenSeconds };
  }
  return { success: true, attemptId: attemptRef.id, score, totalQuestions, accuracy, timeTakenSeconds };
});


exports.getQuizAnswerReview = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const quizId = cleanString(request.data?.quizId, 160);
  const attemptId = cleanString(request.data?.attemptId, 160);
  if (!quizId || !attemptId) throw new HttpsError("invalid-argument", "Quiz and attempt IDs are required.");
  const attemptSnap = await db.collection("quizAttempts").doc(attemptId).get();
  if (!attemptSnap.exists || attemptSnap.data().userId !== uid || attemptSnap.data().quizId !== quizId) {
    throw new HttpsError("permission-denied", "Answer review is available only for your completed attempt.");
  }
  const quizRef = db.collection("quizzes").doc(quizId);
  const questions = await quizRef.collection("questions").orderBy("orderIndex").get();
  const answers = {};
  for (let i = 0; i < questions.docs.length; i++) {
    const qDoc = questions.docs[i];
    const keySnap = await quizRef.collection("answerKeys").doc(qDoc.id).get();
    const key = keySnap.exists ? keySnap.data().correctAnswers : qDoc.data().correctAnswers;
    answers[String(i)] = Array.isArray(key) ? key : [];
  }
  return { answers };
});

exports.followUser = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const targetUid = cleanString(request.data?.targetUserId, 160);
  if (!targetUid || targetUid === uid) throw new HttpsError("invalid-argument", "Invalid follow target.");
  const followerRef = db.collection("users").doc(uid);
  const targetRef = db.collection("users").doc(targetUid);
  const followingRef = db.collection("follows").doc(uid).collection("following").doc(targetUid);
  const followerListRef = db.collection("follows").doc(targetUid).collection("followers").doc(uid);
  await db.runTransaction(async tx => {
    const [followerSnap, targetSnap, existingSnap] = await Promise.all([tx.get(followerRef), tx.get(targetRef), tx.get(followingRef)]);
    if (!followerSnap.exists || !targetSnap.exists) throw new HttpsError("not-found", "User profile not found.");
    if (existingSnap.exists) return;
    const follower = followerSnap.data(); const target = targetSnap.data();
    tx.create(followingRef, { userId: targetUid, displayName: cleanString(target.displayName, 80), username: cleanString(target.username, 40), createdAt: FieldValue.serverTimestamp() });
    tx.create(followerListRef, { userId: uid, displayName: cleanString(follower.displayName, 80), username: cleanString(follower.username, 40), createdAt: FieldValue.serverTimestamp() });
    tx.update(followerRef, { followingCount: FieldValue.increment(1) });
    tx.update(targetRef, { followersCount: FieldValue.increment(1) });
  });
  return { success: true };
});

exports.unfollowUser = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const targetUid = cleanString(request.data?.targetUserId, 160);
  if (!targetUid || targetUid === uid) throw new HttpsError("invalid-argument", "Invalid follow target.");
  const followerRef = db.collection("users").doc(uid);
  const targetRef = db.collection("users").doc(targetUid);
  const followingRef = db.collection("follows").doc(uid).collection("following").doc(targetUid);
  const followerListRef = db.collection("follows").doc(targetUid).collection("followers").doc(uid);
  await db.runTransaction(async tx => {
    const [existingSnap, followerSnap, targetSnap] = await Promise.all([tx.get(followingRef), tx.get(followerRef), tx.get(targetRef)]);
    if (!existingSnap.exists) return;
    tx.delete(followingRef);
    tx.delete(followerListRef);
    if (followerSnap.exists) tx.update(followerRef, { followingCount: Math.max(0, Number(followerSnap.data().followingCount || 0) - 1) });
    if (targetSnap.exists) tx.update(targetRef, { followersCount: Math.max(0, Number(targetSnap.data().followersCount || 0) - 1) });
  });
  return { success: true };
});

exports.processReferralCode = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const code = cleanString(request.data?.referralCode, 48).toUpperCase();
  if (!code) throw new HttpsError("invalid-argument", "Referral code is required.");
  const refereeRef = db.collection("users").doc(uid);
  const matches = await db.collection("users").where("referralCode", "==", code).limit(1).get();
  if (matches.empty) throw new HttpsError("not-found", "Referral code not found.");
  const referrerDoc = matches.docs[0];
  if (referrerDoc.id === uid) throw new HttpsError("failed-precondition", "You cannot use your own referral code.");
  const referrerRef = referrerDoc.ref;
  const referralRef = db.collection("referrals").doc(uid);
  const refereeLbRef = db.collection("competitionLeaderboards").doc(uid);
  const referrerLbRef = db.collection("competitionLeaderboards").doc(referrerDoc.id);
  await db.runTransaction(async tx => {
    const refereeSnap = await tx.get(refereeRef);
    const [referrerSnap, oldReferralSnap, refereeLbSnap, referrerLbSnap] = await Promise.all([
      tx.get(referrerRef), tx.get(referralRef), tx.get(refereeLbRef), tx.get(referrerLbRef)
    ]);
    if (!refereeSnap.exists || !referrerSnap.exists) throw new HttpsError("not-found", "Account not found.");
    if (cleanString(refereeSnap.data().referredBy, 80) || oldReferralSnap.exists) throw new HttpsError("already-exists", "A referral has already been claimed.");
    tx.update(refereeRef, { referredBy: code, updatedAt: FieldValue.serverTimestamp() });
    tx.update(referrerRef, { referralCount: FieldValue.increment(1), referralPointsEarned: FieldValue.increment(100), updatedAt: FieldValue.serverTimestamp() });
    tx.set(refereeLbRef, { userId: uid, totalScore: FieldValue.increment(5), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
    tx.set(referrerLbRef, { userId: referrerDoc.id, totalScore: FieldValue.increment(10), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
    tx.create(referralRef, { referrerUserId: referrerDoc.id, referrerUsername: cleanString(referrerSnap.data().username, 40), refereeUserId: uid, refereeUsername: cleanString(refereeSnap.data().username, 40), referralCode: code, pointsAwardedToReferrer: 100, pointsAwardedToReferee: 50, createdAt: FieldValue.serverTimestamp() });
  });
  return { success: true, message: "Referral applied. Welcome bonus has been recorded." };
});

exports.deleteUserAccount = onCall(callableOptions, async (request) => {
  const uid = requireUid(request);
  const userRef = db.collection("users").doc(uid);
  const userSnap = await userRef.get();
  const username = userSnap.exists ? cleanString(userSnap.data().username, 40) : "";
  // Delete all user-owned top-level documents recursively, including their subcollections.
  const ownedCollections = ["posts", "quizzes", "polls", "quizAttempts"];
  for (const name of ownedCollections) {
    const owned = await db.collection(name).where("userId", "==", uid).get();
    for (const doc of owned.docs) await db.recursiveDelete(doc.ref);
  }
  for (const field of ["senderUserId", "recipientUserId"]) {
    const notifications = await db.collection("notifications").where(field, "==", uid).get().catch(() => ({ docs: [] }));
    for (const doc of notifications.docs) await doc.ref.delete().catch(() => {});
  }
  const referralsA = await db.collection("referrals").where("referrerUserId", "==", uid).get();
  const referralsB = await db.collection("referrals").where("refereeUserId", "==", uid).get();
  for (const doc of [...referralsA.docs, ...referralsB.docs]) await doc.ref.delete().catch(() => {});
  const quizLb = await db.collectionGroup("leaderboard").where("userId", "==", uid).get();
  for (const doc of quizLb.docs) await doc.ref.delete().catch(() => {});
  for (const groupName of ["comments", "items", "replies", "likes"]) {
    const refs = await db.collectionGroup(groupName).where("userId", "==", uid).get().catch(() => ({ docs: [] }));
    for (const doc of refs.docs) await db.recursiveDelete(doc.ref).catch(() => {});
  }
  for (const field of ["reporterUserId"]) {
    const reports = await db.collection("reports").where(field, "==", uid).get().catch(() => ({ docs: [] }));
    for (const doc of reports.docs) await doc.ref.delete().catch(() => {});
  }
  await db.recursiveDelete(db.collection("follows").doc(uid)).catch(() => {});
  await db.recursiveDelete(db.collection("blocks").doc(uid)).catch(() => {});
  const blockedBy = await db.collectionGroup("users").where("blockedUserId", "==", uid).get().catch(() => ({ docs: [] }));
  for (const doc of blockedBy.docs) await doc.ref.delete().catch(() => {});
  for (const groupName of ["following", "followers"]) {
    const refs = await db.collectionGroup(groupName).where("userId", "==", uid).get().catch(() => ({ docs: [] }));
    for (const doc of refs.docs) await doc.ref.delete().catch(() => {});
  }
  await db.collection("competitionLeaderboards").doc(uid).delete().catch(() => {});
  if (username) await db.collection("usernames").doc(username).delete().catch(() => {});
  await db.recursiveDelete(userRef);
  await authAdmin.deleteUser(uid);
  return { success: true };
});
