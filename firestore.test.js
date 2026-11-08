const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-quizpoll-bedc2cc0-96aa-403c-a3e2-c286721cd11b";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("Unauthenticated read of posts is rejected", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore({ databaseId: DATABASE_ID });
  await assertFails(unauthDb.collection("posts").get());
});

test("Authenticated user can create a post with their own userId", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  const postRef = userDb.collection("posts").doc("post1");
  await assertSucceeds(postRef.set({
    userId: "user123",
    authorName: "Alice",
    authorUsername: "alice",
    postType: "discussion",
    title: "Intro to Kotlin Coroutines",
    content: "Let's discuss how Flow works!",
    category: "Programming",
    likeCount: 0,
    commentCount: 0,
  }));
});

test("User cannot create a post impersonating another user", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  const postRef = userDb.collection("posts").doc("post2");
  await assertFails(postRef.set({
    userId: "user999",
    authorName: "Bob",
    authorUsername: "bob",
    postType: "discussion",
    title: "Fake post",
    content: "Impersonation",
    category: "General Knowledge",
  }));
});

test("Client cannot create likes directly; callable backend owns like counters", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  const likeRef = userDb.collection("posts").doc("post1").collection("likes").doc("user123");
  await assertFails(likeRef.set({
    userId: "user123",
  }));
});

test("User cannot like a post on behalf of another user", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  const likeRef = userDb.collection("posts").doc("post1").collection("likes").doc("user456");
  await assertFails(likeRef.set({
    userId: "user456",
  }));
});

test("User can create a quiz and questions", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  const quizRef = userDb.collection("quizzes").doc("quiz1");
  await assertSucceeds(quizRef.set({
    userId: "user123",
    authorName: "Alice",
    authorUsername: "alice",
    title: "Kotlin Basics Quiz",
    description: "Test your Kotlin fundamentals",
    category: "Programming",
    difficulty: "Easy",
    language: "English",
    questionCount: 5,
    timeLimitSeconds: 60,
    isPublic: true,
    attemptCount: 0,
    averageScore: 0,
  }));

  const qRef = quizRef.collection("questions").doc("q1");
  await assertSucceeds(qRef.set({
    quizId: "quiz1",
    questionText: "What is val in Kotlin?",
    questionType: "SINGLE_MCQ",
    options: ["Mutable variable", "Read-only variable", "Constant", "Function"],
    explanation: "val declares a read-only variable",
    orderIndex: 0,
  }));
  await assertSucceeds(quizRef.collection("answerKeys").doc("q1").set({ correctAnswers: [1] }));
  await assertFails(qRef.set({
    quizId: "quiz1",
    questionText: "This must not expose the answer",
    questionType: "SINGLE_MCQ",
    options: ["A", "B"],
    correctAnswers: [1],
    orderIndex: 1,
  }));
});

test("Client cannot write poll votes directly; callable backend owns vote transactions", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  const pollRef = userDb.collection("polls").doc("poll1");
  await assertSucceeds(pollRef.set({
    userId: "user123",
    authorName: "Alice",
    authorUsername: "alice",
    question: "Favorite language?",
    category: "Programming",
    options: ["Kotlin", "Java", "Python", "Rust"],
    optionVotes: [0, 0, 0, 0],
    totalVotes: 0,
    isMultipleChoice: false,
    allowVoteChange: false,
    isAnonymous: false,
    isPublic: true,
  }));

  const voteRef = pollRef.collection("votes").doc("user123");
  await assertFails(voteRef.set({
    userId: "user123",
    pollId: "poll1",
    selectedOptions: [0],
  }));
});

test("Reports are write-only by creator and cannot be read publicly", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  const reportRef = userDb.collection("reports").doc("rep1");
  await assertSucceeds(reportRef.set({
    reporterUserId: "user123",
    targetType: "POST",
    targetId: "post1",
    reason: "Spam",
    details: "Commercial spam",
  }));
  // Reading report must fail
  await assertFails(reportRef.get());
});

test("Client cannot forge individual quiz leaderboard scores", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  const entryRef = userDb.collection("quizzes").doc("quiz1").collection("leaderboard").doc("user123");
  await assertFails(entryRef.set({
    quizId: "quiz1",
    userId: "user123",
    displayName: "Alice",
    username: "alice",
    score: 8,
    totalQuestions: 10,
    accuracy: 80.0,
    timeTakenSeconds: 35,
    completedAtEpochMs: Date.now()
  }));

  // Another user reading this leaderboard entry must succeed (leaderboards are public to authenticated users)
  const otherUserDb = testEnv.authenticatedContext("user456").firestore({ databaseId: DATABASE_ID });
  await assertSucceeds(otherUserDb.collection("quizzes").doc("quiz1").collection("leaderboard").doc("user123").get());

  // Another user trying to overwrite user123's leaderboard entry must fail
  await assertFails(otherUserDb.collection("quizzes").doc("quiz1").collection("leaderboard").doc("user123").set({
    score: 1,
    userId: "user123"
  }));
});

test("Client cannot forge competition leaderboard summary document", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  const compRef = userDb.collection("competitionLeaderboards").doc("user123");
  await assertFails(compRef.set({
    userId: "user123",
    displayName: "Alice",
    username: "alice",
    totalScore: 120,
    quizzesCompleted: 15,
    accuracy: 85.5,
    timeTakenSeconds: 450,
    weeklyScore: 40,
    weeklyAccuracy: 90.0,
    weeklyTimeTakenSeconds: 120
  }));

  // Other user can read competition leaderboard
  const otherUserDb = testEnv.authenticatedContext("user456").firestore({ databaseId: DATABASE_ID });
  await assertSucceeds(otherUserDb.collection("competitionLeaderboards").doc("user123").get());

  // Other user cannot modify user123's competition record
  await assertFails(otherUserDb.collection("competitionLeaderboards").doc("user123").set({
    userId: "user123",
    totalScore: 0
  }));
});



test("Client cannot read private quiz answer keys created by another user", async () => {
  const ownerDb = testEnv.authenticatedContext("owner1").firestore({ databaseId: DATABASE_ID });
  const quizRef = ownerDb.collection("quizzes").doc("quiz-private");
  await assertSucceeds(quizRef.set({ userId: "owner1", isPublic: true, title: "Private key test" }));
  const answerRef = quizRef.collection("answerKeys").doc("q1");
  await assertSucceeds(answerRef.set({ correctAnswers: [1] }));
  const otherDb = testEnv.authenticatedContext("student2").firestore({ databaseId: DATABASE_ID });
  await assertFails(otherDb.collection("quizzes").doc("quiz-private").collection("answerKeys").doc("q1").get());
});

test("Client cannot directly create follow relationships", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  await assertFails(userDb.collection("follows").doc("user123").collection("following").doc("user456").set({ userId: "user456" }));
});


test("Client cannot submit forged quiz attempts", async () => {
  const userDb = testEnv.authenticatedContext("user123").firestore({ databaseId: DATABASE_ID });
  await assertFails(userDb.collection("quizAttempts").doc("forged-attempt").set({
    userId: "user123", quizId: "quiz1", score: 999, totalQuestions: 1
  }));
});
