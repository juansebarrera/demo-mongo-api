#!/bin/bash
# Exporta un dump de MongoDB desde el contenedor demo-mongo-db
# Uso: ./scripts/dump-mongo.sh

set -e

CONTAINER="demo-mongo-db"
DUMP_DIR="./mongo-dump"

echo "Creando dump de MongoDB..."
docker exec "$CONTAINER" mongodump \
  --username admin \
  --password adminpassword \
  --authenticationDatabase admin \
  --db demo_mongo_api \
  --out /data/dump

echo "Copiando dump al host..."
docker cp "$CONTAINER":/data/dump "$DUMP_DIR"

echo "Dump exportado en $DUMP_DIR/"
echo "Para subirlo al repo: git add mongo-dump/ && git commit -m 'Agregar dump de MongoDB'"
