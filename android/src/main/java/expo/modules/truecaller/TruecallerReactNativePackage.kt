package expo.modules.truecaller

import android.content.Context
import expo.modules.core.interfaces.Package
import expo.modules.core.interfaces.ReactActivityLifecycleListener

/** Auto-discovered by Expo autolinking. Registers the ActivityResultLauncher early in the Activity lifecycle. */
class TruecallerReactNativePackage : Package {
    override fun createReactActivityLifecycleListeners(
        activityContext: Context
    ): List<ReactActivityLifecycleListener> {
        return listOf(TruecallerActivityLifecycleListener())
    }
}
