package expo.modules.truecaller

import expo.modules.kotlin.exception.CodedException

internal class NotInitializedException :
    CodedException("ERR_NOT_INITIALIZED", "Call initializeAsync() before using this method", null)

internal class NoActivityException :
    CodedException("ERR_NO_ACTIVITY", "No foreground activity available", null)

internal class ActivityDestroyedException :
    CodedException("ERR_ACTIVITY_DESTROYED", "Activity is finishing or destroyed", null)

internal class LauncherNotRegisteredException :
    CodedException("ERR_LAUNCHER_NOT_REGISTERED", "ActivityResultLauncher not registered — this may indicate a lifecycle issue", null)

internal class NotAvailableException :
    CodedException("ERR_NOT_AVAILABLE", "Truecaller is not installed or user is not logged in", null)

internal class AlreadyInProgressException :
    CodedException("ERR_ALREADY_IN_PROGRESS", "A verification request is already in progress. Await the current request first.", null)

internal class PkceFailedException(
    cause: Throwable? = null,
    message: String = "Failed to generate PKCE code verifier or challenge"
) : CodedException("ERR_PKCE_FAILED", message, cause)

internal class InitFailedException(
    cause: Throwable? = null,
    message: String = "Failed to initialize Truecaller SDK"
) : CodedException("ERR_INIT_FAILED", message, cause)

internal class ResultErrorException(
    cause: Throwable? = null,
    message: String = "Error handling activity result"
) : CodedException("ERR_RESULT_ERROR", message, cause)

internal class VerificationFailedException(
    cause: Throwable? = null,
    message: String = "Failed to start verification flow"
) : CodedException("ERR_VERIFICATION_FAILED", message, cause)

internal class ClearedException :
    CodedException("ERR_CLEARED", "SDK was cleared while a request was pending", null)

internal class VerificationRequiredException :
    CodedException("ERR_VERIFICATION_REQUIRED", "Additional verification required", null)

internal class MissingClientIdException :
    CodedException("ERR_MISSING_CLIENT_ID", "Invalid partner credentials or client ID is missing", null)

internal class SdkErrorException :
    CodedException("ERR_SDK_ERROR", "Internal Truecaller SDK error", null)

internal class SdkTooOldException :
    CodedException("ERR_SDK_TOO_OLD", "Truecaller SDK or device is not compatible", null)

internal class UserCancelledException :
    CodedException("ERR_USER_CANCELLED", "User dismissed the consent screen", null)

internal class UserDismissedException :
    CodedException("ERR_USER_DISMISSED", "User dismissed while loading", null)

internal class UserPressedBackException :
    CodedException("ERR_USER_PRESSED_BACK", "User pressed the footer button to dismiss", null)

internal class NotInstalledException :
    CodedException("ERR_NOT_INSTALLED", "Truecaller app is not installed", null)

internal class UnknownErrorException(
    cause: Throwable? = null,
    message: String = "An unknown Truecaller error occurred"
) : CodedException("ERR_UNKNOWN_ERROR", message, cause)

internal class ModuleDestroyedException :
    CodedException("ERR_MODULE_DESTROYED", "Module was destroyed before result arrived", null)
