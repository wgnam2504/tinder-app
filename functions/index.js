
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { StreamChat } = require("stream-chat");
const fs = require("fs");
const path = require("path");

// Initialize Firebase Admin SDK
admin.initializeApp();

let api_key;
let api_secret;

// --- Bulletproof Config Loading - V2 (Handles file encoding issues) ---
// Check if we are running in the emulator
if (process.env.FUNCTIONS_EMULATOR === 'true') {
  console.log("Emulator environment detected. Loading config manually from .runtimeconfig.json");
  try {
    // Construct the path to the file
    const configPath = path.join(__dirname, '.runtimeconfig.json');
    
    // Read the file as a raw string
    let fileContent = fs.readFileSync(configPath, 'utf8');
    
    // Check for and remove the BOM (Byte Order Mark) character
    if (fileContent.charCodeAt(0) === 0xFEFF) {
      console.log("BOM detected. Stripping it from the file content.");
      fileContent = fileContent.slice(1);
    }

    // Parse the cleaned-up string as JSON
    const runtimeConfig = JSON.parse(fileContent);

    api_key = runtimeConfig.stream.key;
    api_secret = runtimeConfig.stream.secret;
    console.log("Successfully loaded config from .runtimeconfig.json");
  } catch (error) {
    console.error(
      "FATAL ERROR: Could not load or parse .runtimeconfig.json.",
      "Please ensure the file exists in the /functions directory and is valid JSON.",
      error
    );
  }
} else {
  // We are in the deployed production environment
  console.log("Production environment detected. Loading from functions.config()");
  try {
    api_key = functions.config().stream.key;
    api_secret = functions.config().stream.secret;
  } catch (error) {
     console.error(
      "FATAL ERROR: Could not load functions.config().",
      "Please ensure you have set the config on Firebase using 'firebase functions:config:set stream.key=...' etc.",
      error
    );
  }
}
// --- End of Bulletproof Config Loading ---


/**
 * Creates a Stream Chat token for an authenticated user.
 */
exports.getStreamToken = functions.https.onCall(async (data, context) => {
  // Check if the user is authenticated
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
  
  // Final check to ensure keys are loaded
  if (!api_key || !api_secret) {
      throw new functions.https.HttpsError(
          "internal",
          "Stream API key or secret is not configured correctly on the server."
      );
  }

  // Initialize the Stream Chat server client
  const serverClient = StreamChat.getInstance(api_key, api_secret);

  try {
    // Create a token for the given user
    const token = serverClient.createToken(userId);
    console.log(`Successfully created token for user: ${userId}`);

    // Return the token to the client
    return { token: token };
  } catch (error) {
    console.error(`Unable to create Stream token for user: ${userId}`, error);
    throw new functions.https.HttpsError(
      "internal",
      "An error occurred while creating the token."
    );
  }
});
