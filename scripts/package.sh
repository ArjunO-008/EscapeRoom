#!/usr/bin/env bash
# Builds the jar and a standalone app (bundled Java runtime) with jpackage.
#   scripts/package.sh            -> uses version from release.json
# Output goes to dist/. Needs a JDK 17+ (javac, jar, jpackage) and jq.
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION="$(jq -r .version release.json)"
APP_VERSION="${VERSION%%-*}"          # jpackage only accepts numeric x.y.z
NAME="EscapeRoom"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) OS=windows ;;
  Darwin*)              OS=macos ;;
  *)                    OS=linux ;;
esac

rm -rf build dist
mkdir -p build/classes build/input dist

# 1) compile, then copy every non-code asset next to the classes
javac -encoding UTF-8 -d build/classes $(find src -name '*.java')
cp -r src/. build/classes/
find build/classes \( -name '*.java' -o -name '.gitkeep' -o -name 'README.txt' -o -name '*.piskel' \) -delete

# 2) runnable jar
jar --create --file "build/input/$NAME.jar" --main-class core.App -C build/classes .

COMMON=(--name "$NAME" --app-version "$APP_VERSION" --input build/input
        --main-jar "$NAME.jar" --main-class core.App
        --add-modules java.base,java.desktop,java.logging,java.xml
        --vendor "EscapeRoom Team" --description "Top-down 2D escape room game")

# 3) standalone app folder (bundled runtime) -> zip / tar.gz
if [ "$OS" = windows ]; then
  jpackage "${COMMON[@]}" --type app-image --icon packaging/icon.ico --dest build/app
  (cd build/app && 7z a -tzip "../../dist/$NAME-$VERSION-windows.zip" "$NAME" >/dev/null 2>&1 \
     || powershell -NoProfile -Command "Compress-Archive -Path '$NAME' -DestinationPath '../../dist/$NAME-$VERSION-windows.zip'")
  # optional installer (.exe) - needs WiX, skipped quietly if unavailable
  jpackage "${COMMON[@]}" --type exe --icon packaging/icon.ico --win-shortcut --win-menu \
           --win-dir-chooser --dest build/installer \
    && cp build/installer/*.exe "dist/$NAME-$VERSION-windows-setup.exe" \
    || echo "NOTE: Windows installer skipped (WiX not available)"
elif [ "$OS" = linux ]; then
  jpackage "${COMMON[@]}" --type app-image --icon packaging/icon.png --dest build/app
  tar -C build/app -czf "dist/$NAME-$VERSION-linux-x64.tar.gz" "$NAME"
  jpackage "${COMMON[@]}" --type deb --icon packaging/icon.png --linux-shortcut --dest build/installer \
    && cp build/installer/*.deb "dist/$NAME-$VERSION-linux-x64.deb" \
    || echo "NOTE: .deb skipped"
else
  jpackage "${COMMON[@]}" --type app-image --dest build/app
  (cd build/app && zip -qr "../../dist/$NAME-$VERSION-macos.zip" "$NAME.app")
fi
cp "build/input/$NAME.jar" "dist/$NAME-$VERSION.jar"
ls -lh dist
