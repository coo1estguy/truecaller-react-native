# truecaller-react-native

A cross-platform React Native and Expo module for integrating the Truecaller SDK on iOS and Android.

This package provides a unified API for Truecaller authentication. It wraps the Truecaller SDK for Android (OAuth) and iOS, exposing modern async methods while automatically configuring the native projects via Expo config plugins.

## Features

- 📱 **Cross-Platform**: Works seamlessly on both **Android** and **iOS**.
- 🚀 **New Architecture Ready**: Full support for React Native's New Architecture (Fabric).
- 🧩 **Expo Config Plugin**: Zero manual linking—automatic native setup via `app.json`.
- 🔒 **Secure Authentication**: Uses PKCE OAuth on Android and cryptographic payload signatures on iOS.
- ⚡ **Unified Async API**: Clean, Promise-based functions to handle the auth flow gracefully.
- 📘 **Fully Typed**: Written in TypeScript with first-class type definitions included.
- 🌐 **Web Support**: Will be added if there is enough community interest/request.

## Installation

> ⚠️ **Warning:** This package contains custom native code and **will not work with Expo Go**. You must create a [development build](https://docs.expo.dev/develop/development-builds/introduction/) or prebuild your app.

```bash
npm install truecaller-react-native
```

## Configuration

This package requires native integration. If you are using Expo, add the config plugin to your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": [
      [
        "truecaller-react-native",
        {
          "iosAppKey": "YOUR_IOS_APP_KEY",
          "iosAppLink": "https://your.app.link",
          "androidClientId": "YOUR_ANDROID_CLIENT_ID"
        }
      ]
    ]
  }
}
```

> **Note:** The `androidClientId` is used for Android OAuth. The `iosAppKey` and `iosAppLink` are used for the iOS SDK integration.

After adding the plugin, you must rebuild your native project:

```bash
npx expo prebuild --clean
# OR run directly on a device/emulator
npx expo run:android
npx expo run:ios
```

## Usage

### 1. Import the library

```tsx
import { Platform } from 'react-native';
import * as Truecaller from 'truecaller-react-native';
```

### 2. Initialize the SDK

You must initialize the SDK before attempting to authenticate. This checks if the Truecaller app is installed and usable on the device.

```tsx
async function initTruecaller() {
  try {
    const result = await Truecaller.initializeAsync();
    
    // Check if Truecaller is installed and usable
    if (result.isUsable) {
      console.log("Truecaller is ready to use!");
    } else {
      console.log("Truecaller is not installed or not usable on this device.");
    }
  } catch (error) {
    console.error("Failed to initialize Truecaller", error);
  }
}
```

**Example Output:**
```json
{
  "initialized": true,
  "isUsable": true
}
```

### 3. Prompt Authentication (Cross-Platform)

The unified `promptAuthAsync` method handles triggering the Truecaller login flow. 
- On **Android**, this uses the PKCE OAuth flow and returns an `authorizationCode` and `codeVerifier`.
- On **iOS**, this directly returns the user's profile information, along with a `payload`, `signature`, and `signatureAlgorithm` for backend verification.

```tsx
async function loginWithTruecaller() {
  try {
    const result = await Truecaller.promptAuthAsync();
    
    if (Platform.OS === 'android') {
      // --- ANDROID RESULT ---
      const androidResult = result as Truecaller.TruecallerAuthResultAndroid;
      console.log("OAuth Code:", androidResult.authorizationCode);
      console.log("Code Verifier:", androidResult.codeVerifier);
      
      // Send the code and verifier to your backend to exchange for an access token
    } else if (Platform.OS === 'ios') {
      // --- IOS RESULT ---
      const iosResult = result as Truecaller.TruecallerAuthResultIOS;
      console.log("Profile Payload:", iosResult.payload);
      console.log("Signature:", iosResult.signature);
      
      // Send the payload and signature to your backend to verify the profile
    }
  } catch (error) {
    console.error("Truecaller authentication failed", error);
  }
}
```

**Example Output (Android):**
```json
{
  "authorizationCode": "4/0AeaYSH...",
  "codeVerifier": "abc123xyz...",
  "scopesGranted": ["profile", "phone"],
  "state": "some_state_value"
}
```

**Example Output (iOS):**
```json
{
  "payload": "eyJhbGciOi...",
  "signature": "MEYCIQC...",
  "signatureAlgorithm": "ES256"
}
```

> **Server-Side Verification Docs**  
> For instructions on how to verify these credentials on your backend, refer to the official Truecaller documentation:
> - **Android**: [Server-side response validation](https://docs.truecaller.com/truecaller-sdk/android/oauth-sdk-3.2.1/integration-steps/integrating-with-your-backend)
> - **iOS**: [Server-side response validation](https://docs.truecaller.com/truecaller-sdk/ios/server-side-response-validation)

### 4. iOS Specific: `requestProfileAsync`

If you are writing platform-specific code and want to explicitly request the iOS profile without triggering the Android OAuth flow, you can use `requestProfileAsync()`.

```tsx
import { Platform } from 'react-native';

async function fetchAppleProfile() {
  if (Platform.OS !== 'ios') return;

  try {
    const profile = await Truecaller.requestProfileAsync();
    console.log("Welcome,", profile.firstName);
  } catch (error) {
    console.error(error);
  }
}
```

**Example Output:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "countryCode": "US",
  "email": "john.doe@example.com",
  "gender": "male",
  "avatarUrl": "https://...",
  "city": "New York",
  "isVerified": true
}
```

### 5. Clear SDK Instance

If you need to cancel a pending request or clear the SDK state, you can call `clear()`:

```tsx
Truecaller.clear();
```

### 6. Full React Native Code Example

Here is a complete example of initializing the Truecaller SDK within a `useEffect` hook and enabling the login button only when the Truecaller app is usable on the device.

```tsx
import React, { useEffect, useState } from 'react';
import { View, Button, Platform, Alert } from 'react-native';
import * as Truecaller from 'truecaller-react-native';

export default function App() {
  const [isUsable, setIsUsable] = useState(false);

  useEffect(() => {
    async function initTruecaller() {
      try {
        const result = await Truecaller.initializeAsync();
        setIsUsable(result.isUsable);
      } catch (error) {
        console.error("Failed to initialize Truecaller", error);
      }
    }
    initTruecaller();
  }, []);

  const handleLogin = async () => {
    try {
      const result = await Truecaller.promptAuthAsync();
      
      if (Platform.OS === 'android') {
        const androidResult = result as Truecaller.TruecallerAuthResultAndroid;
        console.log("Android OAuth Code:", androidResult.authorizationCode);
      } else if (Platform.OS === 'ios') {
        const iosResult = result as Truecaller.TruecallerAuthResultIOS;
        console.log("iOS Profile Payload:", iosResult.payload);
      }
    } catch (error) {
      Alert.alert("Login Failed", "Could not authenticate with Truecaller.");
      console.error(error);
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Button 
        title="Login with Truecaller" 
        onPress={handleLogin} 
        disabled={!isUsable} 
      />
    </View>
  );
}
```

## API

### `initializeAsync(options?)`
Initialize the Truecaller SDK. Returns `{ initialized, isUsable }`.

On iOS, reads credentials from Info.plist (set by the config plugin).
On Android, accepts the following optional customization properties:

| Option | Type | Description |
|---|---|---|
| `buttonColor` | `string` | Button color as hex string (e.g. `"#4CAF50"`) |
| `buttonTextColor` | `string` | Button text color as hex string (e.g. `"#FFFFFF"`) |
| `consentMode` | `"popup" \| "bottomsheet"` | Layout mode for the consent screen |
| `footerType` | `"skip" \| "anotherNumber" \| "anotherMethod" \| "manually" \| "later"` | Footer CTA text |
| `buttonShape` | `"rounded" \| "rectangle"` | CTA button shape |
| `ctaTextPrefix` | `CtaTextPrefix` | CTA button text prefix |
| `loginTextPrefix` | `LoginTextPrefix` | Login text shown above the CTA button |
| `heading` | `ConsentHeading` | Contextual heading on the consent screen |
| `dismissOption` | `"secondaryCtaBorder" \| "crossButton"` | How the dismiss UI is shown |
| `sdkOption` | `"verifyTcUsersOnly" \| "verifyAllUsers"` | Verification scope (Truecaller users only vs all users) |
| `language` | `SupportedLanguage` | Consent screen language |
| `theme` | `"dark" \| "light"` | Consent screen color theme |

#### Supported String Values

- **`ConsentHeading`**: `"logInTo"`, `"signUpWith"`, `"signInTo"`, `"verifyNumberWith"`, `"registerWith"`, `"getStartedWith"`, `"proceedWith"`, `"verifyWith"`, `"verifyProfileWith"`, `"verifyYourProfileWith"`, `"verifyPhoneNoWith"`, `"verifyYourNoWith"`, `"continueWith"`, `"completeOrderWith"`, `"placeOrderWith"`, `"completeBookingWith"`, `"checkoutWith"`, `"manageDetailsWith"`, `"manageYourDetailsWith"`, `"loginToWithOneTap"`, `"subscribeTo"`, `"getUpdatesFrom"`, `"continueReadingOn"`, `"getNewUpdatesFrom"`, `"loginSignupWith"`
- **`CtaTextPrefix`**: `"continue"`, `"proceed"`, `"accept"`, `"confirm"`, `"use"`, `"continueWith"`, `"proceedWith"`
- **`LoginTextPrefix`**: `"toGetStarted"`, `"toContinue"`, `"toPlaceOrder"`, `"toCompleteYourPurchase"`, `"toCheckout"`, `"toCompleteYourBooking"`, `"toProceedWithYourBooking"`, `"toContinueWithYourBooking"`, `"toGetDetails"`, `"toViewMore"`, `"toContinueReading"`, `"toProceed"`, `"forNewUpdates"`, `"toGetUpdates"`, `"toSubscribe"`, `"toSubscribeAndGetUpdates"`
- **`SupportedLanguage`**: `"en"`, `"hi"`, `"mr"`, `"te"`, `"ml"`, `"ur"`, `"pa"`, `"ta"`, `"bn"`, `"kn"`, `"sw"`, `"ar"`

### `promptAuthAsync(options?)`
Trigger the Truecaller authentication flow. 

On Android, accepts the following options:

| Option | Type | Description |
|---|---|---|
| `scopes` | `string[]` | OAuth scopes to request. Defaults to `["profile", "phone"]`. Valid options include `"openid"`, `"offline_access"`, `"email"`, `"address"`. |

Returns `{ authorizationCode, codeVerifier, scopesGranted, state }` on Android.
Returns `{ payload, signature, signatureAlgorithm }` on iOS.

### `requestProfileAsync()` — iOS
Request the user's Truecaller profile directly.

Returns `{ firstName, lastName, phoneNumber, countryCode, email, gender, avatarUrl, city, isVerified }`.

### `clear()`
Clear the SDK instance. Rejects any pending promise with `ERR_CLEARED`.

## Handling errors
Import `TruecallerErrorCodes` for the full list.

| Code | Meaning |
|---|---|
| `ERR_USER_CANCELLED` | User dismissed the consent screen |
| `ERR_USER_DISMISSED` | User dismissed while loading (Android) |
| `ERR_USER_PRESSED_BACK` | User pressed the footer button (Android) |
| `ERR_NOT_INSTALLED` | Truecaller is not installed |
| `ERR_NOT_AVAILABLE` | OAuth flow is not usable |
| `ERR_SDK_ERROR` | Internal SDK error |
| `ERR_SDK_TOO_OLD` | SDK or device not compatible |
| `ERR_MISSING_CLIENT_ID` | Invalid partner credentials (Android) |
| `ERR_VERIFICATION_REQUIRED` | Additional verification required (Android) |
| `ERR_NETWORK_FAILURE` | Network error occurred (iOS) |
| `ERR_UNKNOWN_ERROR` | Unknown Truecaller error |
| `ERR_NOT_INITIALIZED` | `initializeAsync()` was not called |
| `ERR_PKCE_FAILED` | Failed to generate PKCE verifier or challenge |
| `ERR_CLEARED` | `clear()` was called while pending |
| `ERR_INIT_FAILED` | Failed to initialize the Truecaller SDK |
| `ERR_VERIFICATION_FAILED` | Failed to start or complete verification |
| `ERR_ALREADY_IN_PROGRESS` | A request is already pending |
| `ERR_IOS_APP_KEY_MISSING` | TruecallerAppKey is missing from Info.plist (iOS) |
| `ERR_IOS_APP_LINK_MISSING` | TruecallerAppLink is missing from Info.plist (iOS) |
| `ERR_IOS_USER_NOT_SIGNED_IN` | User is not signed in to Truecaller (iOS) |
| `ERR_IOS_UNAUTHORIZED_DEVELOPER` | Developer account is unauthorized (iOS) |
| `ERR_IOS_UNIVERSAL_LINK_FAILED` | Universal Link resolution failed (iOS) |
| `ERR_IOS_URL_SCHEME_MISSING` | URL scheme not configured (iOS) |

## Acknowledgements

A huge thank you to [shubh73/expo-truecaller](https://github.com/shubh73/expo-truecaller) for providing the base for this module! This library builds heavily upon their excellent foundation, adding crucial support for cryptographic payload signatures for backend profile verification on iOS.

## License

MIT License

