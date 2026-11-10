#!/usr/bin/env bash
# Builds build/GlobeRater-<version>.dmg, signed with your Personal Team (Config/Signing.xcconfig).
# ADHOC=1 builds an ad-hoc signed DMG instead, for testing on this Mac only.
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION=$(sed -n 's/^ *MARKETING_VERSION: *//p' project.yml | head -1)
if [[ "${ADHOC:-0}" != 1 && ! -f Config/Signing.xcconfig ]]; then
  echo "Config/Signing.xcconfig is missing: copy Config/Signing.xcconfig.example and put your Team ID in it." >&2
  exit 1
fi

python3 ../tools/gen_xcstrings.py
xcodegen generate
(cd GlobeKit && swift test)

rm -rf build/release build/export build/dmg build/GlobeRater.xcarchive
if [[ "${ADHOC:-0}" == 1 ]]; then
  xcodebuild -scheme GlobeRater -configuration Release -derivedDataPath build/release \
    CODE_SIGN_IDENTITY=- CODE_SIGN_STYLE=Manual DEVELOPMENT_TEAM= ENABLE_HARDENED_RUNTIME=NO build
  APP=build/release/Build/Products/Release/GlobeRater.app
else
  xcodebuild archive -scheme GlobeRater -configuration Release -archivePath build/GlobeRater.xcarchive
  xcodebuild -exportArchive -archivePath build/GlobeRater.xcarchive \
    -exportOptionsPlist ExportOptions.plist -exportPath build/export
  APP=build/export/GlobeRater.app
fi

mkdir -p build/dmg
cp -R "$APP" build/dmg/
ln -s /Applications build/dmg/Applications
hdiutil create -volname GlobeRater -srcfolder build/dmg -ov -format UDZO "build/GlobeRater-$VERSION.dmg"
echo "Done: macos/build/GlobeRater-$VERSION.dmg"
