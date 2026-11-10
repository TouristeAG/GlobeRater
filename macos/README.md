# GlobeRater for macOS

Native SwiftUI app with a menu bar panel and WidgetKit widgets. macOS 14 Sonoma or later.

## Build

Requirements: Xcode 16+, [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`).

1. Add your Apple ID in Xcode → Settings → Accounts (a free account is enough).
2. `cp Config/Signing.xcconfig.example Config/Signing.xcconfig` and put your Team ID in it.
3. Generate and build:

```bash
xcodegen generate
xcodebuild -scheme GlobeRater -configuration Debug -derivedDataPath build build
```

The logic lives in the `GlobeKit` package and is tested on its own:

```bash
cd GlobeKit && swift test
```
