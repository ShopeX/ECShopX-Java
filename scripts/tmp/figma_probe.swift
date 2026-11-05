import Cocoa
import WebKit

class Delegate: NSObject, WKNavigationDelegate {
    var webView: WKWebView!

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 25) {
            webView.evaluateJavaScript("document.body.innerText") { result, error in
                let text = (result as? String) ?? "ERR: \(String(describing: error))"
                try? text.write(toFile: "/tmp/figma-dom.txt", atomically: true, encoding: .utf8)
                print("DOM chars: \(text.count)")
                let config = WKSnapshotConfiguration()
                config.rect = webView.bounds
                webView.takeSnapshot(with: config) { image, error in
                    if let image = image, let tiff = image.tiffRepresentation,
                       let rep = NSBitmapImageRep(data: tiff),
                       let png = rep.representation(using: .png, properties: [:]) {
                        try? png.write(to: URL(fileURLWithPath: "/tmp/figma-view.png"))
                        print("Wrote screenshot \(png.count) bytes")
                    } else {
                        print("Snapshot error: \(String(describing: error))")
                    }
                    NSApp.terminate(nil)
                }
            }
        }
    }
}

let app = NSApplication.shared
app.setActivationPolicy(.accessory)
let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 2400, height: 1600), styleMask: [.borderless], backing: .buffered, defer: false)
let config = WKWebViewConfiguration()
config.preferences.setValue(true, forKey: "developerExtrasEnabled")
let webView = WKWebView(frame: window.contentView!.bounds, configuration: config)
window.contentView!.addSubview(webView)
window.orderFrontRegardless()
let delegate = Delegate()
delegate.webView = webView
webView.navigationDelegate = delegate
webView.load(URLRequest(url: URL(string: CommandLine.arguments[1])!))
app.run()
