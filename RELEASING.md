# 🚀 Releasing KmpPrinter

This guide covers how to publish KmpPrinter to **GitHub Maven** and **Maven Central**.

---

## 📋 Prerequisites (One-Time Setup)

### 1. Sonatype Account (for Maven Central)

1. Create an account at [https://issues.sonatype.org](https://issues.sonatype.org) (login with GitHub)
2. Create a new Community Support ticket:
   - **Project:** Community Support - Open Source Project Repository Hosting
   - **Summary:** `New OSSRH project for io.github.ringga-dev`
   - **Group Id:** `io.github.ringga-dev`

### 2. Central Portal (new Maven Central)

1. Login to [https://central.sonatype.com/](https://central.sonatype.com/) with GitHub
2. **Verify your namespace** `io.github.ringga-dev`
3. **Generate User Token**: Profile → User Token → Generate
4. Save the token values — use as `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` secrets

### 3. GPG Key (for Artifact Signing)

```bash
# Generate a GPG key (use ringga.dev@gmail.com as email)
gpg --full-generate-key

# List keys to get your KEY_ID
gpg --list-keys --keyid-format LONG

# Upload to keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>

# Export private key (for GitHub Actions)
gpg --armor --export-secret-keys <YOUR_KEY_ID>
```

### 3. GitHub Secrets

Add these secrets to your GitHub repository at:
**Settings → Secrets and variables → Actions → New repository secret**

| Secret | Value |
|---|---|
| `SONATYPE_USERNAME` | Your Sonatype JIRA username |
| `SONATYPE_PASSWORD` | Your Sonatype JIRA password |
| `GPG_SIGNING_KEY` | Output of `gpg --armor --export-secret-keys <KEY_ID>` |
| `GPG_PASSWORD` | The passphrase you used for the GPG key |

---

## 🔄 Release Process

### Automatic Release (Recommended)

Use the release script — it reads the version from `gradle.properties`, syncs docs, and creates the git tag:

```bash
# 1. Update version (single source of truth)
#    Edit gradle.properties → change LIB_VERSION

# 2. Run release script
./scripts/release.sh

# 3. Push to trigger CI
git push origin master --tags
```

**GitHub Actions** then automates everything:
- ✅ Tests & compile all platforms
- ✅ Build Android AAR, iOS XCFramework, JVM Jar
- ✅ CI verifies tag matches `LIB_VERSION` (fails if mismatch)
- ✅ **Deploy to GitHub Maven** (`maven-repo` branch)
- ✅ **Deploy to Maven Central** (Central Portal)
- ✅ Create GitHub Release with artifacts

### Manual Release

If you prefer to do it step by step:

1. Update `LIB_VERSION` in `gradle.properties` (single source of truth):
   ```bash
   # Example: change this in gradle.properties
   LIB_VERSION=2.3.1
   ```

2. Sync documentation version references:
   ```bash
   ./gradlew syncDocumentationVersion
   ```

3. Commit and tag:
   ```bash
   git add -A
   git commit -m "chore: release v2.3.1"
   git tag v2.3.1
   git push origin master --tags
   ```

> ⚠️ **Important:** The git tag (e.g., `v2.3.1`) **must** match `LIB_VERSION` in `gradle.properties`. CI will verify this and fail if they don't match.

### After Release

- Check Maven Central: https://repo1.maven.org/maven2/io/github/ringga-dev/
- It takes ~10-30 minutes for artifacts to appear after Sonatype close & release
- Library will then be available at:
  ```kotlin
  implementation("io.github.ringga-dev:kmp_printer:2.3.0")
  ```
  **No extra repository needed!** Just `mavenCentral()` in your build file.

---

## 🧪 Testing a Release Locally

```bash
# Publish to local Maven repo (validates build + signing)
export GPG_SIGNING_KEY="<key>"
export GPG_PASSWORD="<pass>"
./gradlew :printer:publishAllPublicationsToLocalRepoRepository

# Check output in:
#   printer/build/repo/
```

---

## 📦 Distribution Options

| Where | Coordinates | Repository Needed? |
|---|---|---|
| **GitHub Maven** | `io.github.ringga-dev:kmp_printer:VERSION` | ✅ Custom Maven URL |
| **Maven Central** | `io.github.ringga-dev:kmp_printer:VERSION` | ❌ Just `mavenCentral()` |

---

## 🔧 CI/CD Pipeline

The [publish.yml](.github/workflows/publish.yml) workflow:

1. **check** — Runs tests and cross-compiles
2. **setup** — Reads LIB_VERSION
3. **build-{platform}** — Builds AAR, XCFramework, JAR
4. **deploy-github-maven** — Pushes to `maven-repo` branch + creates Sonatype bundle
5. **deploy-maven-central** — Publishes to Sonatype OSSRH staging
6. **create-release** — Creates GitHub Release with artifacts
