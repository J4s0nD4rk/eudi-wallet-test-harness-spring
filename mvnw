#!/bin/sh
# Maven-Wrapper: erlaubt "./mvnw ..." ohne global installiertes Maven.
#
# Bewusst eine schlanke Eigenimplementierung statt einer 1:1-Kopie des deutlich
# umfangreicheren offiziellen Apache-Maven-Wrapper-Skripts: In der
# Entwicklungsumgebung, in der dieses Projekt entstanden ist, konnte kein
# tatsaechlicher Download/Testlauf gegen echte Maven-Distributionen durchgefuehrt
# werden (siehe README.md, "Bekannte Einschraenkungen"). Diese Variante ist kurz
# genug, um sie beim Lesen vollstaendig nachzuvollziehen, und deckt denselben Zweck
# ab: laedt bei Bedarf die in .mvn/wrapper/maven-wrapper.properties konfigurierte
# Maven-Version herunter, cached sie unter ~/.m2/wrapper/dists/ und delegiert dann
# an das echte "mvn".

set -e

WRAPPER_DIR="$(cd "$(dirname "$0")" && pwd)"
PROPERTIES_FILE="$WRAPPER_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "Fehler: $PROPERTIES_FILE nicht gefunden." >&2
  exit 1
fi

DISTRIBUTION_URL=$(grep '^distributionUrl=' "$PROPERTIES_FILE" | cut -d'=' -f2-)
if [ -z "$DISTRIBUTION_URL" ]; then
  echo "Fehler: distributionUrl fehlt in $PROPERTIES_FILE." >&2
  exit 1
fi

DIST_NAME=$(basename "$DISTRIBUTION_URL" .zip)
DIR_NAME="${DIST_NAME%-bin}"
DIST_CACHE_DIR="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/$DIST_NAME"
MAVEN_HOME="$DIST_CACHE_DIR/$DIR_NAME"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  echo "Lade Maven-Distribution herunter: $DISTRIBUTION_URL"
  mkdir -p "$DIST_CACHE_DIR"
  TMP_ZIP="$DIST_CACHE_DIR/download.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$TMP_ZIP" "$DISTRIBUTION_URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$TMP_ZIP" "$DISTRIBUTION_URL"
  else
    echo "Fehler: weder curl noch wget verfuegbar, um Maven herunterzuladen." >&2
    exit 1
  fi
  unzip -q -o "$TMP_ZIP" -d "$DIST_CACHE_DIR"
  rm -f "$TMP_ZIP"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
