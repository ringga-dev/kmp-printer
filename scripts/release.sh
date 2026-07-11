#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

# Read version from single source of truth
VERSION=$(grep '^LIB_VERSION=' gradle.properties | cut -d= -f2)

if [ -z "$VERSION" ]; then
  echo "❌ Could not read LIB_VERSION from gradle.properties"
  exit 1
fi

echo "🚀 Releasing v$VERSION"
echo ""

# 1. Verify working tree is clean
if [ -n "$(git status --porcelain)" ]; then
  echo "⚠️  Uncommitted changes found. Stashing..."
  git stash --include-untracked
  STASHED=true
else
  STASHED=false
fi

# 2. Sync documentation version
echo "📝 Syncing documentation..."
./gradlew syncDocumentationVersion --no-daemon -q

# 3. Check if files changed after sync
if [ -z "$(git status --porcelain)" ] && [ "$STASHED" = false ]; then
  echo "📄 No documentation changes needed."
else
  git add -A
  git commit -m "chore: sync documentation for v$VERSION" || true
fi

# 4. Restore stash if any
if [ "$STASHED" = true ]; then
  git stash pop || true
fi

echo ""
echo "=== Release Checklist ==="
echo "  Version: $VERSION"
echo "  Tag:     v$VERSION"
echo "  Branch:  $(git branch --show-current)"
echo ""

# 5. Create tag and push
echo "🏷️  Creating tag v$VERSION..."
if git tag -d "v$VERSION" 2>/dev/null; then
  echo "  (replaced existing tag v$VERSION)"
fi
git tag "v$VERSION"

echo ""
echo "✅ Ready to push!"
echo ""
echo "Run this command to release:"
echo "  git push origin $(git branch --show-current) --tags"
echo ""
echo "GitHub Actions will auto:"
echo "  ✅ Test & compile all targets"
echo "  ✅ Build AAR, XCFramework, JAR"
echo "  ✅ Deploy to GitHub Maven"
echo "  ✅ Deploy to Maven Central"
echo "  ✅ Create GitHub Release"
