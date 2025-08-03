#!/bin/bash
# USAGE: ./build.sh [service-directory-name]
# 예: ./build.sh coubee-be-order
set -e

if [ -z "$1" ]; then
  echo "오류: 서비스 디렉터리 이름을 인자로 전달해야 합니다."
  echo "사용법: $0 [service-directory-name]"
  exit 1
fi

SERVICE_DIR=$1

if [ ! -d "../$SERVICE_DIR" ]; then
  echo "오류: 서비스 디렉터리를 찾을 수 없습니다: ../$SERVICE_DIR"
  exit 1
fi

# Dockerfile에 명시된 이미지 이름과 태그 (필요시 서비스별로 수정)
DOCKER_IMAGE_NAME="coubee/$SERVICE_DIR"
DOCKER_IMAGE_TAG="latest"

echo "Building $SERVICE_DIR from directory: ../$SERVICE_DIR"

# 해당 서비스 디렉터리로 이동하여 빌드 수행
cd "../$SERVICE_DIR"

echo "Building the project with Gradle..."
./gradlew build -x test

echo "Building Docker image: $DOCKER_IMAGE_NAME:$DOCKER_IMAGE_TAG"
docker build -t "$DOCKER_IMAGE_NAME:$DOCKER_IMAGE_TAG" .

# 원래 디렉터리로 복귀
cd ../scripts

echo "Build for $SERVICE_DIR complete."
