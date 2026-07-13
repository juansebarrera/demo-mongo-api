#!/usr/bin/env pwsh
# Exporta un dump de MongoDB desde el contenedor demo-mongo-db
# Uso: .\scripts\dump-mongo.ps1

$ErrorActionPreference = "Stop"

$CONTAINER = "demo-mongo-db"
$DUMP_DIR = "./mongo-dump"

Write-Host "Creando dump de MongoDB..."
docker exec $CONTAINER mongodump `
  --username admin `
  --password adminpassword `
  --authenticationDatabase admin `
  --db demo_mongo_api `
  --out /data/dump

Write-Host "Copiando dump al host..."
docker cp "${CONTAINER}:/data/dump" $DUMP_DIR

Write-Host "Dump exportado en $DUMP_DIR/"
Write-Host "Para subirlo al repo: git add mongo-dump/ && git commit -m 'Agregar dump de MongoDB'"
