import AppKit

let root = URL(fileURLWithPath: CommandLine.arguments[1])
let source = root.appendingPathComponent("app/src/main/res/mipmap-xxxhdpi/app_icon.webp")
guard let image = NSImage(contentsOf: source),
      let bitmap = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 1024, pixelsHigh: 1024,
                                    bitsPerSample: 8, samplesPerPixel: 3, hasAlpha: false,
                                    isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0),
      let context = NSGraphicsContext(bitmapImageRep: bitmap) else {
    fatalError("Cannot load the existing Android app icon.")
}
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = context
context.imageInterpolation = .high
NSColor.white.setFill()
NSRect(x: 0, y: 0, width: 1024, height: 1024).fill()
image.draw(in: NSRect(x: 0, y: 0, width: 1024, height: 1024))
NSGraphicsContext.restoreGraphicsState()
guard let png = bitmap.representation(using: .png, properties: [:]) else { fatalError("Cannot encode app icon.") }
let destination = root.appendingPathComponent("iosApp/Apuntes/Assets.xcassets/AppIcon.appiconset/AppIcon.png")
try png.write(to: destination, options: .atomic)
