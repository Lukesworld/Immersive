# Immersive — Termux-Native Production Bundle (API 36)

Zero-Gradle, zero-AndroidX pure Java + Android SDK build tailored for Termux (`aapt2` + `javac` + `d8` + `zipalign` + `apksigner`).

## Quick Build in Termux
```bash
unzip Immersive_PlayStore_Termux_Native_v2.zip -d ~/Immersive
cd ~/Immersive
chmod +x build_immersive.sh
./build_immersive.sh
```
Output APK is signed with `~/release-key.keystore` and copied to `/sdcard/Download/Immersive-signed.apk`.
