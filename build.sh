#!/usr/bin/env bash
set -euo pipefail

# Automated setup and build script for Ubuntu.
# Installs Android SDK command-line tools, required packages, Gradle, and builds the app.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip"
GRADLE_VERSION="8.3.2"
GRADLE_DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_HOME="${HOME}/.gradle/gradle-${GRADLE_VERSION}"
GRADLE_BIN="${GRADLE_HOME}/bin/gradle"

sudo apt-get update
sudo apt-get install -y openjdk-17-jdk wget unzip curl

echo "Using Android SDK root: ${SDK_ROOT}"
mkdir -p "${SDK_ROOT}"

# Install Android command-line tools if missing.
SDKMANAGER="${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "${SDKMANAGER}" ]]; then
  echo "Installing Android command-line tools..."
  tmpdir="$(mktemp -d)"
  wget -O "${tmpdir}/cmdline-tools.zip" "${CMDLINE_TOOLS_URL}"
  mkdir -p "${SDK_ROOT}/cmdline-tools"
  unzip -q "${tmpdir}/cmdline-tools.zip" -d "${tmpdir}"
  mv "${tmpdir}/cmdline-tools" "${SDK_ROOT}/cmdline-tools/latest"
  rm -rf "${tmpdir}"
fi

export ANDROID_SDK_ROOT="${SDK_ROOT}"
export PATH="${SDK_ROOT}/platform-tools:${PATH}"

echo "Accepting Android SDK licenses..."
yes | "${SDKMANAGER}" --licenses

echo "Installing Android SDK components..."
"${SDKMANAGER}" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Install Gradle if missing (local, does not modify system packages).
if [[ ! -x "${GRADLE_BIN}" ]]; then
  echo "Installing Gradle ${GRADLE_VERSION} locally..."
  tmpgradle="$(mktemp -d)"
  wget -O "${tmpgradle}/gradle.zip" "${GRADLE_DIST_URL}"
  unzip -q "${tmpgradle}/gradle.zip" -d "${HOME}/.gradle"
  rm -rf "${tmpgradle}"
fi

cd "${ROOT_DIR}"

echo "Building project..."
"${GRADLE_BIN}" --version
"${GRADLE_BIN}" test assembleDebug

echo "Build complete. APK: app/build/outputs/apk/debug/app-debug.apk"
