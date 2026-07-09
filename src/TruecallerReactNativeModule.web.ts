import { registerWebModule, NativeModule } from 'expo';

class TruecallerReactNativeModule extends NativeModule<{}> {}

export default registerWebModule(TruecallerReactNativeModule, 'TruecallerReactNativeModule');
