#!/usr/bin/env bash
set -euo pipefail

PLATFORM="${1:?platform is required}"
ARCHIVE="${2:?archive type is required}"
DIST_DIR="${3:-jvm-minter/build/install/jvm-minter}"
MAIN_CLASS="com.orioooneee.lmuasister.minter.MainKt"
ARTIFACT_NAME="lmu-minter-${PLATFORM}"
RELEASE_DIR="build/minter-release"
WORK_DIR="${RELEASE_DIR}/${ARTIFACT_NAME}"

if [[ -z "${JAVA_HOME:-}" ]]; then
  JAVA_BIN="$(command -v java || true)"
  if [[ -z "$JAVA_BIN" ]]; then
    echo "JAVA_HOME is not set and java was not found on PATH" >&2
    exit 1
  fi
  JAVA_HOME="$("$JAVA_BIN" -XshowSettings:properties -version 2>&1 | awk -F'= ' '/java.home =/ { print $2; exit }')"
fi

rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR/bin" "$WORK_DIR/lib" "$RELEASE_DIR"
rm -f "$RELEASE_DIR/$ARTIFACT_NAME.tar.gz" "$RELEASE_DIR/$ARTIFACT_NAME.zip"

cp -R "$DIST_DIR/lib/." "$WORK_DIR/lib/"

MODULES_RAW="$("$JAVA_HOME/bin/jdeps" \
  --multi-release 21 \
  --ignore-missing-deps \
  --recursive \
  --print-module-deps \
  --class-path "$WORK_DIR/lib/*" \
  "$WORK_DIR/lib"/*.jar 2>/dev/null || true)"
MODULES="$(printf '%s\n' "$MODULES_RAW" | tail -n 1 | tr -d '[:space:]')"
if [[ -z "$MODULES" || "$MODULES" == Warning:* ]]; then
  MODULES="java.base,java.instrument,java.management,java.naming,java.sql,jdk.httpserver,jdk.unsupported"
fi

add_module() {
  case ",$MODULES," in
    *",$1,"*) ;;
    *) MODULES="$MODULES,$1" ;;
  esac
}

add_module "jdk.crypto.ec"
add_module "java.security.jgss"

"$JAVA_HOME/bin/jlink" \
  --add-modules "$MODULES" \
  --strip-debug \
  --no-header-files \
  --no-man-pages \
  --compress=2 \
  --output "$WORK_DIR/runtime"

cat > "$WORK_DIR/bin/lmu-minter" <<EOF
#!/usr/bin/env sh
set -eu
APP_HOME="\$(CDPATH= cd -- "\$(dirname -- "\$0")/.." && pwd)"
exec "\$APP_HOME/runtime/bin/java" -cp "\$APP_HOME/lib/*" "$MAIN_CLASS" "\$@"
EOF
chmod +x "$WORK_DIR/bin/lmu-minter"

cat > "$WORK_DIR/README.txt" <<EOF
LMU Assister JVM Minter

Run:
  ./bin/lmu-minter

The runtime directory is bundled. Java does not need to be installed separately.
EOF

case "$ARCHIVE" in
  tar.gz)
    tar -C "$RELEASE_DIR" -czf "$RELEASE_DIR/$ARTIFACT_NAME.tar.gz" "$ARTIFACT_NAME"
    ;;
  zip)
    (cd "$RELEASE_DIR" && zip -qr "$ARTIFACT_NAME.zip" "$ARTIFACT_NAME")
    ;;
  *)
    echo "Unsupported archive type: $ARCHIVE" >&2
    exit 1
    ;;
esac
