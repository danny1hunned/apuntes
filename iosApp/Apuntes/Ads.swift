import SwiftUI
import GoogleMobileAds
import UserMessagingPlatform

@MainActor
final class AdsManager: ObservableObject {
    @Published private(set) var canShowAds = false
    @Published private(set) var privacyOptionsRequired = false
    @Published var privacyError: String?
    private var requested = false

    func start() async {
        guard !requested else { return }
        requested = true
        #if DEBUG
        guard !ProcessInfo.processInfo.arguments.contains("--ui-testing") else { return }
        #else
        guard let id = Bundle.main.object(forInfoDictionaryKey: "AdMobBannerID") as? String,
              id.hasPrefix("ca-app-pub-"), !id.contains("3940256099942544") else { return }
        #endif
        do {
            try await ConsentInformation.shared.requestConsentInfoUpdate(with: RequestParameters())
            try await ConsentForm.loadAndPresentIfRequired(from: nil)
        } catch {
            // The SDK may still permit ads using consent from an earlier session.
        }
        privacyOptionsRequired = ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        if ConsentInformation.shared.canRequestAds {
            await MobileAds.shared.start()
            canShowAds = true
        }
    }

    func showPrivacyOptions() async {
        canShowAds = false
        do { try await ConsentForm.presentPrivacyOptionsForm(from: nil) }
        catch { privacyError = error.localizedDescription }
        privacyOptionsRequired = ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        canShowAds = ConsentInformation.shared.canRequestAds
    }
}

struct AdBanner: UIViewRepresentable {
    func makeUIView(context: Context) -> BannerView {
        let banner = BannerView(adSize: AdSizeBanner)
        #if DEBUG
        banner.adUnitID = "ca-app-pub-3940256099942544/2435281174"
        #else
        banner.adUnitID = Bundle.main.object(forInfoDictionaryKey: "AdMobBannerID") as? String
        #endif
        banner.load(Request())
        return banner
    }
    func updateUIView(_ uiView: BannerView, context: Context) {}
}
