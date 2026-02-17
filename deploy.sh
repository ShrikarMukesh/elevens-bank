#!/bin/bash
set -e

# Create Kind cluster
echo "Creating Kind cluster..."
kind create cluster --config k8s/kind-config.yaml || true

# Build Docker images
echo "Building Docker images..."
docker build -f account-service/Dockerfile -t account-service:latest .
docker build -f api-gateway/Dockerfile -t api-gateway:latest .
docker build -f auth-service/Dockerfile -t auth-service:latest .
docker build -f cards-service/Dockerfile -t cards-service:latest .
docker build -f customer-service/Dockerfile -t customer-service:latest .
docker build -f notification-service/Dockerfile -t notification-service:latest .
docker build -f transaction-service/Dockerfile -t transaction-service:latest .
docker build -f discovery-service/Dockerfile -t discovery-service:latest .

# Load images into Kind
echo "Loading images into Kind..."
kind load docker-image account-service:latest --name dev-cluster
kind load docker-image api-gateway:latest --name dev-cluster
kind load docker-image auth-service:latest --name dev-cluster
kind load docker-image cards-service:latest --name dev-cluster
kind load docker-image customer-service:latest --name dev-cluster
kind load docker-image notification-service:latest --name dev-cluster
kind load docker-image transaction-service:latest --name dev-cluster
kind load docker-image discovery-service:latest --name dev-cluster

# Apply manifests
echo "Applying Kubernetes manifests..."
kubectl apply -f k8s/
# Apply service-local manifests
kubectl apply -f cards-service/k8s/

echo "Deployment complete!"
