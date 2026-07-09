package expo.modules.truecaller

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

/**
 * Singleton bridge between [TruecallerActivityLifecycleListener] (which registers
 * the launcher during Activity.onCreate) and [TruecallerReactNativeModule] (which uses it
 * during verifyUser).
 */
object TruecallerLauncherHolder {
    @Volatile var launcher: ActivityResultLauncher<Intent>? = null
    @Volatile var resultErrorCallback: ((Exception) -> Unit)? = null
}
