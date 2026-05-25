# Release Signing

The release build is configured to sign `bundleRelease` from a local `keystore.properties` file.

## Local Files

These files are required for uploading updates to Google Play and must be backed up privately:

- `release-keys/gongbu33-upload-key.jks`
- `keystore.properties`

They are intentionally ignored by git.

## Build

```powershell
.\gradlew.bat :app:bundleRelease
```

Signed output:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Current Local Upload Certificate

The generated local upload key has this SHA-256 fingerprint:

```text
E1:ED:E7:38:57:5B:38:61:58:7B:87:45:24:22:FC:EE:D7:F1:06:97:B4:9B:C5:D9:00:A0:71:DE:C9:63:43:38
```

If Google Play Console already expects a different upload certificate, you must use the original keystore that matches that certificate. A SHA-256 fingerprint alone cannot recreate the private signing key.
