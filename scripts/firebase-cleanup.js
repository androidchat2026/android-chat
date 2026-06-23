/**
 * Firebase daily cleanup script.
 *
 * Triggered by the GitHub Actions cron job (firebase-cleanup.yml).
 *
 * What it cleans:
 *   1. Consumed message docs (consumed=true, consumedAt older than 24h)
 *   2. Stale undelivered message docs (consumed=false, timestamp older than 48h)
 *   3. Firebase Storage voice_notes/ blobs older than 24h (safety net)
 *
 * Set these GitHub secrets before enabling:
 *   FIREBASE_PROJECT_ID          — your Firebase project ID
 *   FIREBASE_SERVICE_ACCOUNT_KEY — JSON content of a service account key with
 *                                  roles/datastore.user and roles/storage.objectAdmin
 */

/**
 * NOTE: Firebase Storage is NOT used (Spark free plan).
 * Voice notes are base64-encoded in Firestore documents and cleared (set to null)
 * immediately after the recipient decodes them. The Storage cleanup section below
 * is a no-op but kept in case the project is later upgraded to Blaze.
 */
const admin = require('firebase-admin');

const projectId = process.env.FIREBASE_PROJECT_ID;
const serviceAccountKey = process.env.FIREBASE_SERVICE_ACCOUNT_KEY;

if (!projectId || !serviceAccountKey) {
  console.error('Missing required environment variables: FIREBASE_PROJECT_ID, FIREBASE_SERVICE_ACCOUNT_KEY');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(JSON.parse(serviceAccountKey)),
  storageBucket: `${projectId}.appspot.com`,
});

const db = admin.firestore();
const bucket = admin.storage().bucket();

const NOW = Date.now();
const TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000;
const FORTY_EIGHT_HOURS = 48 * 60 * 60 * 1000;

async function deleteInBatches(query) {
  let deleted = 0;
  let snapshot = await query.get();

  while (!snapshot.empty) {
    const batch = db.batch();
    snapshot.docs.forEach(doc => batch.delete(doc.ref));
    await batch.commit();
    deleted += snapshot.docs.length;
    snapshot = await query.get();
  }
  return deleted;
}

async function cleanupFirestoreMessages() {
  console.log('--- Cleaning Firestore messages ---');

  // 1. Delete consumed messages older than 24 hours
  const consumedCutoff = NOW - TWENTY_FOUR_HOURS;
  const consumedQuery = db.collection('messages')
    .where('consumed', '==', true)
    .where('consumedAt', '<', consumedCutoff)
    .limit(500);
  const consumedDeleted = await deleteInBatches(consumedQuery);
  console.log(`Deleted ${consumedDeleted} consumed message docs`);

  // 2. Delete stale undelivered messages older than 48 hours
  const staleCutoff = NOW - FORTY_EIGHT_HOURS;
  const staleQuery = db.collection('messages')
    .where('consumed', '==', false)
    .where('timestamp', '<', staleCutoff)
    .limit(500);
  const staleDeleted = await deleteInBatches(staleQuery);
  console.log(`Deleted ${staleDeleted} stale undelivered message docs`);

  return consumedDeleted + staleDeleted;
}

async function cleanupStorageVoiceNotes() {
  console.log('--- Cleaning Storage voice_notes/ ---');

  const cutoffDate = new Date(NOW - TWENTY_FOUR_HOURS);
  const [files] = await bucket.getFiles({ prefix: 'voice_notes/' });

  let deleted = 0;
  const deletePromises = files
    .filter(file => {
      const updated = new Date(file.metadata.updated);
      return updated < cutoffDate;
    })
    .map(async file => {
      try {
        await file.delete();
        deleted++;
        console.log(`  Deleted storage blob: ${file.name}`);
      } catch (err) {
        console.warn(`  Failed to delete ${file.name}: ${err.message}`);
      }
    });

  await Promise.all(deletePromises);
  console.log(`Deleted ${deleted} storage voice note blobs`);
  return deleted;
}

async function main() {
  console.log(`Firebase cleanup started at ${new Date().toISOString()}`);
  console.log(`Project: ${projectId}`);

  try {
    const firestoreDeleted = await cleanupFirestoreMessages();
    const storageDeleted = await cleanupStorageVoiceNotes();

    console.log('\n=== Cleanup summary ===');
    console.log(`  Firestore docs deleted: ${firestoreDeleted}`);
    console.log(`  Storage blobs deleted:  ${storageDeleted}`);
    console.log(`Finished at ${new Date().toISOString()}`);
  } catch (err) {
    console.error('Cleanup failed:', err);
    process.exit(1);
  }
}

main();
