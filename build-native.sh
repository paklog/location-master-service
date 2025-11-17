#!/bin/bash

# Build script for GraalVM Native Image

set -e

echo "==================================="
echo "Building Native Image with GraalVM"
echo "==================================="

# Check if Maven wrapper exists
if [ ! -f "./mvnw" ]; then
    echo "Error: Maven wrapper not found!"
    exit 1
fi

# Clean previous builds
echo "Cleaning previous builds..."
./mvnw clean

# Build native image
echo "Building native image (this may take several minutes)..."
./mvnw package -Pnative -DskipTests

echo ""
echo "==================================="
echo "Native image build complete!"
echo "==================================="
echo "Executable location: target/location-master-service"
echo ""
echo "To run the application:"
echo "  ./target/location-master-service"
echo ""
