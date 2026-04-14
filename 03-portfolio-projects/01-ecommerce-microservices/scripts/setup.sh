#!/bin/bash
set -e

echo "🚀 Setting up E-commerce Microservices Platform..."

# Check prerequisites
command -v java >/dev/null 2>&1 || { echo "❌ Java 21+ required"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "❌ Docker required"; exit 1; }
command -v mvn >/dev/null 2>&1 && MVN="mvn" || MVN="./mvnw"

# Start infrastructure
echo "📦 Starting infrastructure..."
docker-compose up -d postgres kafka redis zipkin

echo "⏳ Waiting for services to be ready..."
sleep 15

# Build
echo "🔨 Building all services..."
$MVN clean package -DskipTests

echo "✅ Setup complete! Run 'docker-compose up -d' to start all services."
