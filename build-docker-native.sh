#!/bin/bash

# Build script for Docker Native Image

set -e

IMAGE_NAME="${1:-location-master-service}"
IMAGE_TAG="${2:-native}"

echo "=========================================="
echo "Building Docker Native Image"
echo "=========================================="
echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""

# Build Docker image
echo "Building Docker image (this may take several minutes)..."
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

echo ""
echo "=========================================="
echo "Docker native image build complete!"
echo "=========================================="
echo "Image: ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""
echo "To run with docker-compose:"
echo "  docker-compose up"
echo ""
echo "To run standalone:"
echo "  docker run -p 8080:8080 ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""

# Show image size
echo "Image size:"
docker images "${IMAGE_NAME}:${IMAGE_TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
echo ""
