#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLASSPATH_DIR="${ROOT_DIR}/out-rest"
MODE="${1:-server}"

if [[ ! -f "${CLASSPATH_DIR}/Main.class" ]]; then
  echo "No se encontró ${CLASSPATH_DIR}/Main.class. Este proyecto ya viene compilado en out-rest/."
  echo "Si querés recompilar desde fuente, primero hay que preparar el classpath de dependencias."
  exit 1
fi

case "${MODE}" in
  demo)
    exec java -cp "${CLASSPATH_DIR}" Main
    ;;
  server|"")
    exec java -cp "${CLASSPATH_DIR}" Main server
    ;;
  *)
    echo "Uso: ./run.sh [server|demo]"
    exit 1
    ;;
esac

