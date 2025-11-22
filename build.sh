#!/usr/bin/env bash
set -euo pipefail

# Automated setup and build script for Ubuntu.
# Installs Android SDK command-line tools, required packages, Gradle, and builds the app.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip"
GRADLE_VERSION="${GRADLE_VERSION_OVERRIDE:-8.4}"
GRADLE_DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_HOME="${HOME}/.gradle/gradle-${GRADLE_VERSION}"
GRADLE_BIN="${GRADLE_HOME}/bin/gradle"
SDKMANAGER="${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"
BUILD_ONLY=false

log() {
  echo "[build.sh] $*"
}

parse_args() {
  for arg in "$@"; do
    case "$arg" in
      --build-only) BUILD_ONLY=true ;;
      *) ;;
    esac
  done
}

install_apt_packages() {
  log "Installing base packages (sudo required)..."
  sudo apt-get update
  sudo apt-get install -y openjdk-17-jdk wget unzip curl
}

ensure_java_home() {
  if [[ -z "${JAVA_HOME:-}" ]]; then
    JAVA_BIN="$(command -v java || true)"
    if [[ -n "${JAVA_BIN}" ]]; then
      export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "${JAVA_BIN}")")")"
    fi
  fi
  log "JAVA_HOME=${JAVA_HOME:-not set} (java: $(command -v java || echo missing))"
}

ensure_cmdline_tools() {
  log "Using Android SDK root: ${SDK_ROOT}"
  mkdir -p "${SDK_ROOT}"

  if [[ ! -x "${SDKMANAGER}" ]]; then
    log "Installing Android command-line tools..."
    tmpdir="$(mktemp -d)"
    wget -O "${tmpdir}/cmdline-tools.zip" "${CMDLINE_TOOLS_URL}"
    mkdir -p "${SDK_ROOT}/cmdline-tools"
    unzip -q "${tmpdir}/cmdline-tools.zip" -d "${tmpdir}"
    mv "${tmpdir}/cmdline-tools" "${SDK_ROOT}/cmdline-tools/latest"
    rm -rf "${tmpdir}"
  else
    log "Android command-line tools already present."
  fi
}

resolve_gradle_bin() {
  if [[ -x "${GRADLE_BIN}" ]]; then
    return
  fi

  if [[ -x "${ROOT_DIR}/gradlew" ]]; then
    GRADLE_BIN="${ROOT_DIR}/gradlew"
    return
  fi

  if command -v gradle >/dev/null 2>&1; then
    GRADLE_BIN="$(command -v gradle)"
    return
  fi
}

accept_licenses() {
  log "Accepting Android SDK licenses..."
  if timeout 300 yes | "${SDKMANAGER}" --sdk_root="${SDK_ROOT}" --licenses >/dev/null; then
    log "Android SDK licenses accepted."
  else
    log "License acceptance failed or timed out (likely due to network). If licenses were already accepted, continuing."
  fi
}

install_sdk_components() {
  log "Installing Android SDK components (platform-tools, platform 34, build-tools 34.0.0)..."
  "${SDKMANAGER}" --sdk_root="${SDK_ROOT}" --install \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0"
  log "Android SDK components installed (or already present)."
}

ensure_gradle() {
  resolve_gradle_bin
  if [[ -x "${GRADLE_BIN}" ]]; then
    log "Using Gradle at ${GRADLE_BIN}"
    return
  fi

  if [[ "${BUILD_ONLY}" == true ]]; then
    log "Gradle not found. Install it or rerun without --build-only."
    exit 1
  fi

  log "Installing Gradle ${GRADLE_VERSION} locally..."
  tmpgradle="$(mktemp -d)"
  wget -O "${tmpgradle}/gradle.zip" "${GRADLE_DIST_URL}"
  unzip -q "${tmpgradle}/gradle.zip" -d "${HOME}/.gradle"
  rm -rf "${tmpgradle}"
  GRADLE_BIN="${GRADLE_HOME}/bin/gradle"
  log "Gradle installed at ${GRADLE_BIN}"
}

build_project() {
  cd "${ROOT_DIR}"
  export ANDROID_SDK_ROOT="${SDK_ROOT}"
  export PATH="${SDK_ROOT}/platform-tools:${PATH}"

  log "Building project with Gradle..."
  "${GRADLE_BIN}" --version
  "${GRADLE_BIN}" test assembleDebug
  log "Build complete. APK: app/build/outputs/apk/debug/app-debug.apk"
}

main() {
  parse_args "$@"

  if [[ "${BUILD_ONLY}" == true ]]; then
    log "Build-only mode: skipping dependency installation and SDK setup."
    ensure_java_home
    ensure_gradle
    build_project
    return
  fi

  install_apt_packages
  ensure_java_home
  ensure_cmdline_tools
  accept_licenses
  install_sdk_components
  ensure_gradle
  build_project
}

main "$@"
