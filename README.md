# KeepSoundbar for Android TV

An Android TV app that prevents your soundbar from entering sleep/standby mode by playing an inaudible ultrasonic tone every 5 minutes.

## How it works

The app runs a foreground service that plays a 19kHz sine wave tone at 15% amplitude once every 5 minutes. The tone is above the typical human hearing range and inaudible in practice, but keeps the audio output active so the soundbar never detects silence long enough to auto-power off.

## Features

- Starts automatically on device boot — no interaction needed after the first launch
- Runs as a foreground service, resistant to being killed by the system
- Restarts itself if killed (`START_STICKY`)
- Minimal resource usage (audio plays for ~1 second every 5 minutes)

## Requirements

- Google TV / Android TV device running Android 7.0 (API 24) or higher
- Sideloading enabled (Unknown sources)

## Installation

### Via ADB (recommended)

1. Enable **Developer Options** on your Android TV device (tap Build Number 7 times in Device Info)
2. Enable **ADB Debugging** in Developer Options
3. Connect from your PC:
   ```bash
   adb connect <device-ip>:5555
   ```
   An authorization dialog will appear on your TV — approve it, then reconnect.
4. If internal storage is low, free up cache first:
   ```bash
   adb shell pm trim-caches 500000000
   ```
5. Push and install:
   ```bash
   adb push app-release.apk /data/local/tmp/app.apk
   adb shell pm install -r /data/local/tmp/app.apk
   ```

### Via file manager

Transfer the APK to the device (USB drive, LocalSend, etc.), open it with a file manager, and install. Make sure the file manager app has **Install unknown apps** permission granted specifically for it.

## Usage

1. Open the app from the launcher
2. Press **Start** — the service begins running in the background
3. That's it. The service will restart automatically after every reboot.

Press **Stop** to manually stop the service.

## Building from source

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

## Tested on

| Device | Soundbar |
|--------|----------|
| ✅ Xiaomi TV Box S 2nd Gen (MITV-AFKRO) — Google TV 11 | Philips TAB6305/10 |
