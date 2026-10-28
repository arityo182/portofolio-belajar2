#!/bin/sh
set -eu

CERT_DIR="${CERT_DIR:-/etc/nginx/certs}"
CERT_FILE="${CERT_DIR}/server.crt"
KEY_FILE="${CERT_DIR}/server.key"

mkdir -p "${CERT_DIR}"

if [ ! -s "${CERT_FILE}" ] || [ ! -s "${KEY_FILE}" ]; then
    echo "[nginx] Generating local self-signed TLS certificate"
    openssl req -x509 -nodes -newkey rsa:2048 -sha256 -days 365 \
        -keyout "${KEY_FILE}" \
        -out "${CERT_FILE}" \
        -subj "/C=ID/ST=DKI Jakarta/L=Jakarta/O=Koperasi Digital/CN=localhost" \
        -addext "subjectAltName=DNS:localhost,DNS:koperasi.local,IP:127.0.0.1"
    chmod 600 "${KEY_FILE}"
fi

exit 0
