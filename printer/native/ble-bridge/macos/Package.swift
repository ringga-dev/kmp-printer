// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "KmpPrinterBleMacos",
    platforms: [.macOS(.v12)],
    products: [
        .executable(name: "kmp-printer-ble-macos", targets: ["KmpPrinterBleMacos"])
    ],
    targets: [
        .executableTarget(name: "KmpPrinterBleMacos")
    ]
)
