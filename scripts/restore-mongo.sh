#!/bin/bash
# Importa un dump de MongoDB al contenedor demo-mongo-db
# Uso: ./scripts/restore-mongo.sh

set -e

CONTAINER="demo-mongo-db"
DUMP_DIR="./mongo-dump"

if [ ! -d "$DUMP_DIR" ]; then
  echo "Error: No se encontro el directorio $DUMP_DIR"
  echo "Descargalo del repo o ejecuta dump-mongo.sh primero."
  exit 1
fi

echo "Copiando dump al contenedor..."
docker cp "$DUMP_DIR" "$CONTAINER":/data/dump

echo "Restaurando base de datos..."
docker exec "$CONTAINER" mongorestore \
  --username admin \
  --password adminpassword \
  --authenticationDatabase admin \
  --db demo_mongo_api \
  /data/dump/demo_mongo_api

echo "Restauracion completada."
