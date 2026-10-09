import SwiftUI

@main
struct ApuntesApp: App {
    @StateObject private var store: AppStore
    @StateObject private var ads = AdsManager()

    init() {
        var directory: URL?
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--ui-testing") {
            let testingDirectory = FileManager.default.temporaryDirectory.appendingPathComponent("ApuntesUITests", isDirectory: true)
            if ProcessInfo.processInfo.arguments.contains("--reset-test-state") {
                try? FileManager.default.removeItem(at: testingDirectory)
            }
            directory = testingDirectory
        }
        #endif
        _store = StateObject(wrappedValue: AppStore(directory: directory))
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environmentObject(ads)
                .tint(Color(red: 0.37, green: 0.21, blue: 0.69))
        }
    }
}
