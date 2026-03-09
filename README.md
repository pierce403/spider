# Spider

Spider is an Android SDR app for local TPMS reconnaissance. It keeps the useful parts of the Unagi SDR/TPMS work, drops the Bluetooth stack entirely, and focuses on capturing `rtl_433` JSON from either:

- A USB-attached RTL-SDR dongle
- A USB-attached HackRF One
- A network bridge streaming newline-delimited `rtl_433` JSON

The app stores observations locally, shows live and historical sensor sightings, exposes raw JSON for export, and uses a yellow-on-black UI theme.

The project landing page is intended for `https://spider.surf/`.

## Scope

- SDR-only capture flow
- TPMS observation parsing and vendor lookup
- Local Room database for sensors and sightings
- Sensor detail view with rename, copy, save, and share
- Diagnostics screen with runtime state and recent log output
- Static `index.html` landing page describing the project

## Build

- JDK 17
- Android SDK with API 35
- `./gradlew assembleDebug`
- `./gradlew installDebug`
- `./gradlew testDebugUnitTest`

If the SDK path is not already configured, this repo uses `local.properties` with:

```properties
sdk.dir=/home/pierce/Android/Sdk
```

## APK

The current debug build can be staged from:

- `app/build/outputs/apk/debug/app-debug.apk`

The site download is intended to point at:

- `downloads/spider-v0.1.0-debug.apk`

## SDR notes

- USB mode auto-detects supported hardware by VID/PID.
- Supported USB hardware IDs currently include common RTL2832U dongles and HackRF One.
- Network mode expects newline-delimited `rtl_433` JSON over TCP.
- Frequency presets are `315 MHz` and `433.92 MHz`.
- Gain is optional; leaving it blank keeps automatic gain handling.

## Privacy

- All observations stay on-device by default.
- No cloud sync is included.
- No Bluetooth collection remains in the app.
