#!/bin/bash
# iceTerm Android APK Build Script (Zero-Gradle, Pure SDK Toolchain)
# Builds high-performance, single-dex, sub-100KB APK in seconds without Gradle overhead.
set -euo pipefail
cd "$(dirname "$0")"

# 1. Locate Android SDK
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ]; then
  for c in "$HOME/Android/Sdk" "$HOME/apkbuild" /usr/local/lib/android/sdk /opt/android-sdk; do
    [ -d "$c" ] && SDK="$c" && break
  done
fi
[ -n "$SDK" ] && [ -d "$SDK" ] || { echo "ERROR: Android SDK not found. Set ANDROID_HOME"; exit 1; }

# Pick highest build-tools version
BT=$(ls -1d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)
[ -n "$BT" ] || { echo "ERROR: No build-tools found under $SDK/build-tools"; exit 1; }

# Pick highest available platform
PLATFORM=""
for v in 35 34 33 32 31 30 29 28 27 26 25 24 23 22 21; do
  [ -f "$SDK/platforms/android-$v/android.jar" ] && PLATFORM="$v" && break
done
[ -n "$PLATFORM" ] || { echo "ERROR: No platform android.jar found under $SDK/platforms"; exit 1; }
ANDROID_JAR="$SDK/platforms/android-$PLATFORM/android.jar"

AAPT2="$BT/aapt2"
D8="$BT/d8"
ZIPALIGN="$BT/zipalign"
APKSIGNER="$BT/apksigner"
command -v javac >/dev/null || { echo "ERROR: javac (JDK 17+) required"; exit 1; }

echo "[1/6] Config: SDK=$SDK, build-tools=$(basename "$BT"), platform=android-$PLATFORM"

WORK=build
rm -rf "$WORK"
mkdir -p "$WORK"/{res,gen,classes,dex}

# 2. Compile Resources
echo "[2/6] Compiling resources with aapt2..."
"$AAPT2" compile --dir res -o "$WORK/res.zip"

echo "[3/6] Linking resources and generating R.java..."
"$AAPT2" link \
  -o "$WORK/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  -R "$WORK/res.zip" \
  --java "$WORK/gen" \
  --min-sdk-version 21 \
  --target-sdk-version 34 \
  --auto-add-overlay

# 3. Compile Java Source Files
echo "[4/6] Compiling Java source..."
SRC=$(find src -name '*.java')
GEN=$(find "$WORK/gen" -name '*.java' 2>/dev/null || true)
javac -encoding UTF-8 -source 8 -target 8 \
  -bootclasspath "$ANDROID_JAR" -classpath "$ANDROID_JAR" \
  -nowarn -d "$WORK/classes" $SRC $GEN 2>&1 | grep -v 'bootstrap class path' || true

CLASS_COUNT=$(find "$WORK/classes" -name '*.class' | wc -l)
[ "$CLASS_COUNT" -gt 0 ] || { echo "ERROR: No class files generated"; exit 1; }
echo "Compiled classes count: $CLASS_COUNT"

# 4. Convert to DEX
echo "[5/6] Converting classes to DEX (d8)..."
"$D8" --min-api 21 --lib "$ANDROID_JAR" --output "$WORK/dex" \
  $(find "$WORK/classes" -name '*.class') > /dev/null
[ -f "$WORK/dex/classes.dex" ] || { echo "ERROR: d8 conversion failed"; exit 1; }

# 5. Assemble, Align & Sign
echo "[6/6] Packaging and signing APK..."
cp "$WORK/base.apk" "$WORK/unsigned.apk"
(cd "$WORK/dex" && zip -q -j "../unsigned.apk" classes.dex)
"$ZIPALIGN" -f 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"

KS="${KEYSTORE:-$WORK/keystore.jks}"
KS_PASS="${KS_PASS:-iceterm123}"
KEY_PASS="${KEY_PASS:-iceterm123}"
if [ ! -f "$KS" ]; then
  keytool -genkey -v -keystore "$KS" -alias iceterm -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$KS_PASS" -keypass "$KEY_PASS" \
    -dname "CN=iceTerm, OU=Terminal, O=ice-wocker, L=Tech, ST=OpenSource, C=CN" 2>/dev/null
fi

"$APKSIGNER" sign --ks "$KS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KEY_PASS" \
  --out iceterm.apk "$WORK/aligned.apk"
"$APKSIGNER" verify iceterm.apk > /dev/null

SIZE=$(stat -c%s iceterm.apk 2>/dev/null || stat -f%z iceterm.apk)
echo "SUCCESS: Produced iceterm.apk ($SIZE bytes / $((SIZE/1024)) KB)"
