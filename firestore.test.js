const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSettingsPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    displayName: "Alice",
    email: "alice@example.com",
    themeMode: "dark",
    accentPalette: "chrome_blue",
    fontScale: 1.0,
    reduceMotion: false,
    highContrast: false,
    largeTargets: false,
    liteMode: true,
    pinnedApps: ["docs", "notes", "files"],
    createdAt: now,
    updatedAt: now,
  };
}

function validDocPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    title: "Chromebook Sync Spec",
    content: "Auto-save offline notes synced cleanly across devices.",
    category: "Notes",
    isPinned: true,
    wordCount: 7,
    lastEditedDevice: "Dell Chromebook 11",
    createdAt: now,
    updatedAt: now,
  };
}

function validDevicePayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    deviceName: "Dell Chromebook 11",
    manufacturer: "Dell",
    formFactor: "chromebook",
    osVersion: "Android 14",
    isLowRamDevice: true,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write users, documents, or devices", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("documents").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("devices").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("doc1")
      .set(validDocPayload(ALICE_UID))
  );
});

test("Owner can create, read, and update their user settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validSettingsPayload(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(
    ref.update({
      themeMode: "amoled",
      liteMode: true,
      updatedAt: new Date(),
    })
  );
});

test("Shadow update with ghost field on user settings fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validSettingsPayload(ALICE_UID)));
  await assertFails(
    ref.update({
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user access to settings, documents, and devices is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(BOB_UID).set(validSettingsPayload(BOB_UID));
    await adminDb
      .collection("users")
      .doc(BOB_UID)
      .collection("documents")
      .doc("bob_doc")
      .set(validDocPayload(BOB_UID));
    await adminDb
      .collection("users")
      .doc(BOB_UID)
      .collection("devices")
      .doc("bob_dev")
      .set(validDevicePayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("documents").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("documents")
      .doc("bob_doc")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("devices")
      .doc("bob_dev")
      .get()
  );
});

test("Owner can CRUD workspace documents and connected devices", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("documents")
    .doc("doc_1");
  await assertSucceeds(docRef.set(validDocPayload(ALICE_UID)));
  await assertSucceeds(
    docRef.update({
      content: "Updated content with auto-save",
      wordCount: 5,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("documents").get()
  );
  await assertSucceeds(docRef.delete());

  const devRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("devices")
    .doc("chromebook_11");
  await assertSucceeds(devRef.set(validDevicePayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("devices").get()
  );
});

test("Invalid enum or spoofing userId fails validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badSettings = validSettingsPayload(BOB_UID);
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(badSettings));

  const badTheme = validSettingsPayload(ALICE_UID);
  badTheme.themeMode = "neon_hacker";
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(badTheme));

  const badDoc = validDocPayload(BOB_UID);
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("bad_doc")
      .set(badDoc)
  );
});
