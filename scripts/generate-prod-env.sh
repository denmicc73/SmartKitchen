#!/usr/bin/env bash
#
# Genera el fichero .env de produccion para desplegar con docker compose en un
# VPS (o servidor propio). Equivalente Linux de generate-prod-env.ps1.
#
# Uso:
#   ./scripts/generate-prod-env.sh [IP_O_DOMINIO_PUBLICO]
#
# Si se pasa la IP o el dominio del VPS, se usa para BASE_URL (los enlaces de
# los correos de confirmacion y de reseteo). Si no, se deja un marcador que
# HAY QUE editar a mano antes de arrancar.
#
# No sobrescribe un .env existente. La contrasena del admin se muestra una sola
# vez: guardala en un gestor de contrasenas.

set -euo pipefail

cd "$(dirname "$0")/.."
ENV_PATH="./.env"

if [ -e "$ENV_PATH" ]; then
    echo ".env ya existe. No se ha sobrescrito."
    exit 0
fi

# Contrasena aleatoria en base64 "url-safe" (sin +, /, =). 24 bytes -> 32 chars,
# de sobra para el minimo de 16 que exige el perfil prod para ADMIN_PASSWORD.
gen_pass() {
    openssl rand -base64 24 | tr '+/' '__' | tr -d '='
}

DB_ROOT_PASS="$(gen_pass)"
DB_PASS="$(gen_pass)"
ADMIN_PASSWORD="$(gen_pass)"

HOST_ARG="${1:-}"
if [ -n "$HOST_ARG" ]; then
    BASE_URL="http://${HOST_ARG}:8080"
else
    BASE_URL="http://CAMBIA_ESTA_IP:8080"
fi

cat > "$ENV_PATH" <<EOF
# Generado por scripts/generate-prod-env.sh — NO subir a git.

# --- Base de datos MySQL -------------------------------------------------
DB_ROOT_PASS=${DB_ROOT_PASS}
DB_NAME=smartkitchen
DB_USER=smartkitchen
DB_PASS=${DB_PASS}

# --- Usuario administrador inicial ------------------------------------- -
ADMIN_USERNAME=admin
ADMIN_PASSWORD=${ADMIN_PASSWORD}
# Correo del admin: permite usar "He olvidado mi contrasena". Opcional.
ADMIN_EMAIL=

# --- URL publica ------------------------------------------------------- -
# Se usa para construir los enlaces de los correos. Debe ser la direccion
# por la que accedes de verdad (IP o dominio del VPS, con su puerto).
BASE_URL=${BASE_URL}

# --- Envio de correo (opcional) --------------------------------------- -
# Opcion A: SMTP (Gmail necesita una "contrasena de aplicacion").
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=
SMTP_PASS=
# Opcion B: API HTTP de Brevo (si tu proveedor bloquea el SMTP saliente).
# Tiene prioridad sobre SMTP si esta rellena.
BREVO_API_KEY=
# Remitente. Con Gmail/Brevo debe ser una direccion verificada en tu cuenta.
MAIL_FROM=

# --- IA (opcional): escaner de productos y sugerencias de recetas ----- -
ANTHROPIC_API_KEY=
EOF

chmod 600 "$ENV_PATH"

echo ".env creado."
echo
echo "  Usuario admin      : admin"
echo "  Contrasena admin   : ${ADMIN_PASSWORD}"
echo "  BASE_URL           : ${BASE_URL}"
echo
echo "Guarda la contrasena ahora; no se vuelve a mostrar."
if [ -z "$HOST_ARG" ]; then
    echo
    echo "AVISO: edita BASE_URL en .env y pon la IP o el dominio reales del VPS"
    echo "       antes de 'docker compose up', o los correos llevaran enlaces rotos."
fi
