import AppKit

let root = URL(fileURLWithPath: CommandLine.arguments[1])
let source = root.appendingPathComponent("iosApp/build/source-icon.png")
guard let data = try? Data(contentsOf: source),
      let image = NSBitmapImageRep(data: data)?.cgImage else {
    fatalError("Cannot load the decoded Android app icon.")
}
guard let context = CGContext(data: nil, width: 1024, height: 1024, bitsPerComponent: 8,
                              bytesPerRow: 4096, space: CGColorSpaceCreateDeviceRGB(),
                              bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue) else {
    fatalError("Cannot create the opaque icon bitmap.")
}
context.interpolationQuality = .high
let bounds = CGRect(x: 0, y: 0, width: 1024, height: 1024)
context.setFillColor(CGColor(gray: 1, alpha: 1))
context.fill(bounds)
context.draw(image, in: bounds)
guard let output = context.makeImage(),
      let png = NSBitmapImageRep(cgImage: output).representation(using: .png, properties: [:]) else {
    fatalError("Cannot encode app icon.")
}
let destination = root.appendingPathComponent("iosApp/Apuntes/Assets.xcassets/AppIcon.appiconset/AppIcon.png")
try png.write(to: destination, options: .atomic)
