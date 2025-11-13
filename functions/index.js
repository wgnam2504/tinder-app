/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

// Import các module cần thiết
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { StreamChat } = require("stream-chat");

// Khởi tạo Firebase Admin SDK
admin.initializeApp();

// Lấy key và secret từ biến môi trường đã cấu hình
const api_key = functions.config().stream.key;
const api_secret = functions.config().stream.secret;

/**
 * Cloud Function này được kích hoạt qua HTTP request.
 * Nó sẽ tạo một token cho GetStream Chat.
 *
 * Cách gọi từ client: Gửi một POST request đến URL của function với body là:
 * {
 *   "data": {
 *     "userId": "THE_USER_ID_TO_GENERATE_TOKEN_FOR"
 *   }
 * }
 */
exports.getStreamToken = functions.https.onCall(async (data, context) => {
  // Kiểm tra xem người dùng đã xác thực với Firebase chưa
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated."
    );
  }

  const userId = data.userId;
  if (!userId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The function must be called with a 'userId' argument."
    );
  }

  // Khởi tạo Stream-Chat server client
  const serverClient = StreamChat.getInstance(api_key, api_secret);

  try {
    // Tạo token cho user ID được cung cấp
    const token = serverClient.createToken(userId);
    console.log(`Successfully created token for user: ${userId}`);

    // Trả token về cho client
    return { token: token };
  } catch (error) {
    console.error(`Unable to create Stream token for user: ${userId}`, error);
    throw new functions.https.HttpsError(
      "internal",
      "An error occurred while creating the token."
    );
  }
});
