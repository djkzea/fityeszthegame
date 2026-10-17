#!/usr/bin/env bash
#
# Builds an installer for the machine this runs on: a .dmg on macOS, a .deb on
# Linux. Windows has its own script next to this one, because jpackage can only
# ever build for the platform it is running on - that is why the three
# installers come off three machines (or off the CI matrix in
# .github/workflows/installers.yml).
#
#   ./packaging/package.sh            # the platform's default installer
#   TYPE=app-image ./packaging/package.sh    # just the application folder
#   VERSION=1.2.0 ./packaging/package.sh
#
set -euo pipefail

cd "$(dirname "$0")/.."

NAME="Fityesz Kronika"
VERSION="${VERSION:-1.0.0}"
VENDOR="fityeszthegame.com"

MVN=./mvnw

echo "==> building the jar"
$MVN -q clean package -DskipTests

echo "==> collecting the dependencies"
$MVN -q dependency:copy-dependencies -DoutputDirectory=target/app-input -DincludeScope=runtime
cp target/demo-*.jar target/app-input/

MAIN_JAR="$(cd target/app-input && ls demo-*.jar | head -1)"

# Launcher, not HelloApplication: a main class that extends Application refuses
# to start when JavaFX is on the class path rather than the module path.
COMMON=(
  --input target/app-input
  --main-jar "$MAIN_JAR"
  --main-class com.example.demo.Launcher
  --name "$NAME"
  --app-version "$VERSION"
  --vendor "$VENDOR"
  --dest target/installer
)

case "$(uname -s)" in
  Darwin)
    TYPE="${TYPE:-dmg}"
    EXTRA=(--icon packaging/app.icns --mac-package-identifier com.fityeszthegame.kronika)
    ;;
  Linux)
    TYPE="${TYPE:-deb}"
    EXTRA=(--icon packaging/app.png --linux-shortcut --linux-menu-group Game)
    ;;
  *)
    echo "Windows: run packaging\\package.bat" >&2
    exit 1
    ;;
esac

echo "==> jpackage --type $TYPE"
rm -rf target/installer
jpackage --type "$TYPE" "${COMMON[@]}" "${EXTRA[@]}"

echo
echo "kész:"
ls -lh target/installer
