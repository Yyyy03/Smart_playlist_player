# Release Steps

## 1) Generate Keystore
```bash
keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release
```

## 2) Create keystore.properties
1. Copy `keystore.properties.example` to `keystore.properties`.
2. Fill in values.

Example:
```properties
storeFile=../keystore/release.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

## 3) Build Release AAB
```bash
./gradlew bundleRelease
```

## 4) Find the Output
- AAB: `app/build/outputs/bundle/release/app-release.aab`
- APK: `app/build/outputs/apk/release/app-release.apk`

## 5) Install Release APK for Testing
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## 6) Verify Signature and Version
- Signature:
```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```
- Version:
```bash
aapt dump badging app/build/outputs/apk/release/app-release.apk | findstr version
```

## Notes
- Use forward slashes in `storeFile` paths, even on Windows.
- If the build fails, double-check keystore path and passwords.
