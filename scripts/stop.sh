#!/bin/bash
# USAGE: ./stop.sh [service-directory-name]
# 예: ./stop.sh coubee-be-order
set -e

if [ -z "$1" ]; then
  echo "오류: 서비스 디렉터리 이름을 인자로 전달해야 합니다."
  echo "사용법: $0 [service-directory-name]"
  exit 1
fi

SERVICE_DIR=$1
KUBE_DIR="../$SERVICE_DIR/.kube"

if [ ! -d "$KUBE_DIR" ]; then
  echo "오류: 쿠버네티스 설정 디렉터리를 찾을 수 없습니다: $KUBE_DIR"
  exit 1
fi

echo "Stopping $SERVICE_DIR service in Minikube..."

# .kube 디렉터리 내 yaml 파일들을 역순으로 삭제
kubectl delete -f "$KUBE_DIR/${SERVICE_DIR}-deploy.yml" --ignore-not-found=true
kubectl delete -f "$KUBE_DIR/${SERVICE_DIR}-service.yml" --ignore-not-found=true
kubectl delete -f "$KUBE_DIR/${SERVICE_DIR}-secret.yml" --ignore-not-found=true
kubectl delete -f "$KUBE_DIR/${SERVICE_DIR}-config.yml" --ignore-not-found=true

echo "All $SERVICE_DIR resources deleted."
