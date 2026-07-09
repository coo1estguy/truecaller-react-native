/**
 * Consent screen heading text.
 * Maps to `TcSdkOptions.SDK_CONSENT_HEADING_*` on Android.
 * @platform android
 */
export type ConsentHeading =
  | "logInTo"
  | "signUpWith"
  | "signInTo"
  | "verifyNumberWith"
  | "registerWith"
  | "getStartedWith"
  | "proceedWith"
  | "verifyWith"
  | "verifyProfileWith"
  | "verifyYourProfileWith"
  | "verifyPhoneNoWith"
  | "verifyYourNoWith"
  | "continueWith"
  | "completeOrderWith"
  | "placeOrderWith"
  | "completeBookingWith"
  | "checkoutWith"
  | "manageDetailsWith"
  | "manageYourDetailsWith"
  | "loginToWithOneTap"
  | "subscribeTo"
  | "getUpdatesFrom"
  | "continueReadingOn"
  | "getNewUpdatesFrom"
  | "loginSignupWith";

/**
 * CTA button text prefix.
 * Maps to `TcSdkOptions.CTA_TEXT_*` on Android.
 * @platform android
 */
export type CtaTextPrefix =
  | "continue"
  | "proceed"
  | "accept"
  | "confirm"
  | "use"
  | "continueWith"
  | "proceedWith";

/**
 * Login text prefix shown above the CTA button.
 * Maps to `TcSdkOptions.LOGIN_TEXT_PREFIX_*` on Android.
 * @platform android
 */
export type LoginTextPrefix =
  | "toGetStarted"
  | "toContinue"
  | "toPlaceOrder"
  | "toCompleteYourPurchase"
  | "toCheckout"
  | "toCompleteYourBooking"
  | "toProceedWithYourBooking"
  | "toContinueWithYourBooking"
  | "toGetDetails"
  | "toViewMore"
  | "toContinueReading"
  | "toProceed"
  | "forNewUpdates"
  | "toGetUpdates"
  | "toSubscribe"
  | "toSubscribeAndGetUpdates";

/**
 * CTA button shape.
 * Maps to `TcSdkOptions.BUTTON_SHAPE_*` on Android.
 * @platform android
 */
export type ButtonShape = "rounded" | "rectangle";

/**
 * Footer CTA text.
 * Maps to `TcSdkOptions.FOOTER_TYPE_*` on Android.
 *
 * - `"skip"` — "Skip"
 * - `"anotherNumber"` — "Use another mobile number"
 * - `"anotherMethod"` — "Use another method"
 * - `"manually"` — "Enter details manually"
 * - `"later"` — "Later"
 * @platform android
 */
export type FooterType =
  | "skip"
  | "anotherNumber"
  | "anotherMethod"
  | "manually"
  | "later";

/**
 * Consent screen layout mode.
 * Maps to `TcSdkOptions.CONSENT_MODE_*` on Android (SDK 3.2.0+).
 * @platform android
 */
export type ConsentMode = "popup" | "bottomsheet";

/**
 * Consent screen color theme.
 * Maps to `OAuthThemeOptions.*` on Android (SDK 3.2.0+).
 * @platform android
 */
export type OAuthTheme = "dark" | "light";

/**
 * How the consent screen can be dismissed.
 * Maps to `TcSdkOptions.DISMISS_OPTION_*` on Android.
 * @platform android
 */
export type DismissOption = "secondaryCtaBorder" | "crossButton";

/**
 * SDK verification scope option.
 *
 * - `"verifyTcUsersOnly"` — only users with Truecaller installed can verify
 * - `"verifyAllUsers"` — all users can verify (may trigger OTP fallback)
 * @platform android
 */
export type SdkOption = "verifyTcUsersOnly" | "verifyAllUsers";

/**
 * Supported consent screen languages (ISO 639-1 codes).
 * @platform android
 */
export type SupportedLanguage =
  | "en"
  | "hi"
  | "mr"
  | "te"
  | "ml"
  | "ur"
  | "pa"
  | "ta"
  | "bn"
  | "kn"
  | "sw"
  | "ar";

/**
 * Available OAuth scopes for the Android SDK.
 * @platform android
 */
export type OAuthScope =
  | "profile"
  | "phone"
  | "openid"
  | "offline_access"
  | "email"
  | "address";

/**
 * Options for `initializeAsync()` on Android. All fields are optional.
 * @platform android
 */
export type TruecallerAndroidInitOptions = {
  /** Button color as hex string, e.g. `"#4CAF50"`. */
  buttonColor?: string;
  /** Button text color as hex string, e.g. `"#FFFFFF"`. */
  buttonTextColor?: string;
  /** Consent screen layout mode (SDK 3.2.0+). */
  consentMode?: ConsentMode;
  /** Footer CTA text. */
  footerType?: FooterType;
  /** CTA button shape. */
  buttonShape?: ButtonShape;
  /** CTA button text prefix. */
  ctaTextPrefix?: CtaTextPrefix;
  /** Login text prefix shown above the CTA button. */
  loginTextPrefix?: LoginTextPrefix;
  /** Contextual heading on the consent screen. */
  heading?: ConsentHeading;
  /** How the consent screen dismiss UI is shown. */
  dismissOption?: DismissOption;
  /** Verification scope — Truecaller users only, or all users. */
  sdkOption?: SdkOption;
  /** Consent screen language. */
  language?: SupportedLanguage;
  /** Consent screen color theme (SDK 3.2.0+). */
  theme?: OAuthTheme;
};

/**
 * Options for `verifyUserAsync()`.
 * @platform android
 */
export type TruecallerVerifyOptions = {
  /** OAuth scopes to request. Defaults to `["profile", "phone"]`. */
  scopes?: OAuthScope[];
};

/** Result from `initializeAsync()` on both platforms. */
export type TruecallerInitResult = {
  /** Whether the SDK initialized successfully. */
  initialized: boolean;
  /** Whether the Truecaller OAuth/profile flow is available on this device. */
  isUsable: boolean;
};

/**
 * Android success result from `verifyUserAsync()`.
 * Send `authorizationCode` and `codeVerifier` to your backend to exchange
 * for an access token via the Truecaller OAuth token endpoint.
 * @platform android
 */
export type TruecallerAndroidResult = {
  /** Exchange this on your backend for an access token. */
  authorizationCode: string;
  /** Scopes the user granted. */
  scopesGranted: string[];
  /** PKCE code verifier — send alongside the authorization code. */
  codeVerifier: string;
  /** OAuth state for CSRF verification — compare against the state you originally set. */
  state: string;
};

export type TruecallerAuthResultAndroid = TruecallerAndroidResult;

/**
 * iOS success result from `requestProfileAsync()`.
 * Unlike Android, the iOS SDK returns profile data directly.
 * @platform ios
 */
export type TruecallerIOSResult = {
  /** User's first name. */
  firstName: string | null;
  /** User's last name. */
  lastName: string | null;
  /** User's phone number. */
  phoneNumber: string | null;
  /** ISO country code for the phone number. */
  countryCode: string | null;
  /** User's email address. */
  email: string | null;
  /** User's gender. */
  gender: "male" | "female" | null;
  /** URL to the user's Truecaller avatar. */
  avatarUrl: string | null;
  /** User's city. */
  city: string | null;
  /** Whether the user's profile is verified by Truecaller. */
  isVerified: boolean;
  /** Base64 encoded payload for backend verification (available in newer SDKs). */
  payload?: string;
  /** Signature of the payload. */
  signature?: string;
  /** Algorithm used for the signature. */
  signatureAlgorithm?: string;
};

/**
 * iOS success result from `promptAuthAsync()`.
 * Returns only the fields necessary for backend verification.
 * @platform ios
 */
export type TruecallerAuthResultIOS = {
  /** Base64 encoded payload for backend verification. */
  payload: string;
  /** Signature of the payload. */
  signature: string;
  /** Algorithm used for the signature. */
  signatureAlgorithm: string;
};

/**
 * Config plugin options for `app.json` / `app.config.ts`.
 *
 * All fields are optional. Omitting a platform's fields skips its native setup.
 * At least one platform should be configured for the module to be useful.
 */
export type TruecallerPluginConfig = {
  /** Your Truecaller Android OAuth client ID. */
  androidClientId?: string;
  /** Your Truecaller iOS app key (from the Truecaller developer portal). */
  iosAppKey?: string;
  /** Your iOS app link URL for universal links (associated domains). */
  iosAppLink?: string;
};
