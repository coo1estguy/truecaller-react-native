import ExpoModulesCore
import TrueSDK

private final class TruecallerDelegateProxy: NSObject, TCTrueSDKDelegate {
  weak var module: TruecallerReactNativeModule?

  func didReceive(_ profile: TCTrueProfile) {
    module?.handleDidReceive(profile)
  }

  // Also implement the newer response method if available in the SDK
  @objc(didReceiveTrueProfileResponse:)
  func didReceive(_ profileResponse: TCTrueProfileResponse) {
    module?.handleDidReceiveResponse(profileResponse)
  }

  func didFailToReceiveTrueProfileWithError(_ error: TCError) {
    module?.handleDidFailToReceiveTrueProfile(error)
  }
}

public class TruecallerReactNativeModule: Module {
  private var pendingPromise: Promise?
  private var isInitialized = false
  private let truecallerDelegate = TruecallerDelegateProxy()

  public func definition() -> ModuleDefinition {
    Name("TruecallerReactNative")

    AsyncFunction("initializeAsync") { (promise: Promise) in
      let appKey = Bundle.main.object(forInfoDictionaryKey: "TruecallerAppKey") as? String ?? ""
      let appLink = Bundle.main.object(forInfoDictionaryKey: "TruecallerAppLink") as? String ?? ""

      guard !appKey.isEmpty else {
        promise.reject("ERR_INIT_FAILED", "TruecallerAppKey not found in Info.plist. Configure it via the expo-truecaller config plugin.")
        return
      }

      guard !appLink.isEmpty else {
        promise.reject("ERR_INIT_FAILED", "TruecallerAppLink not found in Info.plist. Configure it via the expo-truecaller config plugin.")
        return
      }

      let manager = TCTrueSDK.sharedManager()
      manager.setup(withAppKey: appKey, appLink: appLink)
      self.truecallerDelegate.module = self
      manager.delegate = self.truecallerDelegate
      self.isInitialized = true

      promise.resolve([
        "initialized": true,
        "isUsable": manager.isSupported()
      ])
    }.runOnQueue(.main)

    AsyncFunction("requestProfileAsync") { (promise: Promise) in
      self.startTruecallerAuth(promise)
    }.runOnQueue(.main)

    AsyncFunction("promptAuthAsync") { (promise: Promise) in
      self.startTruecallerAuth(promise)
    }.runOnQueue(.main)

    Function("clear") {
      let p = self.pendingPromise
      self.pendingPromise = nil
      p?.reject("ERR_CLEARED", "SDK was cleared")
      TCTrueSDK.sharedManager().delegate = nil
      self.truecallerDelegate.module = nil
      self.isInitialized = false
    }

    OnDestroy {
      let p = self.pendingPromise
      self.pendingPromise = nil
      p?.reject("ERR_MODULE_DESTROYED", "Module was destroyed")
      TCTrueSDK.sharedManager().delegate = nil
      self.truecallerDelegate.module = nil
      self.isInitialized = false
    }
  }

  private func startTruecallerAuth(_ promise: Promise) {
      guard self.isInitialized else {
        promise.reject("ERR_NOT_INITIALIZED", "Call initializeAsync() first")
        return
      }

      let manager = TCTrueSDK.sharedManager()

      guard manager.isSupported() else {
        promise.reject("ERR_NOT_AVAILABLE", "Truecaller is not installed on this device")
        return
      }

      if self.pendingPromise != nil {
        promise.reject("ERR_ALREADY_IN_PROGRESS", "A requestProfileAsync() call is already in progress. Await the current request first.")
        return
      }

      self.pendingPromise = promise
      manager.requestTrueProfile()
  }

  fileprivate func handleDidReceive(_ profile: TCTrueProfile) {
    let p = pendingPromise
    pendingPromise = nil
    guard let promise = p else { return }

    var gender: String? = nil
    switch profile.gender {
    case .male:  gender = "male"
    case .female: gender = "female"
    default: break
    }

    promise.resolve([
      "firstName": profile.firstName as Any,
      "lastName": profile.lastName as Any,
      "phoneNumber": profile.phoneNumber as Any,
      "countryCode": profile.countryCode as Any,
      "email": profile.email as Any,
      "gender": gender as Any,
      "avatarUrl": profile.avatarURL as Any,
      "city": profile.city as Any,
      "isVerified": profile.isVerified
    ])
  }

  fileprivate func handleDidReceiveResponse(_ profileResponse: TCTrueProfileResponse) {
    let p = pendingPromise
    pendingPromise = nil
    guard let promise = p else { return }

    var firstName: String? = nil
    var lastName: String? = nil
    var phoneNumber: String? = nil
    var countryCode: String? = nil
    var email: String? = nil
    var gender: String? = nil
    var avatarUrl: String? = nil
    var city: String? = nil
    var isVerified = false

    if let payload = profileResponse.payload,
       let decodedData = Data(base64Encoded: payload),
       let json = try? JSONSerialization.jsonObject(with: decodedData, options: []) as? [String: Any] {
        firstName = json["firstName"] as? String
        lastName = json["lastName"] as? String
        phoneNumber = json["phoneNumber"] as? String
        countryCode = json["countryCode"] as? String
        email = json["email"] as? String
        avatarUrl = json["avatarUrl"] as? String
        city = json["city"] as? String
        isVerified = (json["isVerified"] as? Bool) ?? false
        
        if let g = json["gender"] as? String {
            if g.lowercased() == "m" || g.lowercased() == "male" {
                gender = "male"
            } else if g.lowercased() == "f" || g.lowercased() == "female" {
                gender = "female"
            }
        }
    }

    promise.resolve([
      "firstName": firstName as Any,
      "lastName": lastName as Any,
      "phoneNumber": phoneNumber as Any,
      "countryCode": countryCode as Any,
      "email": email as Any,
      "gender": gender as Any,
      "avatarUrl": avatarUrl as Any,
      "city": city as Any,
      "isVerified": isVerified,
      "payload": profileResponse.payload as Any,
      "signature": profileResponse.signature as Any,
      "signatureAlgorithm": profileResponse.signatureAlgorithm as Any
    ])
  }

  fileprivate func handleDidFailToReceiveTrueProfile(_ error: TCError) {
    let p = pendingPromise
    pendingPromise = nil
    p?.reject(mapIOSErrorCode(error.code), error.localizedDescription)
  }

  private func mapIOSErrorCode(_ code: Int) -> String {
    switch code {
    case 1:  return "ERR_IOS_APP_KEY_MISSING"
    case 2:  return "ERR_IOS_APP_LINK_MISSING"
    case 3:  return "ERR_USER_CANCELLED"
    case 4:  return "ERR_IOS_USER_NOT_SIGNED_IN"
    case 5:  return "ERR_SDK_TOO_OLD"
    case 6:  return "ERR_SDK_TOO_OLD"             // TruecallerTooOld
    case 7:  return "ERR_SDK_TOO_OLD"             // OSNotSupported
    case 8:  return "ERR_NOT_INSTALLED"
    case 9:  return "ERR_NETWORK_FAILURE"
    case 10: return "ERR_SDK_ERROR"
    case 11: return "ERR_SDK_ERROR"               // UnauthorizedUser
    case 12: return "ERR_IOS_UNAUTHORIZED_DEVELOPER"
    case 13: return "ERR_SDK_ERROR"               // UserProfileContentNotValid
    case 14: return "ERR_SDK_ERROR"               // BadRequest
    case 15: return "ERR_VERIFICATION_FAILED"
    case 16: return "ERR_SDK_ERROR"               // RequestNonceMismatch
    case 17: return "ERR_SDK_ERROR"               // ViewDelegateNil
    case 18: return "ERR_SDK_ERROR"               // InvalidName
    case 19: return "ERR_IOS_UNIVERSAL_LINK_FAILED"
    case 20: return "ERR_IOS_URL_SCHEME_MISSING"
    default: return "ERR_UNKNOWN_ERROR"
    }
  }
}
