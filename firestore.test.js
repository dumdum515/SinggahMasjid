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

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    unauthDb.collection("custom_mosques").where("visibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("mosque_reviews").where("visibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
  await assertFails(
    unauthDb.collection("mosque_reviews").doc("rev_unauth").set({
      userId: ALICE_UID,
      authorName: "Anon",
      mosqueId: "osm_node_123",
      mosqueName: "Masjid Al-Ikhlas",
      rating: 5,
      facilities: ["wifi"],
      comment: "Mantap",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("custom_mosques: owner create, partial update, query alignment, and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Valid create matching Kotlin payload (no `id` field)
  await assertSucceeds(
    aliceDb.collection("custom_mosques").doc("custom_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      name: "Masjid Al-Muhajirin",
      address: "Jl. Sudirman No. 12",
      lat: -6.2088,
      lon: 106.8456,
      baseFacilities: ["wifi", "minuman", "shower"],
      visibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Filtered query succeeds; unfiltered list query fails
  await assertSucceeds(
    bobDb.collection("custom_mosques").where("visibility", "==", "public").get()
  );
  await assertFails(bobDb.collection("custom_mosques").get());

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("custom_mosques").doc("custom_1").update({
      name: "Masjid Raya Al-Muhajirin",
      address: "Jl. Sudirman No. 12A",
      baseFacilities: ["wifi", "minuman", "shower", "menginap"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user update and delete fail
  await assertFails(
    bobDb.collection("custom_mosques").doc("custom_1").update({
      name: "Hijacked Masjid",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("custom_mosques").doc("custom_1").delete());

  // Ghost `id` field, immutable createdAt edit, and future timestamp fail
  await assertFails(
    aliceDb.collection("custom_mosques").doc("custom_2").set({
      id: "custom_2",
      userId: ALICE_UID,
      authorName: "Alice",
      name: "Masjid Ghost",
      address: "Jl. Thamrin",
      lat: -6.19,
      lon: 106.82,
      baseFacilities: ["wifi"],
      visibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("custom_mosques").doc("custom_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("custom_mosques").doc("custom_3").set({
      userId: ALICE_UID,
      authorName: "Alice",
      name: "Masjid Future",
      address: "Jl. Thamrin",
      lat: -6.19,
      lon: 106.82,
      baseFacilities: [],
      visibility: "public",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Owner delete succeeds
  await assertSucceeds(aliceDb.collection("custom_mosques").doc("custom_1").delete());
});

test("mosque_reviews: author create/update/delete, bounds validation, and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("mosque_reviews").doc("rev_1").set({
      userId: ALICE_UID,
      authorName: "Alice Musafir",
      mosqueId: "osm_node_98765",
      mosqueName: "Masjid Istiqlal",
      rating: 5,
      facilities: ["wifi", "minuman", "istirahat", "menginap", "shower"],
      comment: "Sangat nyaman untuk musafir beristirahat, shower bersih.",
      visibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Filtered query matches Kotlin repository
  await assertSucceeds(
    bobDb.collection("mosque_reviews").where("visibility", "==", "public").get()
  );
  await assertFails(bobDb.collection("mosque_reviews").get());

  // Author partial update succeeds
  await assertSucceeds(
    aliceDb.collection("mosque_reviews").doc("rev_1").update({
      authorName: "Alice",
      rating: 4,
      facilities: ["wifi", "shower"],
      comment: "WiFi lancar dan ada shower bersih.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Spoofing userId on create fails
  await assertFails(
    bobDb.collection("mosque_reviews").doc("rev_spoof").set({
      userId: ALICE_UID,
      authorName: "Bob",
      mosqueId: "osm_node_98765",
      mosqueName: "Masjid Istiqlal",
      rating: 1,
      facilities: [],
      comment: "Spoof",
      visibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Invalid rating bounds fail
  await assertFails(
    bobDb.collection("mosque_reviews").doc("rev_bad_rating").set({
      userId: BOB_UID,
      authorName: "Bob",
      mosqueId: "osm_node_98765",
      mosqueName: "Masjid Istiqlal",
      rating: 6,
      facilities: [],
      comment: "Invalid rating",
      visibility: "public",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user update & delete fail
  await assertFails(
    bobDb.collection("mosque_reviews").doc("rev_1").update({
      comment: "Edited by Bob",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("mosque_reviews").doc("rev_1").delete());

  // Author delete succeeds
  await assertSucceeds(aliceDb.collection("mosque_reviews").doc("rev_1").delete());
});

test("users/{userId}/favorites: strict user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("osm_node_98765")
      .set({
        userId: ALICE_UID,
        mosqueId: "osm_node_98765",
        name: "Masjid Istiqlal",
        address: "Jl. Taman Wijaya Kusuma",
        lat: -6.1702,
        lon: 106.8314,
        createdAt: created,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );

  // Cross-user read/write/delete denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("osm_node_98765")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("osm_node_98765")
      .delete()
  );

  // Update denied per rules
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("osm_node_98765")
      .update({ name: "Changed" })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("osm_node_98765")
      .delete()
  );
});
