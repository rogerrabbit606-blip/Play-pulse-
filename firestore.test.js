const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-playpuls-20454cee-56d2-4f17-8b29-ceda04f0c30d";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validOpportunity(userId, status = "scouted") {
  return {
    userId,
    name: "ScanLite PDF",
    packageName: "com.legacy.scanlite",
    store: "Google Play",
    category: "Productivity",
    downloads: 1200000,
    rating: 3.1,
    monthsDormant: 34,
    opportunityScore: 91,
    estMonthlyProfitUsd: 8400.0,
    recommendedSubPriceUsd: 6.99,
    status,
    redesignNotes: "Add Material 3 dark mode and instant cloud sync.",
    marketingHook: "Zero-ad private PDF scanner",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validSubPlan(userId) {
  return {
    userId,
    name: "Pro Monthly",
    platform: "Google Play",
    billingCycle: "monthly",
    priceUsd: 6.99,
    trialDays: 7,
    conversionRatePct: 4.8,
    monthlyChurnPct: 3.5,
    activeSubscribers: 420,
    isActive: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
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

test("Unauthenticated user cannot list or write trackedApps", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("trackedApps").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("trackedApps").doc("opp_1").set(validOpportunity(ALICE_UID))
  );
});

test("Authenticated user can create, list, and update own trackedApps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("trackedApps").doc("opp_1");
  await assertSucceeds(docRef.set(validOpportunity(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("trackedApps").get());
  await assertSucceeds(
    docRef.update({
      status: "redesigning",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Cross-user access to trackedApps and subscriptionPlans is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("trackedApps").doc("opp_1").set(validOpportunity(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subscriptionPlans").doc("plan_1").set(validSubPlan(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("trackedApps").doc("opp_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("trackedApps").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("subscriptionPlans").doc("plan_2").set(validSubPlan(BOB_UID))
  );
});

test("Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("trackedApps").doc("opp_shadow");
  const badPayload = { ...validOpportunity(ALICE_UID), isAdmin: true };
  await assertFails(docRef.set(badPayload));
});

test("Terminal archived state blocks further updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("trackedApps").doc("opp_arch");
  await assertSucceeds(docRef.set(validOpportunity(ALICE_UID, "archived")));
  await assertFails(
    docRef.update({
      status: "launched",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Invalid ID and out-of-range opportunityScore are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("trackedApps").doc("bad id!").set(validOpportunity(ALICE_UID))
  );
  const badScore = { ...validOpportunity(ALICE_UID), opportunityScore: 150 };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("trackedApps").doc("opp_bad_score").set(badScore)
  );
});

test("AlertRule, PaymentGateway, SavedReport, and AiStrategyBrief enforce valid schemas", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("alertRules").doc("rule_1").set({
      userId: ALICE_UID,
      title: "High Dormancy Gem",
      metricKey: "dormancy_months",
      operator: "gt",
      threshold: 24,
      isEnabled: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("paymentGateways").doc("gw_1").set({
      userId: ALICE_UID,
      providerName: "Google Play Billing v7",
      skuId: "playpulse_pro_monthly",
      mode: "live",
      webhookConfigured: true,
      gracePeriodEnabled: true,
      isConnected: true,
      payoutReady: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("savedReports").doc("rep_1").set({
      userId: ALICE_UID,
      title: "Top Google Play Opportunities",
      datasetType: "opportunities",
      storeFilter: "Google Play",
      minOpportunityScore: 75,
      selectedColumns: ["Name", "Store", "Downloads", "OpportunityScore"],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("strategyBriefs").doc("brief_1").set({
      userId: ALICE_UID,
      targetName: "ScanLite PDF",
      platform: "Google Play",
      competitiveMoatScore: 86,
      monetizationStrategy: "Anchor annual $39.99 plan with 7-day trial.",
      redesignPriorities: "Material 3 dark theme and batch OCR.",
      marketingPlaybook: "Target long-tail ASO keywords for offline PDF scan.",
      retentionHooks: "Weekly document digest and cloud backup streaks.",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});
