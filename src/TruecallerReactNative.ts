// Fallback entry resolved when neither .android nor .ios applies (for example, web).

import type {
  TruecallerAndroidInitOptions,
  TruecallerAuthResultAndroid,
  TruecallerAuthResultIOS,
  TruecallerIOSResult,
  TruecallerInitResult,
  TruecallerVerifyOptions,
} from "./TruecallerReactNative.types";

export * from "./TruecallerReactNative.types";
export * from "./errors";

export async function initializeAsync(
  _options?: TruecallerAndroidInitOptions,
): Promise<TruecallerInitResult> {
  throw new Error("truecaller-react-native is not available on this platform");
}

export async function promptAuthAsync(
  _options?: TruecallerVerifyOptions,
): Promise<TruecallerAuthResultAndroid | TruecallerAuthResultIOS> {
  throw new Error("truecaller-react-native is not available on this platform");
}

export async function requestProfileAsync(): Promise<TruecallerIOSResult> {
  throw new Error("truecaller-react-native is not available on this platform");
}

export function clear(): void {
  // no-op on unsupported platforms
}
