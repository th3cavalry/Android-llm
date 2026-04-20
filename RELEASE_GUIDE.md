# SovereignDroid Release Guide

This document explains how to create and publish a high-quality, signed release for SovereignDroid.

## 🚀 Release Process

We use a dual-workflow system to ensure that every release is stable and secure.

### 1. PR Validation (CI)
Every Pull Request and push to `main` triggers an automatic check that includes:
- **Secret Scanning**: Uses TruffleHog to detect accidental credential leaks.
- **Static Analysis**: Uses Detekt to enforce Kotlin coding standards.
- **Android Lint**: Checks for potential bugs and performance issues.
- **Unit Tests**: Runs all automated tests.
- **Compilation Check**: Ensures the app builds successfully.

**All checks must pass** before code can be merged or released.

### 2. Tagged Releases (CD)
A formal release is triggered **only when a version tag (e.g., `v0.4.0`) is pushed**. This workflow:
1. Re-runs all validation checks strictly (no errors allowed).
2. Builds an **optimized, signed Release APK**.
3. Creates a GitHub Release with automated release notes and the APK.

## 🏷️ How to Create a Release

### 1. Update the Version
Use the included script to manage Semantic Versioning correctly.

```bash
# For a standard patch update (0.4.0 -> 0.4.1)
./scripts/bump-version.sh patch

# For a new beta version (0.4.0 -> 0.4.1-beta.1)
./scripts/bump-version.sh beta

# To promote a beta to stable (0.4.1-beta.1 -> 0.4.1)
./scripts/bump-version.sh stable
```

### 2. Push and Tag
Once you've bumped the version, commit your changes and push the tag.

```bash
# Commit the version bump
git add app/build.gradle.kts
git commit -m "chore(release): v0.4.1"
git push origin main

# Create and push the tag to trigger the release
git tag v0.4.1
git push origin v0.4.1
```

## 🔐 Setup (Required Once)
To enable automated signing, you must add these secrets to your GitHub Repository (**Settings > Secrets and variables > Actions**):

| Secret Name | Description |
| :--- | :--- |
| `KEYSTORE_BASE64` | The `base64 -w 0` output of your `.jks` file |
| `KEYSTORE_PASSWORD` | The password for your keystore file |
| `KEY_ALIAS` | The alias name for your signing key |
| `KEY_PASSWORD` | The password for the specific key |

## 🛠️ Troubleshooting

- **Validation Fails:** Check the Actions tab on GitHub. Fix any Detekt, Lint, or test failures locally before pushing a new tag.
- **Signing Fails:** Ensure your GitHub Secrets exactly match your local keystore credentials.
- **Version Mismatch:** Ensure the git tag (e.g., `v0.4.0`) matches the `versionName` in `app/build.gradle.kts` exactly.
