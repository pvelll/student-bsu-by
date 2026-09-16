import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Compose draws edge to edge and applies the safe area insets itself
        // (status bar, home indicator and keyboard), exactly like on Android.
        // Constraining it to the SwiftUI safe area produced black bars at the
        // top and bottom of the screen.
        ComposeView()
            .ignoresSafeArea(.all, edges: .all)
    }
}
