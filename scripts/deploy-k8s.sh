#!/bin/bash
# deploy.sh — Despliega demo-mongo-api en Kubernetes
# Uso: ./scripts/deploy-k8s.sh
#
# Prerrequisitos:
#   - kubectl configurado apuntando al cluster
#   - (Opcional) minikube para desarrollo local
#
# Este script:
#   1. Aplica namespace, secrets y configmap
#   2. Levanta MongoDB (StatefulSet + Service headless)
#   3. Espera a que MongoDB esté listo
#   4. Despliega la app (Deployment + Service)

set -e

NAMESPACE="demo-mongo-api"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
K8S_DIR="$(dirname "$SCRIPT_DIR")/k8s"

echo "=== Desplegando demo-mongo-api en Kubernetes ==="

echo "[1/6] Aplicando namespace..."
kubectl apply -f "$K8S_DIR/namespace.yaml"

echo "[2/6] Aplicando secrets y configmap..."
kubectl apply -f "$K8S_DIR/secrets.yaml"
kubectl apply -f "$K8S_DIR/configmap.yaml"

echo "[3/6] Desplegando MongoDB..."
kubectl apply -f "$K8S_DIR/mongo-service.yaml"
kubectl apply -f "$K8S_DIR/mongo-statefulset.yaml"

echo "[4/6] Esperando a que MongoDB esté listo..."
kubectl wait --for=condition=ready pod -l app=mongo -n "$NAMESPACE" --timeout=120s

echo "[5/6] Desplegando app..."
kubectl apply -f "$K8S_DIR/app-service.yaml"
kubectl apply -f "$K8S_DIR/app-deployment.yaml"

echo "[6/6] Esperando a que la app esté lista..."
kubectl wait --for=condition=ready pod -l app=app -n "$NAMESPACE" --timeout=120s

echo ""
echo "=== Despliegue completado ==="
echo ""
echo "Ver pods:"
echo "  kubectl get pods -n $NAMESPACE"
echo ""
echo "Ver servicios:"
echo "  kubectl get svc -n $NAMESPACE"
echo ""
echo "URL de la app (LoadBalancer):"
echo "  kubectl get svc app -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}'"
echo ""
echo "Logs:"
echo "  kubectl logs -l app=app -n $NAMESPACE -f"
echo ""
echo "Swagger UI:"
echo "  Abrir en navegador con port-forward:"
echo "  kubectl port-forward svc/app 8080:80 -n $NAMESPACE"
echo "  Luego: http://localhost:8080/swagger-ui/index.html"
