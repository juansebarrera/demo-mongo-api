# deploy-k8s.ps1 — Despliega demo-mongo-api en Kubernetes (PowerShell)
# Uso: .\scripts\deploy-k8s.ps1
#
# Prerrequisitos:
#   - kubectl configurado apuntando al cluster
#   - (Opcional) minikube para desarrollo local

$ErrorActionPreference = "Stop"
$NAMESPACE = "demo-mongo-api"
$K8S_DIR = Join-Path (Split-Path $PSScriptRoot) "k8s"

Write-Host "=== Desplegando demo-mongo-api en Kubernetes ===" -ForegroundColor Cyan

Write-Host "[1/6] Aplicando namespace..."
kubectl apply -f "$K8S_DIR\namespace.yaml"

Write-Host "[2/6] Aplicando secrets y configmap..."
kubectl apply -f "$K8S_DIR\secrets.yaml"
kubectl apply -f "$K8S_DIR\configmap.yaml"

Write-Host "[3/6] Desplegando MongoDB..."
kubectl apply -f "$K8S_DIR\mongo-service.yaml"
kubectl apply -f "$K8S_DIR\mongo-statefulset.yaml"

Write-Host "[4/6] Esperando a que MongoDB este listo..."
kubectl wait --for=condition=ready pod -l app=mongo -n $NAMESPACE --timeout=120s

Write-Host "[5/6] Desplegando app..."
kubectl apply -f "$K8S_DIR\app-service.yaml"
kubectl apply -f "$K8S_DIR\app-deployment.yaml"

Write-Host "[6/6] Esperando a que la app este lista..."
kubectl wait --for=condition=ready pod -l app=app -n $NAMESPACE --timeout=120s

Write-Host ""
Write-Host "=== Despliegue completado ===" -ForegroundColor Green
Write-Host ""
Write-Host "Ver pods:"
Write-Host "  kubectl get pods -n $NAMESPACE"
Write-Host ""
Write-Host "Ver servicios:"
Write-Host "  kubectl get svc -n $NAMESPACE"
Write-Host ""
Write-Host "Swagger UI (port-forward):"
Write-Host "  kubectl port-forward svc/app 8080:80 -n $NAMESPACE"
Write-Host "  Luego: http://localhost:8080/swagger-ui/index.html"
