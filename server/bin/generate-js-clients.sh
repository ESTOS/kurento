#!/usr/bin/env bash
# Generate kurento-client-(core|elements|filters) JavaScript API packages
# from .kmd.json definitions using kurento-module-creator.
#
# Usage:
#   bin/generate-js-clients.sh [Debug|RelWithDebInfo]
#
# Output (per module):
#   module-<name>/build/js/package.json
#   module-<name>/build/js/lib/*.js
#   module-<name>/build/js/src/*.kmd.json

set -euo pipefail

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
SERVER_DIR="$(cd -P -- "$SELF_DIR/.." >/dev/null && pwd -P)"

BUILD_TYPE="${1:-Debug}"
BUILD_DIR="$SERVER_DIR/build-${BUILD_TYPE}"
JAR="$BUILD_DIR/module-creator/kurento-module-creator-jar-with-dependencies.jar"

CORE_INTERFACE="$SERVER_DIR/module-core/src/server/interface"
ELEMENTS_INTERFACE="$SERVER_DIR/module-elements/src/server/interface"
FILTERS_INTERFACE="$SERVER_DIR/module-filters/src/server/interface"

if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA="${JAVA_HOME}/bin/java"
elif command -v java >/dev/null 2>&1; then
  JAVA="java"
else
  echo "ERROR: java not found (set JAVA_HOME)" >&2
  exit 1
fi

if command -v mvn.cmd >/dev/null 2>&1; then
  MVN="mvn.cmd"
elif command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
else
  echo "ERROR: mvn not found" >&2
  exit 1
fi

ensure_module_creator()
{
  if [[ -f "$JAR" ]]; then
    return
  fi

  echo "Building kurento-module-creator into $BUILD_DIR/module-creator ..."
  mkdir -p "$BUILD_DIR/module-creator"
  (
    cd "$SERVER_DIR/module-creator"
    "$MVN" package -DskipTests -DbuildDirectory="$BUILD_DIR/module-creator"
  )
}

generate_js_module()
{
  local name="$1"
  local kmd_dir="$2"
  local out_dir="$3"
  shift 3

  echo "Generating JavaScript client for $name -> $out_dir"
  mkdir -p "$out_dir/lib" "$out_dir/src"

  "$JAVA" -jar "$JAR" -r "$kmd_dir" "$@" -c "$out_dir" -npm
  "$JAVA" -jar "$JAR" -r "$kmd_dir" "$@" -c "$out_dir/lib" -it client-js/templates
  "$JAVA" -jar "$JAR" -r "$kmd_dir" "$@" -o "$out_dir/src"

  if [[ -f "$SERVER_DIR/LICENSE" ]]; then
    cp "$SERVER_DIR/LICENSE" "$out_dir/LICENSE"
  fi
}

ensure_module_creator

KMD_EXPORT="$BUILD_DIR/kmd-export"
mkdir -p "$KMD_EXPORT"

echo "Exporting merged KMD descriptors to $KMD_EXPORT ..."
"$JAVA" -jar "$JAR" -r "$CORE_INTERFACE" -o "$KMD_EXPORT/core"
"$JAVA" -jar "$JAR" -r "$ELEMENTS_INTERFACE" -dr "$KMD_EXPORT/core" -o "$KMD_EXPORT/elements"
"$JAVA" -jar "$JAR" -r "$FILTERS_INTERFACE" \
  -dr "$KMD_EXPORT/core" -dr "$KMD_EXPORT/elements" -o "$KMD_EXPORT/filters"

CORE_KMD_DEP="$KMD_EXPORT/core"
ELEMENTS_KMD_DEP="$KMD_EXPORT/elements"

generate_js_module "module-core" "$CORE_INTERFACE" \
  "$SERVER_DIR/module-core/build/js"

generate_js_module "module-elements" "$ELEMENTS_INTERFACE" \
  "$SERVER_DIR/module-elements/build/js" \
  -dr "$CORE_KMD_DEP"

generate_js_module "module-filters" "$FILTERS_INTERFACE" \
  "$SERVER_DIR/module-filters/build/js" \
  -dr "$CORE_KMD_DEP" \
  -dr "$ELEMENTS_KMD_DEP"

echo "JavaScript client API generation finished."
