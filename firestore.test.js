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
const PUBLIC_KAS_ID = "KAS-8A49B2X7";
const ALICE_SECRET = "A7kP9mQ2xR4vN8wL5zT3cF6h"; // 24 chars
const WRONG_SECRET = "Z9xC8vB7nM6aS5dF4gH3jK2l"; // 24 chars

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

function buildValidPublicKasPayload(writeSecret = ALICE_SECRET, ownerUid = "") {
  return {
    writeSecret,
    ownerUid,
    groupName: "Kas RT 05 / RW 02",
    description: "Sekretariat Balai Warga RT 05",
    currencyCode: "IDR",
    initialBalance: 500000,
    totalBalance: 1250000,
    totalIncome: 1000000,
    totalExpense: 250000,
    updatedAt: new Date(Date.now() - 1000),
    transactions: [
      {
        id: "tx_1",
        title: "Iuran Warga Blok A",
        category: "Iuran Bulanan",
        type: "income",
        amount: 1000000,
        date: "2026-11-24",
        notes: "Lunas 20 KK",
      },
      {
        id: "tx_2",
        title: "Beli Lampu Jalan",
        category: "Operasional",
        type: "expense",
        amount: 250000,
        date: "2026-11-23",
        notes: "5 pcs lampu LED",
      },
    ],
  };
}

// --- EXISTING PRIVATE LEDGER TESTS ---

test("Unauthenticated user: cannot read private transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user: cannot read another user's private transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "INCOME",
      title: "Kas Masuk",
      amount: 100000,
      category: "Iuran",
      date: "2026-11-01",
      status: "ACTIVE",
      createdAt: new Date(Date.now() - 1000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get());
});

test("Authenticated user: can read own private transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "INCOME",
      title: "Kas Masuk",
      amount: 100000,
      category: "Iuran",
      date: "2026-11-01",
      status: "ACTIVE",
      createdAt: new Date(Date.now() - 1000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get());
});

test("Authenticated user: cannot create transaction with invalid negative amount", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_2").set({
    userId: ALICE_UID,
    type: "INCOME",
    title: "Negative",
    amount: -500,
    category: "Iuran",
    date: "2026-11-01",
    status: "ACTIVE",
    createdAt: new Date(Date.now() - 1000),
  }));
});

// --- PUBLIC_KAS (LOGIN-FREE + WRITE SECRET PROTECTION) TESTS ---

test("Treasurer without login: can create and update public_kas with matching writeSecret", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const payload = buildValidPublicKasPayload(ALICE_SECRET, "");

  await assertSucceeds(unauthDb.collection("public_kas").doc(PUBLIC_KAS_ID).set(payload));

  const updated = {
    ...payload,
    totalBalance: 1350000,
    totalIncome: 1100000,
    updatedAt: new Date(Date.now() - 500),
  };
  await assertSucceeds(unauthDb.collection("public_kas").doc(PUBLIC_KAS_ID).set(updated));
});

test("Unauthenticated citizen: can GET by exact ID, but CANNOT LIST or DELETE", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("public_kas").doc(PUBLIC_KAS_ID).set(buildValidPublicKasPayload(ALICE_SECRET));
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Citizens can read by exact link ID without login
  await assertSucceeds(unauthDb.collection("public_kas").doc(PUBLIC_KAS_ID).get());

  // Citizens cannot list/scrape all public_kas docs
  await assertFails(unauthDb.collection("public_kas").get());

  // Citizens cannot delete public_kas docs
  await assertFails(unauthDb.collection("public_kas").doc(PUBLIC_KAS_ID).delete());
});

test("Group isolation: another device with wrong or missing writeSecret CANNOT overwrite existing public_kas", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("public_kas").doc(PUBLIC_KAS_ID).set(buildValidPublicKasPayload(ALICE_SECRET));
  });

  const otherDeviceDb = testEnv.unauthenticatedContext().firestore();

  // Wrong writeSecret must fail
  await assertFails(
    otherDeviceDb.collection("public_kas").doc(PUBLIC_KAS_ID).set(buildValidPublicKasPayload(WRONG_SECRET))
  );

  // Missing writeSecret must fail
  const noSecretPayload = buildValidPublicKasPayload(ALICE_SECRET);
  delete noSecretPayload.writeSecret;
  await assertFails(
    otherDeviceDb.collection("public_kas").doc(PUBLIC_KAS_ID).set(noSecretPayload)
  );
});

test("Schema validation: rejects short writeSecret, groupName > 120 chars, ghost fields, invalid ID, future timestamp, negative totals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  // Short writeSecret (< 16 chars)
  await assertFails(unauthDb.collection("public_kas").doc(PUBLIC_KAS_ID).set(buildValidPublicKasPayload("short123")));

  // groupName > 120 chars
  await assertFails(unauthDb.collection("public_kas").doc(PUBLIC_KAS_ID).set({
    ...buildValidPublicKasPayload(ALICE_SECRET),
    groupName: "A".repeat(121),
  }));

  // Invalid ID format (missing KAS- prefix)
  await assertFails(unauthDb.collection("public_kas").doc("bad_id_123").set(buildValidPublicKasPayload(ALICE_SECRET)));

  // Shadow/ghost field injection
  await assertFails(unauthDb.collection("public_kas").doc(PUBLIC_KAS_ID).set({
    ...buildValidPublicKasPayload(ALICE_SECRET),
    isAdmin: true,
  }));

  // Tampered future timestamp
  await assertFails(unauthDb.collection("public_kas").doc(PUBLIC_KAS_ID).set({
    ...buildValidPublicKasPayload(ALICE_SECRET),
    updatedAt: new Date(Date.now() + 60000),
  }));

  // Negative totalIncome
  await assertFails(unauthDb.collection("public_kas").doc(PUBLIC_KAS_ID).set({
    ...buildValidPublicKasPayload(ALICE_SECRET),
    totalIncome: -100,
  }));
});
