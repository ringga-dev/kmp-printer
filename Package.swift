// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "KmpPrinter",
    platforms: [
        .iOS(.v14),
        .macOS(.v12)
    ],
    products: [
        .library(
            name: "KmpPrinter",
            targets: ["KmpPrinter"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "KmpPrinter",
            path: "./printer/build/XCFrameworks/release/KmpPrinter.xcframework"
        )
    ]
)
