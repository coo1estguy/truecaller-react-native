package expo.modules.truecaller

import android.graphics.Color
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.truecaller.android.sdk.oAuth.CodeVerifierUtil
import com.truecaller.android.sdk.oAuth.OAuthThemeOptions
import com.truecaller.android.sdk.oAuth.TcOAuthCallback
import com.truecaller.android.sdk.oAuth.TcOAuthData
import com.truecaller.android.sdk.oAuth.TcOAuthError
import com.truecaller.android.sdk.oAuth.TcSdk
import com.truecaller.android.sdk.oAuth.TcSdkOptions
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import java.math.BigInteger
import java.security.SecureRandom
import java.util.Locale

class InitOptions : Record {
    @Field val buttonColor: String? = null
    @Field val buttonTextColor: String? = null
    @Field val consentMode: String? = null
    @Field val footerType: String? = null
    @Field val buttonShape: String? = null
    @Field val ctaTextPrefix: String? = null
    @Field val loginTextPrefix: String? = null
    @Field val heading: String? = null
    @Field val dismissOption: String? = null
    @Field val sdkOption: String? = null
    @Field val language: String? = null
    @Field val theme: String? = null
}

class VerifyOptions : Record {
    @Field val scopes: List<String>? = null
}

private val DEFAULT_SCOPES = arrayOf("profile", "phone")

class TruecallerReactNativeModule : Module() {

    companion object {
        private const val TAG = "TruecallerReactNative"
        private val secureRandom = SecureRandom()
    }

    @Volatile private var pendingPromise: Promise? = null
    @Volatile private var isInitialized = false
    @Volatile private var codeVerifier: String? = null
    @Volatile private var themeOption: String? = null

    private val oauthCallback = createOAuthCallback()

    override fun definition() = ModuleDefinition {

        Name("TruecallerReactNative")

        AsyncFunction("initializeAsync") { options: InitOptions, promise: Promise ->
            try {
                val activity = requireActivity()
                val tcSdkOptions = buildSdkOptions(activity, oauthCallback, options)
                TcSdk.init(tcSdkOptions)
                isInitialized = true

                options.language?.let { lang ->
                    try { TcSdk.getInstance().setLocale(Locale(lang)) }
                    catch (e: Exception) { Log.e(TAG, "Error setting locale", e) }
                }

                themeOption = options.theme

                val isUsable = try { TcSdk.getInstance().isOAuthFlowUsable }
                    catch (_: Exception) { false }

                promise.resolve(bundleOf("initialized" to true, "isUsable" to isUsable))
            } catch (e: Exception) {
                throw InitFailedException(cause = e)
            }
        }

        AsyncFunction("promptAuthAsync") { options: VerifyOptions, promise: Promise ->
            if (!isInitialized) throw NotInitializedException()

            val activity = requireActivity()
            val launcher = TruecallerLauncherHolder.launcher ?: throw LauncherNotRegisteredException()

            if (!TcSdk.getInstance().isOAuthFlowUsable) throw NotAvailableException()
            if (pendingPromise != null) throw AlreadyInProgressException()

            pendingPromise = promise

            TruecallerLauncherHolder.resultErrorCallback = callback@{ e ->
                val p = pendingPromise ?: return@callback
                pendingPromise = null
                TruecallerLauncherHolder.resultErrorCallback = null
                p.reject(ResultErrorException(cause = e))
            }

            activity.runOnUiThread {
                try {
                    val tcSdk = TcSdk.getInstance()

                    tcSdk.setOAuthState(BigInteger(130, secureRandom).toString(32))

                    val scopes = options.scopes
                        ?.toTypedArray()
                        ?.takeIf { it.isNotEmpty() }
                        ?: DEFAULT_SCOPES
                    tcSdk.setOAuthScopes(scopes)

                    val verifier = CodeVerifierUtil.generateRandomCodeVerifier()
                        ?: run {
                            settleWithError(PkceFailedException())
                            return@runOnUiThread
                        }
                    codeVerifier = verifier

                    val challenge = CodeVerifierUtil.getCodeChallenge(verifier)
                        ?: run {
                            settleWithError(PkceFailedException())
                            return@runOnUiThread
                        }
                    tcSdk.setCodeChallenge(challenge)

                    // Theme must be set immediately before getAuthorizationCode
                    when (themeOption) {
                        "dark" -> tcSdk.setTheme(OAuthThemeOptions.DARK)
                        "light" -> tcSdk.setTheme(OAuthThemeOptions.LIGHT)
                    }

                    tcSdk.getAuthorizationCode(activity, launcher)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting verification flow", e)
                    settleWithError(VerificationFailedException(cause = e))
                }
            }
        }

        Function("clear") {
            cleanup(ClearedException())
        }

        OnDestroy {
            cleanup(ModuleDestroyedException())
        }
    }

    private fun requireActivity(): FragmentActivity {
        val activity = appContext.currentActivity as? FragmentActivity
            ?: throw NoActivityException()
        if (activity.isFinishing || activity.isDestroyed) throw ActivityDestroyedException()
        return activity
    }

    private fun settleWithError(error: CodedException) {
        val p = pendingPromise ?: return
        pendingPromise = null
        TruecallerLauncherHolder.resultErrorCallback = null
        p.reject(error)
    }

    private fun cleanup(error: CodedException) {
        try {
            if (isInitialized) {
                TcSdk.clear()
                isInitialized = false
            }
            settleWithError(error)
            codeVerifier = null
            themeOption = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    private fun createOAuthCallback(): TcOAuthCallback {
        return object : TcOAuthCallback {
            override fun onSuccess(tcOAuthData: TcOAuthData) {
                val p = pendingPromise ?: return
                val verifier = codeVerifier
                if (verifier == null) {
                    pendingPromise = null
                    TruecallerLauncherHolder.resultErrorCallback = null
                    p.reject(PkceFailedException())
                    return
                }
                pendingPromise = null
                TruecallerLauncherHolder.resultErrorCallback = null
                p.resolve(
                    bundleOf(
                        "authorizationCode" to tcOAuthData.authorizationCode,
                        "scopesGranted" to ArrayList(tcOAuthData.scopesGranted),
                        "codeVerifier" to verifier,
                        "state" to (tcOAuthData.state ?: "")
                    )
                )
            }

            override fun onFailure(tcOAuthError: TcOAuthError) {
                val p = pendingPromise ?: return
                pendingPromise = null
                TruecallerLauncherHolder.resultErrorCallback = null
                p.reject(mapErrorCode(tcOAuthError.errorCode))
            }

            override fun onVerificationRequired(tcOAuthError: TcOAuthError?) {
                val p = pendingPromise ?: return
                pendingPromise = null
                TruecallerLauncherHolder.resultErrorCallback = null
                p.reject(VerificationRequiredException())
            }
        }
    }

    // Verified against truecaller-sdk:3.2.1 bytecode
    private fun mapErrorCode(errorCode: Int): CodedException = when (errorCode) {
        0 -> SdkErrorException()                // DefaultError
        2 -> UserCancelledException()           // UserDeniedError
        5 -> SdkErrorException()                // TruecallerClosedError (app closed unexpectedly)
        6 -> SdkTooOldException()               // OldSdkError
        7 -> SdkErrorException()                // RequestCodeCollisionError
        10 -> SdkErrorException()               // InvalidAccountStateError
        11 -> NotInstalledException()           // TruecallerNotInstalledError
        12 -> MissingClientIdException()        // InvalidPartnerError
        13 -> UserDismissedException()          // UserDeniedWhileLoadingError
        14 -> UserPressedBackException()        // UserDeniedByPressingFooterError
        15 -> SdkErrorException()               // TruecallerActivityNotFoundError
        16 -> SdkTooOldException()              // DeviceNotSupported
        else -> UnknownErrorException()
    }

    private fun buildSdkOptions(
        activity: FragmentActivity,
        callback: TcOAuthCallback,
        options: InitOptions
    ): TcSdkOptions {
        val builder = TcSdkOptions.Builder(activity, callback)

        options.consentMode?.let { mode ->
            when (mode) {
                "popup" -> builder.consentMode(TcSdkOptions.CONSENT_MODE_POPUP)
                "bottomsheet" -> builder.consentMode(TcSdkOptions.CONSENT_MODE_BOTTOMSHEET)
            }
        }

        options.buttonColor?.let { color ->
            try { builder.buttonColor(Color.parseColor(color)) }
            catch (e: Exception) { Log.e(TAG, "Invalid buttonColor: $color", e) }
        }

        options.buttonTextColor?.let { color ->
            try { builder.buttonTextColor(Color.parseColor(color)) }
            catch (e: Exception) { Log.e(TAG, "Invalid buttonTextColor: $color", e) }
        }

        options.buttonShape?.let { shape ->
            when (shape) {
                "rounded" -> builder.buttonShapeOptions(TcSdkOptions.BUTTON_SHAPE_ROUNDED)
                "rectangle" -> builder.buttonShapeOptions(TcSdkOptions.BUTTON_SHAPE_RECTANGLE)
            }
        }

        options.ctaTextPrefix?.let { cta ->
            when (cta) {
                "continue" -> builder.ctaText(TcSdkOptions.CTA_TEXT_CONTINUE)
                "proceed" -> builder.ctaText(TcSdkOptions.CTA_TEXT_PROCEED)
                "accept" -> builder.ctaText(TcSdkOptions.CTA_TEXT_ACCEPT)
                "confirm" -> builder.ctaText(TcSdkOptions.CTA_TEXT_CONFIRM)
                "use" -> builder.ctaText(TcSdkOptions.CTA_TEXT_USE)
                "continueWith" -> builder.ctaText(TcSdkOptions.CTA_TEXT_CONTINUE_WITH)
                "proceedWith" -> builder.ctaText(TcSdkOptions.CTA_TEXT_PROCEED_WITH)
            }
        }

        options.loginTextPrefix?.let { prefix ->
            when (prefix) {
                "toGetStarted" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_GET_STARTED)
                "toContinue" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_CONTINUE)
                "toPlaceOrder" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_PLACE_ORDER)
                "toCompleteYourPurchase" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_COMPLETE_YOUR_PURCHASE)
                "toCheckout" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_CHECKOUT)
                "toCompleteYourBooking" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_COMPLETE_YOUR_BOOKING)
                "toProceedWithYourBooking" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_PROCEED_WITH_YOUR_BOOKING)
                "toContinueWithYourBooking" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_CONTINUE_WITH_YOUR_BOOKING)
                "toGetDetails" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_GET_DETAILS)
                "toViewMore" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_VIEW_MORE)
                "toContinueReading" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_CONTINUE_READING)
                "toProceed" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_PROCEED)
                "forNewUpdates" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_FOR_NEW_UPDATES)
                "toGetUpdates" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_GET_UPDATES)
                "toSubscribe" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_SUBSCRIBE)
                "toSubscribeAndGetUpdates" -> builder.loginTextPrefix(TcSdkOptions.LOGIN_TEXT_PREFIX_TO_SUBSCRIBE_AND_GET_UPDATES)
            }
        }

        options.dismissOption?.let { dismiss ->
            when (dismiss) {
                "secondaryCtaBorder" -> builder.dismissOptions(TcSdkOptions.DISMISS_OPTION_SECONDARY_CTA_BORDER)
                "crossButton" -> builder.dismissOptions(TcSdkOptions.DISMISS_OPTION_CROSS_BUTTON)
            }
        }

        options.footerType?.let { footer ->
            when (footer) {
                "skip" -> builder.footerType(TcSdkOptions.FOOTER_TYPE_SKIP)
                "anotherNumber" -> builder.footerType(TcSdkOptions.FOOTER_TYPE_ANOTHER_MOBILE_NO)
                "anotherMethod" -> builder.footerType(TcSdkOptions.FOOTER_TYPE_ANOTHER_METHOD)
                "manually" -> builder.footerType(TcSdkOptions.FOOTER_TYPE_MANUALLY)
                "later" -> builder.footerType(TcSdkOptions.FOOTER_TYPE_LATER)
            }
        }

        options.heading?.let { heading ->
            when (heading) {
                "logInTo" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_LOG_IN_TO)
                "signUpWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_SIGN_UP_WITH)
                "signInTo" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_SIGN_IN_TO)
                "verifyNumberWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_VERIFY_NUMBER_WITH)
                "registerWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_REGISTER_WITH)
                "getStartedWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_GET_STARTED_WITH)
                "proceedWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_PROCEED_WITH)
                "verifyWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_VERIFY_WITH)
                "verifyProfileWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_VERIFY_PROFILE_WITH)
                "verifyYourProfileWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_VERIFY_YOUR_PROFILE_WITH)
                "verifyPhoneNoWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_VERIFY_PHONE_NO_WITH)
                "verifyYourNoWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_VERIFY_YOUR_NO_WITH)
                "continueWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_CONTINUE_WITH)
                "completeOrderWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_COMPLETE_ORDER_WITH)
                "placeOrderWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_PLACE_ORDER_WITH)
                "completeBookingWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_COMPLETE_BOOKING_WITH)
                "checkoutWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_CHECKOUT_WITH)
                "manageDetailsWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_MANAGE_DETAILS_WITH)
                "manageYourDetailsWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_MANAGE_YOUR_DETAILS_WITH)
                "loginToWithOneTap" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_LOGIN_TO_WITH_ONE_TAP)
                "subscribeTo" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_SUBSCRIBE_TO)
                "getUpdatesFrom" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_GET_UPDATES_FROM)
                "continueReadingOn" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_CONTINUE_READING_ON)
                "getNewUpdatesFrom" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_GET_NEW_UPDATES_FROM)
                "loginSignupWith" -> builder.consentTitleOption(TcSdkOptions.SDK_CONSENT_HEADING_LOGIN_SIGNUP_WITH)
            }
        }

        options.sdkOption?.let { opt ->
            when (opt) {
                "verifyTcUsersOnly" -> builder.sdkOptions(TcSdkOptions.OPTION_VERIFY_ONLY_TC_USERS)
                "verifyAllUsers" -> builder.sdkOptions(TcSdkOptions.OPTION_VERIFY_ALL_USERS)
            }
        }

        return builder.build()
    }
}
