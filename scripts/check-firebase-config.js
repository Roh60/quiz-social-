const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const gradlePath = path.join(root, 'app', 'build.gradle.kts');
const configPath = path.join(root, 'app', 'google-services.json');

if (!fs.existsSync(configPath)) {
  console.error('Firebase config missing: app/google-services.json. Download it for the QuizPoll Android app.');
  process.exit(1);
}
const gradle = fs.readFileSync(gradlePath, 'utf8');
const appId = gradle.match(/applicationId\s*=\s*"([^"]+)"/)?.[1];
if (!appId) {
  console.error('Could not read applicationId from app/build.gradle.kts.');
  process.exit(1);
}
const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
const packages = (config.client || []).map(client => client.client_info?.android_client_info?.package_name).filter(Boolean);
if (!packages.includes(appId)) {
  console.error(`Firebase package mismatch. Gradle applicationId is ${appId}; google-services.json contains: ${packages.join(', ') || '(none)'}.`);
  console.error('Register the QuizPoll package in the correct Firebase project and replace app/google-services.json.');
  process.exit(1);
}
console.log(`Firebase package configuration matches ${appId}.`);
