const { initializeApp, applicationDefault } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const DATABASE_ID = "(default)";
initializeApp({ credential: applicationDefault() });
const db = getFirestore(undefined, DATABASE_ID);

(async () => {
  let migrated = 0;
  const quizzes = await db.collection("quizzes").get();
  for (const quiz of quizzes.docs) {
    const questions = await quiz.ref.collection("questions").get();
    for (const question of questions.docs) {
      const data = question.data();
      if (!Array.isArray(data.correctAnswers)) continue;
      await quiz.ref.collection("answerKeys").doc(question.id).set({ correctAnswers: data.correctAnswers });
      await question.ref.update({ correctAnswers: FieldValue.delete() });
      migrated++;
    }
  }
  process.stdout.write(`Migrated ${migrated} legacy question answer keys.\n`);
  process.exit(0);
})().catch(error => { console.error(error); process.exit(1); });
