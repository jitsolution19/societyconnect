const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const MEMBER_UID = "member_uid_2";
const OUTSIDER_UID = "outsider_uid_3";
const VENDOR_UID = "vendor_uid_4";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastTs = () => new Date(Date.now() - 5000);
const futureTs = () => new Date(Date.now() + 60000);

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

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(MEMBER_UID).get());
  await assertFails(
    unauthDb.collection("societies").where("discoverable", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("participations").where("userId", "==", MEMBER_UID).get()
  );
  await assertFails(
    unauthDb.collection("vendors").where("activeCatalog", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("quotations").where("vendorUid", "==", VENDOR_UID).get()
  );
});

test("User profiles enforce owner-only PII isolation and reject ghost fields", async () => {
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastTs();

  await assertSucceeds(
    memberDb.collection("users").doc(MEMBER_UID).set({
      userId: MEMBER_UID,
      displayName: "Aarav Sharma",
      email: "aarav@example.com",
      activeRole: "MEMBER",
      activeSocietyId: "soc_1",
      activeSocietyName: "Greenwood Heights",
      flatNumber: "B-402",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Outsider cannot read PII
  await assertFails(outsiderDb.collection("users").doc(MEMBER_UID).get());
  // Shadow update with ghost field rejected
  await assertFails(
    memberDb.collection("users").doc(MEMBER_UID).update({
      isSuperAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Future timestamp rejected
  await assertFails(
    memberDb.collection("users").doc(OUTSIDER_UID).set({
      userId: OUTSIDER_UID,
      displayName: "Future User",
      activeRole: "MEMBER",
      createdAt: futureTs(),
      updatedAt: futureTs(),
    })
  );
});

test("Societies, membership isolation, and anti-privilege-escalation", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastTs();

  // Admin creates society + ADMIN membership
  await assertSucceeds(
    adminDb.collection("societies").doc("soc_1").set({
      adminUid: ADMIN_UID,
      adminName: "Priya Mehta",
      name: "Greenwood Heights",
      city: "Bengaluru",
      address: "Whitefield Main Rd",
      totalUnits: 120,
      inviteCode: "GRN402",
      memberCount: 1,
      discoverable: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb
      .collection("societies")
      .doc("soc_1")
      .collection("members")
      .doc(ADMIN_UID)
      .set({
        userId: ADMIN_UID,
        societyId: "soc_1",
        displayName: "Priya Mehta",
        flatNumber: "A-101",
        role: "ADMIN",
        status: "ACTIVE",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Member CANNOT self-assign ADMIN role on join
  await assertFails(
    memberDb
      .collection("societies")
      .doc("soc_1")
      .collection("members")
      .doc(MEMBER_UID)
      .set({
        userId: MEMBER_UID,
        societyId: "soc_1",
        displayName: "Aarav",
        flatNumber: "B-402",
        role: "ADMIN",
        status: "ACTIVE",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Member joins legitimately as MEMBER and increments memberCount
  await assertSucceeds(
    memberDb
      .collection("societies")
      .doc("soc_1")
      .collection("members")
      .doc(MEMBER_UID)
      .set({
        userId: MEMBER_UID,
        societyId: "soc_1",
        displayName: "Aarav",
        flatNumber: "B-402",
        role: "MEMBER",
        status: "ACTIVE",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertSucceeds(
    memberDb.collection("societies").doc("soc_1").update({
      memberCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Discoverable query succeeds, unfiltered query rejected
  await assertSucceeds(
    memberDb.collection("societies").where("discoverable", "==", true).get()
  );
  await assertFails(outsiderDb.collection("societies").get());

  // Outsider cannot list society members
  await assertFails(
    outsiderDb.collection("societies").doc("soc_1").collection("members").get()
  );
});

test("Offers, two-stage participation, threshold transition, and vendor isolation", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const vendorDb = testEnv.authenticatedContext(VENDOR_UID).firestore();
  const ts = pastTs();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("societies").doc("soc_1").set({
      adminUid: ADMIN_UID,
      adminName: "Priya Mehta",
      name: "Greenwood Heights",
      city: "Bengaluru",
      address: "Whitefield",
      totalUnits: 100,
      inviteCode: "GRN402",
      memberCount: 2,
      discoverable: true,
      createdAt: ts,
      updatedAt: ts,
    });
    await db
      .collection("societies")
      .doc("soc_1")
      .collection("members")
      .doc(MEMBER_UID)
      .set({
        userId: MEMBER_UID,
        societyId: "soc_1",
        displayName: "Aarav",
        flatNumber: "B-402",
        role: "MEMBER",
        status: "ACTIVE",
        createdAt: ts,
        updatedAt: ts,
      });
  });

  // Admin creates offer (no @DocumentId id field in document body)
  await assertSucceeds(
    adminDb
      .collection("societies")
      .doc("soc_1")
      .collection("offers")
      .doc("offer_1")
      .set({
        societyId: "soc_1",
        societyName: "Greenwood Heights",
        societyAdminUid: ADMIN_UID,
        title: "Organic A2 Cow Milk Bulk Pack",
        description: "Morning doorstep delivery for society residents.",
        category: "DAILY_ESSENTIALS",
        estimatedPrice: 75.0,
        unitLabel: "per 1L bottle",
        negotiatedPrice: 0.0,
        minThreshold: 1,
        thresholdType: "MEMBERS",
        maxLimit: 200,
        interestedCount: 0,
        totalRequestedQty: 0,
        confirmedCount: 0,
        confirmedQty: 0,
        status: "COLLECTING_INTEREST",
        deadlineLabel: "Sunday 8 PM",
        serviceDetails: "Delivered between 6 AM - 7:30 AM",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Outsider CANNOT read or list society offers
  await assertFails(
    outsiderDb.collection("societies").doc("soc_1").collection("offers").get()
  );
  await assertFails(
    vendorDb.collection("societies").doc("soc_1").collection("offers").get()
  );

  // Member CAN list society offers
  await assertSucceeds(
    memberDb.collection("societies").doc("soc_1").collection("offers").get()
  );

  // Member expresses Stage 1 Interest
  await assertSucceeds(
    memberDb.collection("participations").doc("soc_1_offer_1_" + MEMBER_UID).set({
      userId: MEMBER_UID,
      memberName: "Aarav",
      flatNumber: "B-402",
      societyId: "soc_1",
      societyAdminUid: ADMIN_UID,
      offerId: "offer_1",
      offerTitle: "Organic A2 Cow Milk Bulk Pack",
      stage: "INTERESTED",
      requestedQty: 2,
      confirmedQty: 0,
      preferredOption: "6:00 AM Slot",
      unitPrice: 75.0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Member updates offer interest counter & transitions COLLECTING_INTEREST -> THRESHOLD_REACHED
  await assertSucceeds(
    memberDb
      .collection("societies")
      .doc("soc_1")
      .collection("offers")
      .doc("offer_1")
      .update({
        interestedCount: 1,
        totalRequestedQty: 2,
        status: "THRESHOLD_REACHED",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Member CANNOT arbitrarily edit offer title or negotiatedPrice
  await assertFails(
    memberDb
      .collection("societies")
      .doc("soc_1")
      .collection("offers")
      .doc("offer_1")
      .update({
        negotiatedPrice: 1.0,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Vendor registers catalog profile
  await assertSucceeds(
    vendorDb.collection("vendors").doc(VENDOR_UID).set({
      vendorUid: VENDOR_UID,
      businessName: "FreshValley Dairy Co.",
      category: "DAILY_ESSENTIALS",
      city: "Bengaluru",
      description: "Farm-direct A2 milk & dairy supply for apartment communities.",
      activeCatalog: true,
      completedDeals: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin invites vendor via quotation
  await assertSucceeds(
    adminDb.collection("quotations").doc("quote_1").set({
      societyId: "soc_1",
      societyName: "Greenwood Heights",
      societyAdminUid: ADMIN_UID,
      offerId: "offer_1",
      offerTitle: "Organic A2 Cow Milk Bulk Pack",
      offerCategory: "DAILY_ESSENTIALS",
      aggregatedMemberCount: 1,
      aggregatedQty: 2,
      estimatedPrice: 75.0,
      unitLabel: "per 1L bottle",
      vendorUid: VENDOR_UID,
      vendorName: "FreshValley Dairy Co.",
      status: "INVITED",
      quotedPrice: 0.0,
      availability: "",
      estimatedDelivery: "",
      proposalNotes: "",
      adminQuery: "",
      vendorReply: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Vendor submits quote
  await assertSucceeds(
    vendorDb.collection("quotations").doc("quote_1").update({
      status: "SUBMITTED",
      quotedPrice: 64.0,
      availability: "Daily 6 AM",
      estimatedDelivery: "Starts Monday",
      proposalNotes: "Glass bottles included, free doorstep drop.",
      vendorReply: "",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Vendor CANNOT accept their own quote
  await assertFails(
    vendorDb.collection("quotations").doc("quote_1").update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Vendor queries own invitations; cannot query admin's participations
  await assertSucceeds(
    vendorDb.collection("quotations").where("vendorUid", "==", VENDOR_UID).get()
  );
  await assertFails(
    vendorDb.collection("participations").where("societyAdminUid", "==", ADMIN_UID).get()
  );

  // Admin accepts quote and publishes final offer
  await assertSucceeds(
    adminDb.collection("quotations").doc("quote_1").update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    adminDb
      .collection("societies")
      .doc("soc_1")
      .collection("offers")
      .doc("offer_1")
      .update({
        status: "FINAL_OFFER_PUBLISHED",
        negotiatedPrice: 64.0,
        selectedVendorUid: VENDOR_UID,
        selectedVendorName: "FreshValley Dairy Co.",
        selectedQuotationId: "quote_1",
        finalTerms: "Free cancellation within 24 hours.",
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Member confirms Stage 2 Final Order
  await assertSucceeds(
    memberDb
      .collection("participations")
      .doc("soc_1_offer_1_" + MEMBER_UID)
      .update({
        stage: "CONFIRMED",
        confirmedQty: 3,
        unitPrice: 64.0,
        updatedAt: new Date(Date.now() - 200),
      })
  );
});
