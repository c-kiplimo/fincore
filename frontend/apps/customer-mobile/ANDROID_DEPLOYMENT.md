# Android App Deployment Guide

**Customer Mobile App - FinCore**

This guide explains how to install and run the Customer Mobile App on your physical Android device.

---

## Prerequisites

Before starting, ensure you have:

- ✅ Android phone (Android 6.0 or higher)
- ✅ USB cable to connect phone to computer
- ✅ Node.js installed (v18+)
- ✅ Android Studio installed (for Android SDK)
- ✅ Project dependencies installed (`npm install`)

---

## Method 1: Install via USB (Development Build)

This is the **easiest method** for testing during development.

### Step 1: Enable Developer Mode on Your Phone

1. **Open Settings** on your Android phone
2. **Go to "About phone"** (or "About device")
3. **Find "Build number"** (usually at the bottom)
4. **Tap "Build number" 7 times**
   - You'll see a message: "You are now a developer!"
5. **Go back to Settings**
6. **Open "Developer options"** (should appear above or below "About phone")

### Step 2: Enable USB Debugging

1. In **Developer options**, scroll down
2. **Enable "USB debugging"**
3. You may also want to enable:
   - "Stay awake" (keeps screen on while charging)
   - "Install via USB" (allows app installation)

### Step 3: Connect Phone to Computer

1. **Connect your Android phone** to your computer via USB cable
2. On your phone, you'll see a popup asking for USB debugging permission
3. **Check "Always allow from this computer"**
4. **Tap "Allow"**

### Step 4: Verify Phone Connection

Open a terminal and run:

```bash
# Navigate to mobile app directory
cd frontend/apps/customer-mobile

# Check if device is connected
npx react-native doctor

# Or check with adb directly
adb devices
```

**Expected Output**:
```
List of devices attached
ABC123XYZ    device
```

If you see your device listed, you're ready to proceed!

**If no devices show up**:
- Try a different USB cable
- Make sure USB debugging is enabled
- Try revoking and re-allowing USB debugging permissions

### Step 5: Install and Run the App

```bash
# Make sure you're in the mobile app directory
cd frontend/apps/customer-mobile

# Install dependencies (first time only)
npm install

# Start Metro bundler
npm start
```

**In a new terminal** (keep Metro running):

```bash
cd frontend/apps/customer-mobile

# Install and run on your connected phone
npm run android
```

**What happens**:
1. The app builds (takes 2-5 minutes first time)
2. APK is installed on your phone
3. App opens automatically
4. Metro bundler serves the JavaScript code

**Success!** The app should now be running on your phone.

---

## Method 2: Install APK Directly (No USB Cable)

If you want to share the app with others or install without a cable, build an APK file.

### Step 1: Build Debug APK

```bash
cd frontend/apps/customer-mobile

# Build debug APK
cd android
./gradlew assembleDebug
cd ..
```

**APK Location**:
```
frontend/apps/customer-mobile/android/app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Transfer APK to Your Phone

**Option A: USB Transfer**
1. Connect phone via USB
2. Enable "File Transfer" mode
3. Copy `app-debug.apk` to your phone's Downloads folder

**Option B: Cloud Transfer**
1. Upload `app-debug.apk` to Google Drive / Dropbox
2. Download on your phone

**Option C: Email**
1. Email the APK to yourself
2. Download on your phone

### Step 3: Install APK on Phone

1. **Open file manager** on your phone
2. **Navigate to Downloads** (or wherever you saved the APK)
3. **Tap on `app-debug.apk`**
4. You may see a warning: "Install unknown apps"
   - **Tap "Settings"**
   - **Enable "Allow from this source"**
   - **Go back and tap the APK again**
5. **Tap "Install"**
6. **Tap "Open"** when installation completes

**Success!** The app is now installed on your phone.

---

## Method 3: Build Release APK (Production)

For a production-ready APK (smaller size, better performance):

### Step 1: Generate a Signing Key

```bash
cd frontend/apps/customer-mobile/android/app

# Generate keystore (first time only)
keytool -genkeypair -v -storetype PKCS12 -keystore fincore.keystore -alias fincore -keyalg RSA -keysize 2048 -validity 10000
```

**You'll be asked**:
- Keystore password (remember this!)
- Your name, organization, etc.

**Important**: Keep `fincore.keystore` safe! You'll need it for all future updates.

### Step 2: Configure Signing

Create `android/gradle.properties` (or add to existing):

```properties
MYAPP_UPLOAD_STORE_FILE=fincore.keystore
MYAPP_UPLOAD_KEY_ALIAS=fincore
MYAPP_UPLOAD_STORE_PASSWORD=your-keystore-password
MYAPP_UPLOAD_KEY_PASSWORD=your-key-password
```

**Important**: Add `gradle.properties` to `.gitignore` to keep passwords secret!

### Step 3: Update Build Configuration

Edit `android/app/build.gradle`:

```gradle
android {
    ...
    signingConfigs {
        release {
            if (project.hasProperty('MYAPP_UPLOAD_STORE_FILE')) {
                storeFile file(MYAPP_UPLOAD_STORE_FILE)
                storePassword MYAPP_UPLOAD_STORE_PASSWORD
                keyAlias MYAPP_UPLOAD_KEY_ALIAS
                keyPassword MYAPP_UPLOAD_KEY_PASSWORD
            }
        }
    }
    buildTypes {
        release {
            ...
            signingConfig signingConfigs.release
        }
    }
}
```

### Step 4: Build Release APK

```bash
cd frontend/apps/customer-mobile

# Build release APK
cd android
./gradlew assembleRelease
cd ..
```

**Release APK Location**:
```
frontend/apps/customer-mobile/android/app/build/outputs/apk/release/app-release.apk
```

This APK is:
- ✅ Smaller size (~20-30% smaller)
- ✅ Better performance (optimized)
- ✅ Ready for distribution
- ✅ Signed with your key (required for Play Store)

---

## Connecting to Backend API

### Local Development (Phone on Same WiFi)

If your phone and computer are on the **same WiFi network**:

1. **Find your computer's local IP address**:

   **Windows**:
   ```bash
   ipconfig
   ```
   Look for "IPv4 Address" (e.g., `192.168.1.100`)

   **macOS/Linux**:
   ```bash
   ifconfig
   ```
   Look for `inet` address

2. **Update API URL** in the app:

   Edit `frontend/apps/customer-mobile/src/services/api.ts`:
   ```typescript
   const API_BASE_URL = __DEV__
     ? 'http://192.168.1.100:8080'  // Your computer's IP
     : 'https://api.fincore.com';
   ```

3. **Rebuild the app**:
   ```bash
   npm run android
   ```

### Production (Using Real API)

If you have a deployed backend API:

```typescript
const API_BASE_URL = 'https://api.fincore.com';
```

---

## Troubleshooting

### Issue: "adb: device offline"

**Solution**:
```bash
adb kill-server
adb start-server
adb devices
```

### Issue: "Installation failed"

**Solution**:
1. Uninstall existing app from phone
2. Revoke USB debugging permissions
3. Re-allow USB debugging
4. Try again

### Issue: "Unable to load script from assets 'index.android.bundle'"

**Solution**:
```bash
# Make sure Metro bundler is running
npm start

# In another terminal
npm run android
```

### Issue: "App keeps crashing"

**Solution**:
1. Check Metro bundler logs for errors
2. Clear app data on phone:
   - Settings → Apps → FinCore → Storage → Clear Data
3. Rebuild app:
   ```bash
   cd android
   ./gradlew clean
   cd ..
   npm run android
   ```

### Issue: "Cannot connect to Metro"

**Solution**:
```bash
# On your phone, shake the device or press Ctrl+M
# Select "Settings"
# Change "Debug server host & port" to:
192.168.1.100:8081  # Your computer's IP:8081
```

### Issue: "Build failed with Gradle error"

**Solution**:
```bash
cd android
./gradlew clean
cd ..
npm run android
```

---

## Testing on Multiple Devices

You can connect multiple Android devices and install on all:

```bash
# List all connected devices
adb devices

# Install on all devices
npm run android

# Install on specific device
adb -s ABC123XYZ install android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Updating the App

When you make code changes:

### Hot Reload (Automatic)

1. **Save your code changes**
2. **Press R twice** in Metro bundler terminal
3. Or **shake your device** → select "Reload"

**Changes appear instantly!**

### Full Rebuild (If needed)

```bash
npm run android
```

---

## Removing the App

### From Phone
1. Long-press the app icon
2. Drag to "Uninstall"

### From Computer
```bash
adb uninstall com.fincore.customer
```

---

## Performance Tips

### Enable Proguard (Reduces APK Size)

Edit `android/app/build.gradle`:

```gradle
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
    }
}
```

### Enable Hermes (Faster Startup)

Already enabled by default in React Native 0.76+

Check `android/app/build.gradle`:
```gradle
project.ext.react = [
    enableHermes: true  // ✅ Should be true
]
```

---

## App Permissions

The app requests these permissions (defined in `AndroidManifest.xml`):

- **INTERNET**: Connect to API
- **CAMERA**: QR code scanning (future)
- **READ_EXTERNAL_STORAGE**: Document upload (future)
- **WRITE_EXTERNAL_STORAGE**: Save receipts (future)

Users will be prompted when these features are used.

---

## Sharing the App

### With Team Members

**Option 1: Send APK directly**
1. Build debug APK
2. Share `app-debug.apk` via email/Slack/Drive
3. They install on their phone

**Option 2: Use Firebase App Distribution**
1. Upload APK to Firebase
2. Invite testers via email
3. They get download link

**Option 3: Internal Testing (Google Play)**
1. Create Google Play Console account
2. Upload APK to Internal Testing track
3. Invite testers via email
4. They install via Play Store

### With End Users (Production)

1. **Build release APK** (signed)
2. **Upload to Google Play Console**
3. **Create app listing** (description, screenshots)
4. **Submit for review**
5. **Publish to Play Store**

---

## Quick Reference

### Daily Development

```bash
# Terminal 1: Start Metro
npm start

# Terminal 2: Install on phone
npm run android

# Hot reload: Press R R in Metro terminal
# Or shake device → Reload
```

### Build APK for Sharing

```bash
cd android
./gradlew assembleDebug
cd ..

# APK is at:
# android/app/build/outputs/apk/debug/app-debug.apk
```

### Check Connected Devices

```bash
adb devices
```

### View Logs

```bash
# Android logs
adb logcat

# React Native logs (in Metro)
# Already visible when you run npm start
```

---

## Platform Details

**App Package**: `com.fincore.customer`
**Min SDK**: Android 6.0 (API 23)
**Target SDK**: Android 14 (API 34)
**React Native**: 0.76.6

---

## Security Notes

### Debug APK
- ✅ Fast to build
- ✅ Easy to install
- ⚠️ Larger file size
- ⚠️ Not optimized
- ⚠️ **DO NOT** distribute to end users

### Release APK
- ✅ Optimized performance
- ✅ Smaller file size
- ✅ Signed with your key
- ✅ Ready for production
- ✅ Required for Play Store

**Never share your keystore file or passwords!**

---

## Next Steps

After successfully installing the app:

1. ✅ Test all features (Wallet, Payments, Transfers, Profile)
2. ✅ Test on different Android versions
3. ✅ Test with real API (not mock data)
4. ✅ Add crash reporting (Sentry)
5. ✅ Add analytics (Firebase Analytics)
6. ✅ Prepare for Play Store submission

---

## Support

**Logs Location**: Check Metro bundler output for errors

**Common Issues**: See Troubleshooting section above

**Documentation**:
- React Native Docs: https://reactnative.dev/docs/running-on-device
- Android Developer Docs: https://developer.android.com/studio/run/device

---

**Happy Developing!** 🚀

For more information, see the main README in the customer-mobile directory.

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
