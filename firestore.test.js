const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const firebase = require("@firebase/rules-unit-testing");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const EVE_UID = "eve_uid_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("1. Unauthenticated user cannot read or list messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("messages").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("2. PII Isolation: Alice cannot read Bob's private userSettings", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("userSettings").doc(BOB_UID).set({
      userId: BOB_UID,
      phoneNumber: "+628123456789",
      email: "bob@example.com",
      privacyPhoto: "EVERYONE",
      privacyBio: "EVERYONE",
      privacyLastSeen: "EVERYONE",
      privacyOnline: "EVERYONE",
      privacyStatus: "CONTACTS",
      readReceipts: true,
      themeMode: "SYSTEM",
      wallpaperHex: "#ECE5DD",
      fontScale: "MEDIUM",
      notificationsEnabled: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("userSettings").doc(BOB_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("userSettings").doc(BOB_UID).get());
});

test("3. Conversation & Message Membership: Eve cannot read Alice-Bob messages", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("messages").doc("msg_1").set({
      conversationId: "conv_ab",
      senderId: ALICE_UID,
      senderName: "Alice",
      memberUids: [ALICE_UID, BOB_UID],
      text: "Halo Bob!",
      messageType: "TEXT",
      mediaData: "",
      mediaCaption: "",
      replyToMessageId: "",
      replyToSenderName: "",
      replyToText: "",
      isForwarded: false,
      isEdited: false,
      isDeleted: false,
      status: "SENT",
      reactionsSummary: "",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  await assertFails(eveDb.collection("messages").doc("msg_1").get());
  await assertFails(
    eveDb.collection("messages").where("memberUids", "array-contains", ALICE_UID).get()
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("messages").doc("msg_1").get());
  await assertSucceeds(
    bobDb.collection("messages").where("memberUids", "array-contains", BOB_UID).get()
  );
});

test("4. Shadow Update & Identity Spoofing rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Spoofing Bob's userId
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: BOB_UID,
      username: "alice_w",
      displayName: "Alice",
      bio: "Ada",
      avatarData: "",
      isOnline: true,
      lastSeenText: "Online",
      isDiscoverable: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Adding undeclared ghost field isAdmin
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_w",
      displayName: "Alice",
      bio: "Ada",
      avatarData: "",
      isOnline: true,
      lastSeenText: "Online",
      isDiscoverable: true,
      isAdmin: true,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
