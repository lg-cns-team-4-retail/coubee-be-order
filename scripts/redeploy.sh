#!/bin/bash
# USAGE: ./redeploy.sh [service-directory-name]
# 예: ./redeploy.sh coubee-be-order
set -e

if [ -z "$1" ]; then
  echo "오류: 서비스 디렉터리 이름을 인자로 전달해야 합니다."
  echo "사용법: $0 [service-directory-name]"
  exit 1
fi

SERVICE_DIR=$1

# 스크립트들이 있는 디렉터리로 이동
cd "$(dirname "${BASH_SOURCE[0]}")"

if [ ! -d "../$SERVICE_DIR" ]; then
  echo "오류: 서비스 디렉터리를 찾을 수 없습니다: ../$SERVICE_DIR"
  exit 1
fi

echo "--- Starting Full Redeployment for $SERVICE_DIR ---"

./stop.sh "$SERVICE_DIR"
./build.sh "$SERVICE_DIR"
./deploy.sh "$SERVICE_DIR"

echo "--- Redeployment for $SERVICE_DIR Finished Successfully ---"
