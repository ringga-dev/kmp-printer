# Contributing to KmpPrinter

First off, thank you for considering contributing to KmpPrinter! 🙌

We welcome contributions of all kinds — bug reports, feature requests, documentation improvements, code changes, and platform support additions.

---

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Code Style](#code-style)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Adding Platform Support](#adding-platform-support)
- [Questions?](#questions)

---

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). By participating, you agree to maintain a respectful and inclusive environment for everyone.

---

## Getting Started

1. **Fork** the repository
2. **Clone** your fork:
   ```bash
   git clone https://github.com/ringga-dev/kmp-printer.git
   cd kmp-printer
   ```
3. **Set up environment**:
   - JDK 17+ (Temurin recommended)
   - Android SDK (for Android target compilation)
   - Xcode (for iOS, macOS only)
4. **Open in IntelliJ IDEA** with Kotlin Multiplatform plugin installed

---

## How to Contribute

### 🐛 Reporting Bugs

- Open a [GitHub Issue](https://github.com/ringga-dev/kmp-printer/issues/new)
- Include:
  - Platform (Android/iOS/Desktop/Web)
  - Printer model and connection type
  - Minimal reproduction code or steps
  - Full error message / log output

### 💡 Feature Requests

- Check existing issues first (someone might already be working on it)
- Describe the use case clearly
- If proposing a new transport (e.g., Wi-Fi Direct, NFC), include platform constraints

### 📖 Documentation

- Fix typos, improve clarity, add examples
- Documentation lives in `README.md` and `docs/` folder
- For API changes, update both the KDoc and markdown docs

### 🧪 Testing

- Help test on real printer hardware
- Add unit tests in `commonTest` or `jvmTest`
- Report compatibility with new printer models

---

## Development Setup

### Prerequisites

- **JDK 17** (required for Kotlin Multiplatform compilation)
- **Gradle 8.x** (wrappers provided)
- **Android SDK** — set `ANDROID_HOME` or let Android Studio manage it
- **Xcode** (iOS target — macOS only)

### Verify Setup

```bash
./gradlew build
```

Expected: all targets compile successfully. Full build may take 10–20 minutes on first run.

### Common Issues

| Issue | Solution |
|---|---|
| `Android SDK not found` | Install Android SDK or set `ANDROID_HOME` in `local.properties` |
| `Could not resolve dependencies` | Check `gradle/libs.versions.toml` for version alignment |
| `Kotlin compile error in expect/actual` | Ensure all source sets have corresponding actual declarations |

---

## Code Style

- Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **`kotlin.code.style=official`** (already set in `gradle.properties`)
- Run before committing:
  ```bash
  ./gradlew ktlintCheck  # if ktlint is configured
  ```
- Keep `commonMain` code **platform-independent** — use `expect`/`actual` for platform APIs
- Use descriptive names over comments — prefer `isPaperOut` over `status == 0x08`

### Package Structure

```
ngga.ring.printer
├── model/         # Data classes, enums, configs
├── manager/       # Connection, permission, diagnostics
├── usecase/       # Print, discover, diagnose use cases
├── repository/    # Data persistence interfaces
└── util/          # ESC/POS commands, encoding, rendering
```

---

## Testing

- **Unit tests** go in `commonTest` or `jvmTest`
- **Integration tests** with real hardware go in `jvmTest` (marked with `@Tag("HardwareIntegrationTest")`)
- Run tests:
  ```bash
  # JVM unit tests (no hardware needed)
  ./gradlew :printer:jvmTest

  # Hardware integration tests (printer required)
  ./gradlew :printer:jvmTest --tests "*JvmPrinterHardwareIntegrationTest"
  ```

### Writing Good Tests

- Cover: config parsing, command building, connection state machine
- Mock transports using `VirtualPrinterConnector`
- For platform-specific tests, use `@RequiresPlatformApi` annotations

---

## Pull Request Process

1. **Branch from `main`** — use a descriptive branch name:
   ```
   feature/ble-reconnection
   fix/usb-timeout
   docs/improve-readme
   ```

2. **Keep PRs focused** — one feature/fix per PR

3. **Include tests** — new code should have corresponding tests

4. **Update docs** — if you change public API, update:
   - KDoc comments in source
   - Relevant markdown files in `docs/`
   - Migration guide if breaking changes

5. **Ensure CI passes** — all checks must be green before merge

6. **Review process**:
   - At least one maintainer review required
   - Address feedback with additional commits (no force-push)
   - Squash commits on merge

### PR Checklist

```markdown
- [ ] Code follows project style guidelines
- [ ] Tests added/updated and passing
- [ ] Documentation updated (KDoc + markdown)
- [ ] CHANGELOG updated (if applicable)
- [ ] `LIB_VERSION` bumped for release PRs
- [ ] All targets compile (`./gradlew build`)
```

---

## Adding Platform Support

To add a new platform target:

1. Create source set directories (`src/<newPlatform>Main/kotlin/...`)
2. Implement `expect` declarations from `commonMain`:
   - `PrinterConnectorFactory`
   - `PrinterPermissionManager`
   - `PrinterPlatformDiagnostics`
   - `ESCPosRenderer`
3. Register the target in `printer/build.gradle.kts`
4. Update [Platform Support](README.md#platform-support) table
5. Add platform-specific connector implementation
6. Test with a real or virtual printer

---

## Release Process (Maintainers)

1. Update `LIB_VERSION` in `gradle.properties`
2. Run `./gradlew syncDocumentationVersion`
3. Commit + tag: `git tag v{version}` && `git push --tags`
4. GitHub Actions builds and publishes automatically to:
   - Maven repository (`maven-repo` branch)
   - GitHub Release with AAR, XCFramework, JAR
   - GitHub Pages documentation (if docs changed)

---

## Questions?

- Open a [Discussion](https://github.com/ringga-dev/kmp-printer/discussions)
- Join our Telegram group (link in repository about section)
- Or just [open an issue](https://github.com/ringga-dev/kmp-printer/issues/new)

---

**Happy printing!** 🖨️✨
