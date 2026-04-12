# Release Guide

This document explains how to create and publish a new release for Android-llm.

## Automated Release Process

The project uses GitHub Actions to automatically build and release APKs when you create a git tag.

### Workflow Overview

1. **On every push/PR**: Builds the debug APK and uploads it as an artifact
2. **On tag creation** (`v*`): Automatically creates a GitHub release with the APK attached

### Creating a Release

Follow these steps to create a new release:

#### 1. Bump the version number

Use the version bumping script:

```bash
./scripts/bump-version.sh <patch|minor|major>
```

- **patch**: Increment the patch version (e.g., 0.0.3 → 0.0.4)
- **minor**: Increment the minor version and reset patch (e.g., 0.0.3 → 0.1.0)
- **major**: Increment the major version and reset minor/patch (e.g., 0.0.3 → 1.0.0)

#### 2. Commit the changes

```bash
git add app/build.gradle.kts
git commit -m "Bump version to 0.0.4"
git push origin main
```

#### 3. Create and push a tag

```bash
git tag v0.0.4
git push origin v0.0.4
```

#### 4. GitHub Actions will automatically:
- Build the APK
- Create a GitHub Release
- Attach the APK to the release

### Manual Release Creation

If you need to create a release manually:

1. Go to GitHub → Releases → Create a new release
2. Create a new tag (e.g., `v0.0.4`) or select an existing tag
3. Click "Generate release notes"
4. Download the APK from workflow artifacts (Actions tab)
5. Upload the APK manually to the release

## Version Numbering

The project uses semantic versioning:

- **versionCode**: Internal integer, incremented for each build (e.g., 2, 3, 4)
- **versionName**: Semantic version string (e.g., "0.0.3-alpha", "0.0.4", "1.0.0")

## Build Artifacts

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk` (requires signing)

## Troubleshooting

### Build fails with Java version errors
Ensure you're using Java 17+:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### Gradle wrapper issues
Regenerate the wrapper:
```bash
gradle wrapper --gradle-version 8.13
```

## CI/CD Workflow Status

Check the workflow run status at:
`https://github.com/th3cavalry/Android-llm/actions/workflows/build-and-release.yml`
