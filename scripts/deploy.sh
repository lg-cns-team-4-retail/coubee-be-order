#!/bin/bash
# USAGE: ./deploy.sh [service-directory-name]
# 예: ./deploy.sh coubee-be-order
set -e

if [ -z "$1" ]; then
  echo "오류: 서비스 디렉터리 이름을 인자로 전달해야 합니다."
  echo "사용법: $0 [service-directory-name]"
  exit 1
fi

SERVICE_DIR=$1
KUBE_DIR="../$SERVICE_DIR/.kube"
DOCKER_IMAGE_NAME="coubee/$SERVICE_DIR"
DOCKER_IMAGE_TAG="latest"

if [ ! -d "$KUBE_DIR" ]; then
  echo "오류: 쿠버네티스 설정 디렉터리를 찾을 수 없습니다: $KUBE_DIR"
  exit 1
fi

echo "Loading Docker image into Minikube..."
minikube image load "$DOCKER_IMAGE_NAME:$DOCKER_IMAGE_TAG"

echo "Deploying $SERVICE_DIR resources to Minikube..."
kubectl apply -f "$KUBE_DIR/${SERVICE_DIR}-config.yml"
kubectl apply -f "$KUBE_DIR/${SERVICE_DIR}-secret.yml"
kubectl apply -f "$KUBE_DIR/${SERVICE_DIR}-service.yml"
kubectl apply -f "$KUBE_DIR/${SERVICE_DIR}-deploy.yml"

echo "Deployment complete. Waiting for pods to be ready..."
sleep 5

# deploy.yml 파일에 정의된 label selector (app=서비스이름 형식으로 가정)
POD_LABEL="app=$SERVICE_DIR"

kubectl get pods -l $POD_LABEL

echo "Use 'kubectl logs -l $POD_LABEL -f' to see the logs."
