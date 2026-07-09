import type {
  TruecallerAuthResultAndroid,
  TruecallerAuthResultIOS,
  TruecallerIOSResult,
  TruecallerInitResult,
} from "./TruecallerReactNative.types";
import TruecallerReactNativeModule from "./TruecallerReactNativeModule";

export async function initializeAsync(): Promise<TruecallerInitResult> {
  return TruecallerReactNativeModule.initializeAsync();
}

export async function promptAuthAsync(): Promise<TruecallerAuthResultAndroid | TruecallerAuthResultIOS> {
  const result = await TruecallerReactNativeModule.requestProfileAsync();
  return {
    payload: result.payload as string,
    signature: result.signature as string,
    signatureAlgorithm: result.signatureAlgorithm as string,
  };
}

export async function requestProfileAsync(): Promise<TruecallerIOSResult> {
  return TruecallerReactNativeModule.requestProfileAsync();
}

export function clear(): void {
  TruecallerReactNativeModule.clear();
}

export * from "./TruecallerReactNative.types";
export * from "./errors";
