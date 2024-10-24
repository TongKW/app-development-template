import jwt from "jsonwebtoken";
import { X509Certificate } from "crypto";

export async function handler(event) {
  try {
    // Parse the incoming request body
    const body = JSON.parse(event.body);

    // For Notification V2, the payload is a JWT
    const signedPayload = body.signedPayload;
    const userId = body.userId;

    if (!signedPayload) {
      throw new Error("Missing signedPayload in the request body.");
    }

    // Decode the JWT header to get the x5c array
    const decodedHeader = jwt.decode(signedPayload, { complete: true });
    if (!decodedHeader || !decodedHeader.header || !decodedHeader.header.x5c) {
      throw new Error("Invalid JWT header: x5c not found.");
    }

    const x5cArray = decodedHeader.header.x5c;
    if (!Array.isArray(x5cArray) || x5cArray.length === 0) {
      throw new Error("Invalid x5c certificate chain.");
    }

    // Convert the first certificate in the chain to PEM format
    const leafCertPEM = convertCertificateToPEM(x5cArray[0]);

    // Verify the JWT using the public key from the leaf certificate
    const publicKey = extractPublicKeyFromCertificate(leafCertPEM);

    const decodedPayload = jwt.verify(signedPayload, publicKey, {
      algorithms: ["ES256"],
    });

    // Extract necessary information
    const notificationType = decodedPayload.notificationType;
    const subtype = decodedPayload.subtype;
    const data = decodedPayload.data;

    // Now, extract and verify signedTransactionInfo and signedRenewalInfo
    const { transactionInfo, renewalInfo } = await extractAndVerifyInfo(data);

    if (!transactionInfo) {
      return {
        statusCode: 400,
        body: JSON.stringify({ message: "Transactional Info not found." }),
      };
    }

    // Handle the notificationType as per your business logic
    await handleNotification(notificationType, transactionInfo, userId, subtype);

    // Return a 200 response
    return {
      statusCode: 200,
      body: JSON.stringify({ message: "Notification processed successfully" }),
    };
  } catch (error) {
    console.error("Error processing notification:", error);
    return {
      statusCode: 400,
      body: JSON.stringify({ message: "Error processing notification" }),
    };
  }
}

// Helper function to convert a certificate to PEM format
function convertCertificateToPEM(certBase64) {
  return `-----BEGIN CERTIFICATE-----\n${certBase64}\n-----END CERTIFICATE-----`;
}

// Function to extract public key from certificate
function extractPublicKeyFromCertificate(certPEM) {
  const x509 = new X509Certificate(certPEM);
  return x509.publicKey;
}

// Function to verify and decode nested JWTs
async function verifyAndDecodeJWT(signedJWT) {
  const decodedHeader = jwt.decode(signedJWT, { complete: true });
  if (!decodedHeader || !decodedHeader.header || !decodedHeader.header.x5c) {
    throw new Error("Invalid JWT header: x5c not found.");
  }

  const x5cArray = decodedHeader.header.x5c;
  if (!Array.isArray(x5cArray) || x5cArray.length === 0) {
    throw new Error("Invalid x5c certificate chain.");
  }

  const leafCertPEM = convertCertificateToPEM(x5cArray[0]);

  const publicKey = extractPublicKeyFromCertificate(leafCertPEM);

  const decodedPayload = jwt.verify(signedJWT, publicKey, {
    algorithms: ["ES256"],
  });

  return decodedPayload;
}

// Function to extract and verify transaction and renewal info
async function extractAndVerifyInfo(data) {
  const signedTransactionInfo = data.signedTransactionInfo;
  const signedRenewalInfo = data.signedRenewalInfo;

  // Verify signedTransactionInfo
  let transactionInfo = null;
  if (signedTransactionInfo) {
    transactionInfo = await verifyAndDecodeJWT(signedTransactionInfo);
  }

  // Verify signedRenewalInfo
  let renewalInfo = null;
  if (signedRenewalInfo) {
    renewalInfo = await verifyAndDecodeJWT(signedRenewalInfo);
  }

  return { transactionInfo, renewalInfo };
}

// Function to handle notifications based on type
async function handleNotification(notificationType, transactionInfo, userId, subtype) {
  switch (notificationType) {
    case "SUBSCRIBED":
      await handleSubscriptionStarted(transactionInfo, userId, subtype);
      break;
    case "ONE_TIME_CHARGE":
      await handleLifetimePurchased(transactionInfo, userId, subtype);
      break;
    case "EXPIRED":
      await handleSubscriptionExpired(transactionInfo, userId, subtype);
      break;
    case "REFUND":
      await handleRefund(transactionInfo, userId, subtype);
      break;
    case "DID_CHANGE_RENEWAL_STATUS":
      await handleRenewalStatusChange(transactionInfo, userId, subtype);
      break;
    case "GRACE_PERIOD_EXPIRED":
      await handleGracePeriodExpired(transactionInfo, userId, subtype);
      break;
    // Add more cases as needed
    // Reference: https://developer.apple.com/documentation/appstoreservernotifications/notificationtype
    default:
      console.log(`Unhandled notification type: ${notificationType}`);
  }
}

// Example handler functions for different notification types
async function handleSubscriptionStarted(transactionInfo, userId, subtype) {
  console.log("Subscription started:", transactionInfo);

  const { productId, transactionId, originalTransactionId } = transactionInfo;

  // Implement your logic to handle a subscription start event.
  // For example:
  // - Record the transaction in your database.
  // - Grant user access.

  // Example:
  // await database.insertTransaction(...);
  // await userAccess.grantSubscription(userId, productId);
}

async function handleLifetimePurchased(transactionInfo, userId, subtype) {
  console.log("Lifetime purchase received:", transactionInfo);

  const { productId, transactionId, originalTransactionId } = transactionInfo;

  // Implement your logic to handle a lifetime purchase event.
  // For example:
  // - Record the purchase.
  // - Grant user lifetime access.
}

async function handleSubscriptionExpired(transactionInfo, userId, subtype) {
  console.log("Subscription expired:", transactionInfo);

  const { productId, transactionId, originalTransactionId } = transactionInfo;

  // Implement your logic to handle a subscription expiration.
  // use originalTransactionId to refer to the corresponding expired transaction
  // For example:
  // - Update the user's subscription status.
  // - Revoke user access.
}

// Example handler for REFUND
async function handleRefund(transactionInfo, userId, subtype) {
  console.log("Refund received:", transactionInfo);

  const { productId, transactionId, originalTransactionId } = transactionInfo;

  // Implement your logic to handle a refund.
  // For example:
  // - Record the refund.
  // - Revoke user access if necessary.
}

async function handleRenewalStatusChange(transactionInfo, userId, subtype) {
  console.log("Renewal status changed:", transactionInfo);

  const { productId, transactionId } = transactionInfo;

  // Implement your logic to handle a renewal status change.
  // For example:
  // - Update the user's renewal preferences.
}

async function handleGracePeriodExpired(transactionInfo, userId, subtype) {
  console.log("Grace period expired:", transactionInfo);

  const { productId, transactionId, originalTransactionId } = transactionInfo;

  // Implement your logic to handle grace period expiration.
  // For example:
  // - Notify user.
  // - Revoke access.
}
