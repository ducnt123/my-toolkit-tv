#!/usr/bin/env bash
set -euo pipefail

# Đảm bảo luôn tìm thấy Homebrew (Apple Silicon & Intel) kể cả khi Terminal chưa nạp lại PATH
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

if [ "$#" -lt 1 ]; then
  echo "Cách dùng: ./publish-release.sh <phiên_bản> [ghi_chú_cập_nhật]"
  echo "Ví dụ:    ./publish-release.sh 1.2 \"Update to version 1.2\""
  exit 1
fi

RAW_VER="$1"
NEW_VERSION="${RAW_VER#v}"
TAG_NAME="v${NEW_VERSION}"
NOTES="${2:-Phát hành phiên bản ${TAG_NAME}}"

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
GRADLE_FILE="${ROOT_DIR}/app/build.gradle.kts"
APK_PATH="${ROOT_DIR}/app/build/outputs/apk/release/app-release.apk"

if ! command -v gh >/dev/null 2>&1; then
  echo "Lỗi: Không tìm thấy GitHub CLI ('gh'). Hãy cài bằng: brew install gh"
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "==> Bạn chưa đăng nhập GitHub CLI. Đang mở bước đăng nhập 'gh auth login'..."
  gh auth login
fi

CURRENT_VER="$(grep -E 'versionName\s*=\s*"[^"]+"' "$GRADLE_FILE" | head -n1 | sed -E 's/.*"([^"]+)".*/\1/')"
CURRENT_CODE="$(grep -E 'versionCode\s*=\s*[0-9]+' "$GRADLE_FILE" | head -n1 | sed -E 's/.*=\s*([0-9]+).*/\1/')"

if [ "$CURRENT_VER" = "$NEW_VERSION" ]; then
  NEW_CODE="$CURRENT_CODE"
  echo "==> Giữ nguyên versionName = \"${NEW_VERSION}\", versionCode = ${NEW_CODE} (chạy lại cùng phiên bản)..."
else
  NEW_CODE=$((CURRENT_CODE + 1))
  echo "==> Cập nhật versionName = \"${NEW_VERSION}\", versionCode = ${NEW_CODE} trong app/build.gradle.kts..."
  sed -i '' -E "s/versionCode = [0-9]+/versionCode = ${NEW_CODE}/" "$GRADLE_FILE"
  sed -i '' -E "s/versionName = \"[^\"]+\"/versionName = \"${NEW_VERSION}\"/" "$GRADLE_FILE"
fi

echo "==> Đang biên dịch bản APK Release (đã ký bằng app/keystore/toolkit-signing.keystore)..."
if [ -x "/Users/duc/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home/bin/java" ]; then
  /Users/duc/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home/bin/java \
    -classpath "${ROOT_DIR}/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain assembleRelease \
    -Dorg.gradle.java.home=/Users/duc/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home
else
  "${ROOT_DIR}/gradlew" assembleRelease
fi

if [ ! -f "$APK_PATH" ]; then
  echo "Lỗi: Không tìm thấy tệp APK tại $APK_PATH"
  exit 1
fi

NAMED_APK="${ROOT_DIR}/app/build/outputs/apk/release/toolkit-launcher-${TAG_NAME}.apk"
cp "$APK_PATH" "$NAMED_APK"

echo "==> Đang đẩy mã nguồn và tạo GitHub Release ${TAG_NAME}..."
git add "$GRADLE_FILE"
git commit -m "Release ${TAG_NAME} (build ${NEW_CODE})" || true
git push origin HEAD

if gh release view "$TAG_NAME" >/dev/null 2>&1; then
  gh release upload "$TAG_NAME" "$NAMED_APK" --clobber
  gh release edit "$TAG_NAME" --title "Toolkit Launcher ${TAG_NAME}" --notes "$NOTES"
else
  gh release create "$TAG_NAME" "$NAMED_APK" \
    --title "Toolkit Launcher ${TAG_NAME}" \
    --notes "$NOTES"
fi

echo "==> Hoàn tất! Người dùng trên TV đã có thể bấm 'Cập nhật ứng dụng' để tải bản ${TAG_NAME}."
