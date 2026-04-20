#!/bin/bash

# Improved SemVer Bumper for SovereignDroid
# Usage: ./scripts/bump-version.sh <major|minor|patch|beta|stable>

set -e

GRADLE_FILE="app/build.gradle.kts"

# Extract current version details
CURRENT_VERSION=$(grep 'versionName = ' "$GRADLE_FILE" | sed 's/.*"\(.*\)".*/\1/')
CURRENT_CODE=$(grep 'versionCode = ' "$GRADLE_FILE" | grep -oE '[0-9]+')

# Parse version: MAJOR.MINOR.PATCH[-PRERELEASE.BUILD]
if [[ "$CURRENT_VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(-([a-zA-Z]+)\.([0-9]+))?$ ]]; then
    MAJOR="${BASH_REMATCH[1]}"
    MINOR="${BASH_REMATCH[2]}"
    PATCH="${BASH_REMATCH[3]}"
    PRE_TAG="${BASH_REMATCH[5]}"    # e.g. "beta" or ""
    PRE_NUM="${BASH_REMATCH[6]}"    # e.g. "1" or ""
else
    echo "❌ Cannot parse current version '$CURRENT_VERSION' as SemVer."
    exit 1
fi

BUMP_TYPE=$1
NEW_MAJOR=$MAJOR
NEW_MINOR=$MINOR
NEW_PATCH=$PATCH
NEW_PRE_TAG=$PRE_TAG
NEW_PRE_NUM=$PRE_NUM

case "$BUMP_TYPE" in
    major)
        NEW_MAJOR=$((MAJOR + 1))
        NEW_MINOR=0
        NEW_PATCH=0
        NEW_PRE_TAG=""
        NEW_PRE_NUM=""
        ;;
    minor)
        NEW_MINOR=$((MINOR + 1))
        NEW_PATCH=0
        NEW_PRE_TAG=""
        NEW_PRE_NUM=""
        ;;
    patch)
        NEW_PATCH=$((PATCH + 1))
        NEW_PRE_TAG=""
        NEW_PRE_NUM=""
        ;;
    beta)
        # If already in a pre-release, increment the pre-release number
        if [ -n "$PRE_TAG" ]; then
            NEW_PRE_NUM=$((PRE_NUM + 1))
        else
            # If not in pre-release, bump patch and start beta.1 (standard SemVer)
            NEW_PATCH=$((PATCH + 1))
            NEW_PRE_TAG="beta"
            NEW_PRE_NUM=1
        fi
        ;;
    stable)
        # Remove pre-release tags to make it stable
        if [ -n "$PRE_TAG" ]; then
            NEW_PRE_TAG=""
            NEW_PRE_NUM=""
        else
            echo "ℹ️ Version is already stable."
            exit 0
        fi
        ;;
    *)
        echo "Usage: $0 <major|minor|patch|beta|stable>"
        echo "  major:  X.Y.Z -> (X+1).0.0"
        echo "  minor:  X.Y.Z -> X.(Y+1).0"
        echo "  patch:  X.Y.Z -> X.Y.(Z+1)"
        echo "  beta:   X.Y.Z -> X.Y.(Z+1)-beta.1  OR  X.Y.Z-beta.N -> X.Y.Z-beta.(N+1)"
        echo "  stable: X.Y.Z-beta.N -> X.Y.Z"
        exit 1
        ;;
esac

# Assemble new version string
if [ -n "$NEW_PRE_TAG" ]; then
    NEW_VERSION="${NEW_MAJOR}.${NEW_MINOR}.${NEW_PATCH}-${NEW_PRE_TAG}.${NEW_PRE_NUM}"
else
    NEW_VERSION="${NEW_MAJOR}.${NEW_MINOR}.${NEW_PATCH}"
fi

NEW_CODE=$((CURRENT_CODE + 1))

echo "📦 Bumping version: $CURRENT_VERSION -> $NEW_VERSION"
echo "🔢 Bumping versionCode: $CURRENT_CODE -> $NEW_CODE"

# Update the file
sed -i "s/versionName = \".*\"/versionName = \"$NEW_VERSION\"/" "$GRADLE_FILE"
sed -i "s/versionCode = .*/versionCode = $NEW_CODE/" "$GRADLE_FILE"

echo "✅ Successfully updated $GRADLE_FILE"
echo "👉 Next step: git add $GRADLE_FILE && git commit -m \"chore(release): v$NEW_VERSION\" && git tag v$NEW_VERSION"
