# SmartTrip - Real-Time Group Location Tracking Android App

SmartTrip is a Kotlin-based Android application that allows users to create group trip sessions, share deep links via WhatsApp, and view real-time location updates of all group members on Google Maps.

## Features
- **Real-Time Location Tracking:** Uses Google Maps SDK and FusedLocationProviderClient.
- **Firebase Realtime Database:** Dynamically syncs live user coordinates across all active group participants.
- **Deep Linking:** Generates custom `smarttrip://join?code=TRIPXXXX` links for seamless 1-tap entry from WhatsApp.
- **Optimized Performance:** Designed and tested on physical hardware (Android 15 / vivo Y56 5G).

## Tech Stack
- Kotlin
- Android Studio (Gradle Kotlin DSL `.kts`)
- Google Maps SDK & Location Services
- Firebase Realtime Database

## Setup Instructions
1. Clone this repository: `git clone https://github.com/your-username/SmartTrip.git`
2. Open the project in Android Studio.
3. Replace `YOUR_GOOGLE_MAPS_API_KEY` in `app/src/main/AndroidManifest.xml` with your actual Google Maps API key.
4. Add your `google-services.json` file inside the `app/` folder for Firebase connection.
5. Build and run on a physical device or emulator!
