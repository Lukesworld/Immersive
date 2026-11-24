#!/usr/bin/env bash
set -e

echo "[*] Building Unified Immersive v2.0 (Termux Zero-Gradle Pipeline)..."
ANDROID_JAR="${ANDROID_JAR:-$HOME/.android-sdk/android.jar}"

if [ ! -f "$ANDROID_JAR" ]; then
    echo "[!] android.jar not found at $ANDROID_JAR"
    exit 1
fi

echo "[+] Using android.jar: $ANDROID_JAR"
rm -rf build
mkdir -p build/compiled_res build/gen build/obj build/dex

echo "[*] Compiling resources with aapt2..."
aapt2 compile --dir app/src/main/res -o build/compiled_res/

echo "[*] Linking resources and generating R.java..."
aapt2 link \
    -I "$ANDROID_JAR" \
    --manifest app/src/main/AndroidManifest.xml \
    --java build/gen \
    -o build/unaligned.apk \
    --auto-add-overlay \
    build/compiled_res/*.flat

echo "[*] Compiling pure Java sources..."
find app/src/main/java build/gen -name "*.java" > build/sources.txt
javac \
    --release 11 \
    -cp "$ANDROID_JAR" \
    -d build/obj \
    @build/sources.txt

echo "[*] Dexing with d8..."
d8 \
    --lib "$ANDROID_JAR" \
    --min-api 29 \
    --output build/dex \
    $(find build/obj -name "*.class")

echo "[*] Packaging classes.dex into APK..."
cp build/dex/classes.dex .
zip -uj build/unaligned.apk classes.dex
rm -f classes.dex

echo "[*] Aligning APK with zipalign..."
zipalign -f -p 4 build/unaligned.apk build/aligned.apk

KEYSTORE="$HOME/release-key.keystore"
if [ ! -f "$KEYSTORE" ]; then
    echo "[*] Generating release keystore..."
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -alias androidkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android123 -keypass android123 \
        -dname "CN=Immersive, OU=NewmanServices, O=Immersive, L=Perth, ST=WA, C=AU"
fi

echo "[*] Signing APK (v1+v2+v3) with apksigner..."
apksigner sign \
    --ks "$KEYSTORE" \
    --ks-key-alias androidkey \
    --ks-pass pass:android123 \
    --key-pass pass:android123 \
    --min-sdk-version 29 \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --out build/Immersive-signed.apk \
    build/aligned.apk

cp build/Immersive-signed.apk /sdcard/Download/Immersive-signed.apk
echo "[+] SUCCESS: Signed APK copied to /sdcard/Download/Immersive-signed.apk"
