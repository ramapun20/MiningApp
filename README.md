# The Mining Android app

The Mining is a Kotlin and Jetpack Compose Android app. The current project is a
single Gradle `:app` module with Clean Architecture package boundaries:

- `domain/model` contains product, map-place, and chat entities.
- `domain/repository` defines the repository contract.
- `domain/usecase` contains the product, map, and chat use cases.
- `data/repository` provides the local demo implementation.
- `MainActivity.kt` hosts Compose presentation and app navigation.

## Run

Open the project in Android Studio and run the `app` configuration. The
debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

The map uses osmdroid with OpenStreetMap tiles; no Google Maps API key or
billing configuration is required. Map tiles require internet access, and
live device location requires the user to grant location permission. The map
shows representative demo mine and market locations in Mauritania and credits
OpenStreetMap contributors.

## Account access

The app signs users in with Firebase Phone Authentication and SMS one-time
codes. To enable it:

1. Register an Android app using the exact package name
   `com.mining.app` in your Firebase project and place its
   `google-services.json` in `app/`.
2. Enable **Phone** in Firebase Authentication sign-in providers and configure
   the SMS region policy for the countries where users will receive codes.
3. Create a Cloud Firestore database in the Firebase console. From the project
   root, run `firebase deploy --only firestore:rules --project YOUR_PROJECT_ID`
   to deploy the included `firestore.rules` using `firebase.json`. Install and
   sign in to the Firebase CLI first, and replace `YOUR_PROJECT_ID` with the
   project ID used by `google-services.json`.
4. Add the app signing certificate SHA-1 and SHA-256 fingerprints in Firebase.
   Firebase test phone numbers can be used while testing without sending real
   SMS messages.
5. Sync Gradle and run the app. Enter phone numbers in international format,
   for example `+22212345678`.

Firebase creates a phone-auth account after the first successful verification;
subsequent verifications sign in to that account. SMS delivery and quotas are
controlled by Firebase. No Firebase service account key or provider secret is
stored in the Android app. The config file must match the app package exactly.
After a successful account registration, the app stores the user's UID, phone
number, full name, email address, and timestamps in `users/{uid}`. Firestore
rules restrict access to each signed-in user’s own profile. Signing in later
merges the phone and update timestamp without deleting the saved name or email.

The Chats tab now contains only a private conversation with The Mining Admin.
Messages are stored in `supportChats/{uid}/messages` and can only be read by
that account or a Firebase user with the trusted `admin: true` custom claim.
The admin claim must be assigned from a trusted server using the Firebase
Admin SDK; never let a client assign its own claim. Deploy the updated
`firestore.rules` after enabling Firestore. This app provides the account
holder's chat client; an authenticated admin client or trusted support workflow
must be configured separately to read and reply to user conversations.
For this project's local debug build, `.\gradlew signingReport` reports SHA-1
`D6:76:13:62:13:F3:3E:A2:9A:F4:3E:D8:EE:6E:16:6F:3B:58:9F:35` and SHA-256
`C4:B3:47:64:9F:3B:42:81:8B:69:57:58:24:0C:A0:D1:D4:2D:9A:63:1D:49:74:3D:B1:8E:E3:50:D5:8B:29:EA`.
These are the local debug fingerprints only; add the signing fingerprints used
for any release build separately.

## Demo-only integrations

Marketplace inquiry opens a local chat; messages are held in memory and are not
sent to a seller. Listings and Mauritania map places are sample data. Validate
real business locations before production use. No payment is taken by the
Buy / Inquire action.
