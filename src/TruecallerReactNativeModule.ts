import { requireNativeModule } from "expo";

import type {
  TruecallerAndroidInitOptions,
  TruecallerAndroidResult,
  TruecallerIOSResult,
  TruecallerInitResult,
  TruecallerVerifyOptions,
} from "./TruecallerReactNative.types";

export default requireNativeModule<{
  initializeAsync(
    options?: TruecallerAndroidInitOptions,
  ): Promise<TruecallerInitResult>;
  promptAuthAsync(
    options?: TruecallerVerifyOptions,
  ): Promise<TruecallerAndroidResult | TruecallerIOSResult>;
  requestProfileAsync(): Promise<TruecallerIOSResult>;
  clear(): void;
}>("TruecallerReactNative");
