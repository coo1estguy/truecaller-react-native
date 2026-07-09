import type {
  TruecallerAndroidInitOptions,
  TruecallerAuthResultAndroid,
  TruecallerAuthResultIOS,
  TruecallerIOSResult,
  TruecallerInitResult,
  TruecallerVerifyOptions,
} from "./TruecallerReactNative.types";
import TruecallerReactNativeModule from "./TruecallerReactNativeModule";

export async function initializeAsync(
  options: TruecallerAndroidInitOptions = {},
): Promise<TruecallerInitResult> {
  return TruecallerReactNativeModule.initializeAsync(options);
}

export async function promptAuthAsync(
  options: TruecallerVerifyOptions = {},
): Promise<TruecallerAuthResultAndroid | TruecallerAuthResultIOS> {
  return TruecallerReactNativeModule.promptAuthAsync(options) as unknown as Promise<TruecallerAuthResultAndroid>;
}

export async function requestProfileAsync(): Promise<TruecallerIOSResult> {
  throw new Error("requestProfileAsync is an iOS only method. Use promptAuthAsync on Android.");
}

export function clear(): void {
  TruecallerReactNativeModule.clear();
}

export * from "./TruecallerReactNative.types";
export * from "./errors";
