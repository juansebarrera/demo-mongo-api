#!/usr/bin/env pwsh
# Importa un dump de MongoDB al contenedor demo-mongo-db
# Uso: .\scripts\restore-mongo.ps1

$ErrorActionPreference = "Stop"

$CONTAINER = "demo-mongo-db"
$DUMP_DIR = "./mongo-dump"

if (-not (Test-Path $DUMP_DIR)) {
  Write-Host "Error: No se encontro el directorio $DUMP_DIR"
  Write-Host "Descargalo del repo o ejecuta dump-mongo.ps1 primero."
  exit 1
}

Write-Host "Copiando dump al contenedor..."
docker cp $DUMP_DIR "${CONTAINER}:/data/dump"

Write-Host "Restaurando base de datos..."
docker exec $CONTAINER mongorestore `
  --username admin `
  --password adminpassword `
  --authenticationDatabase admin `
  --db demo_mongo_api `
  /data/dump/demo_mongo_api

Write-Host "Restauracion completada."
