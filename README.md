# HitLarm - Android Anti-Oversleep Solution

**HitLarm** is a premium, high-performance anti-oversleep alarm application for Android. It uses interactive, movement-based, and cognitive challenges to ensure you are fully awake, alert, and active before your alarm can be silenced. 

The application is built natively in **Kotlin** using **Jetpack Compose** and features a sleek, cyberpunk-themed dark mode UI with glassmorphic cards and smooth micro-animations.

---

## Core Features

### 1. Wakeup Streak Dashboard
HitLarm keeps you motivated by tracking your consecutive wakeup days. 
* **Streak Cards**: Displays your *Current Wakeup Streak* and *Best Streak*.
* **Streak Freeze**: Life happens. If you are on vacation or sick, you can freeze your streak (up to 2 consecutive days) to protect it from resetting.
* **Streak History**: A dedicated log screen displaying the exact dates, times, and statuses (woke up, frozen, missed, skipped) of your wakeup history.

### 2. Time Setting format Toggle (12H / 24H Support)
Setting your alarm is seamless with a custom cyberpunk-themed Dial Time Picker. A format toggle allows you to switch between 12-hour (AM/PM) and 24-hour formats instantly.

### 3. Alarm Dismissal Challenges
To turn off an active alarm, you must complete one of the following interactive challenges:
* **Math Solver**: Solves a configurable number of random arithmetic equations (e.g. addition, multiplication) with an on-screen numpad.
* **Shake Challenge**: Monitors the hardware accelerometer sensor, requiring a set count of vigorous phone shakes to silence the alarm.
* **Barcode Scan**: Scans a target product barcode/QR code (such as your toothpaste or coffee jar) using the camera to verify you are out of bed.
* **Workout (Pushups/Squats)**: Uses the front camera to track pushups or squats. 
  * **Start Challenge Overlay**: Includes a manual "Start Counting" button overlay. The sensor will remain paused until you tap the button and position yourself, preventing false positives.

### 4. Skip Challenge Streak Freeze
If you need to silence the alarm immediately due to an emergency:
* Tap **Skip the Challenge**.
* Type a justification reason in the mandatory dialog field.
* The alarm silences, and your streak is **preserved (Streak Frozen)** rather than being reset to `0`. 
* The reason is logged as a warning entry under your **Streak History**.

---

## Open Source Attribution

This project is open-sourced under the following attribution metadata:
* **Ideation & Concept:** Vikash Seelam
* **Place of Origin:** Ingolstadt
* **Created completely by:** Google Antigravity (AI Pair Programmer)
* **Creation Date:** 06.07.2026

---

## App Size & Build Optimization

To ensure a lightweight and smooth user experience, the release build utilizes **ABI splits**. Instead of downloading a monolithic "fat" APK containing libraries for all processors, users download a package built specifically for their device's architecture:
* **Monolithic APK Size:** **120 MB**
* **Optimized Split APK Size:** **~36 MB** (arm64-v8a) / **~29 MB** (armeabi-v7a) — **an over 70% size reduction!**

---

## Project Structure & Clean Assets
All showcase screenshots, verification assets, and app design previews have been moved into a dedicated directory:
* **Screenshots folder:** `art/`

---

## How to Build the App

### Prerequisites
* Java JDK 17 (recommended: JetBrains Runtime or OpenJDK 17)
* Android SDK command line tools or Android Studio

### Compile Debug Build
To build an APK for testing/development:
```bash
./gradlew assembleDebug
```
The output APKs will be located in:
`app/build/outputs/apk/debug/`

### Compile Production Release
To compile the final optimized split APKs:
```bash
./gradlew assembleRelease
```
The output split APKs will be located in:
`app/build/outputs/apk/release/`
