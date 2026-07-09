package expo.modules.truecaller

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.truecaller.android.sdk.oAuth.TcSdk
import expo.modules.core.interfaces.ReactActivityLifecycleListener

/**
 * Registers an AndroidX ActivityResultLauncher during Activity.onCreate.
 *
 * Truecaller SDK 3.2.0+ requires this launcher (replacing deprecated onActivityResult).
 * Must be registered before the Activity reaches STARTED state.
 */
class TruecallerActivityLifecycleListener : ReactActivityLifecycleListener {

    companion object {
        private const val TAG = "ExpoTCLifecycle"
        private const val LAUNCHER_KEY = "expo_truecaller_oauth_launcher"
    }

    override fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is FragmentActivity) return

        try {
            TruecallerLauncherHolder.launcher?.let {
                try { it.unregister() } catch (_: Exception) {}
            }

            TruecallerLauncherHolder.launcher = activity.activityResultRegistry.register(
                LAUNCHER_KEY,
                ActivityResultContracts.StartActivityForResult()
            ) { result: ActivityResult ->
                handleResult(activity, result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register ActivityResultLauncher", e)
        }
    }

    override fun onDestroy(activity: Activity) {
        try {
            TruecallerLauncherHolder.launcher?.unregister()
            TruecallerLauncherHolder.launcher = null
            TruecallerLauncherHolder.resultErrorCallback = null
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering launcher", e)
        }
    }

    private fun handleResult(activity: FragmentActivity, result: ActivityResult) {
        try {
            TcSdk.getInstance().onActivityResultObtained(activity, result.resultCode, result.data)
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding result to TcSdk", e)
            TruecallerLauncherHolder.resultErrorCallback?.invoke(e)
        }
    }
}
