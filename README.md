# 순공

순공은 과목별 순수 공부 시간을 재고, 날짜별 통계를 확인하는 Android 앱입니다.

## App Info

- App name: 순공
- Package name: `kr.kro.gongbu33`
- Version name: `alpa01`
- Version code: `1`
- Min SDK: `24`
- Target SDK: `36`
- Upload/app signing certificate fingerprint provided for release setup:
  `02:74:0F:29:8F:28:57:9E:C8:59:C0:7A:F5:01:A9:07:F1:09:B9:45:94:C4:13:A7:40:92:2D:5E:35:0A:C3:72`

## Play Console Files

- Privacy policy draft: [PRIVACY_POLICY.md](PRIVACY_POLICY.md)
- Foreground service declaration draft: [FOREGROUND_SERVICE_DECLARATION.md](FOREGROUND_SERVICE_DECLARATION.md)
- Release signing setup: [SIGNING.md](SIGNING.md)
- Preview assets: [store-assets](store-assets)

## Release Build

```powershell
.\gradlew.bat :app:bundleRelease
```

Output:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Data Safety Summary

The app stores study records locally on the user's device. It does not include ads, analytics SDKs, account login, server sync, or third-party data sharing.

Declare data collection carefully in Play Console based on the final release build and privacy policy.
