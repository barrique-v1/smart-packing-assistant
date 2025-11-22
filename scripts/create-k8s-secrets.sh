#!/bin/bash
set -e

echo "======================================================================"
echo "  Smart Packing Assistant - Kubernetes Secrets Setup"
echo "======================================================================"

# Check if .env file exists
if [ ! -f .env ]; then
    echo "❌ Error: .env file not found!"
    echo ""
    echo "   Please create .env file from .env.example:"
    echo "   $ cp .env.example .env"
    echo "   $ nano .env  # Edit and add your OpenAI API key"
    echo ""
    exit 1
fi

# Load environment variables
echo "📖 Loading environment variables from .env..."
source .env

# Validate required variables
echo "✓ Validating environment variables..."

if [ -z "$OPENAI_API_KEY" ]; then
    echo "❌ Error: OPENAI_API_KEY not set in .env file!"
    exit 1
fi

if [ "$OPENAI_API_KEY" = "sk-your-actual-openai-api-key-here" ]; then
    echo "❌ Error: Please replace the placeholder OPENAI_API_KEY in .env with your actual key!"
    echo "   Get your key from: https://platform.openai.com/api-keys"
    exit 1
fi

if [ -z "$POSTGRES_DB" ] || [ "$POSTGRES_DB" = "NAME" ]; then
    echo "❌ Error: POSTGRES_DB not properly set in .env file!"
    exit 1
fi

if [ -z "$POSTGRES_USER" ] || [ "$POSTGRES_USER" = "USERNAME" ]; then
    echo "❌ Error: POSTGRES_USER not properly set in .env file!"
    exit 1
fi

if [ -z "$POSTGRES_PASSWORD" ] || [ "$POSTGRES_PASSWORD" = "PASSWORD" ]; then
    echo "❌ Error: POSTGRES_PASSWORD not properly set in .env file!"
    exit 1
fi

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ Error: kubectl not found!"
    echo "   Please install kubectl: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi

# Check if namespace exists, create if not
echo "🔍 Checking namespace 'packing-assistant'..."
if kubectl get namespace packing-assistant &> /dev/null; then
    echo "✅ Namespace 'packing-assistant' already exists"
else
    echo "⚠️  Namespace 'packing-assistant' not found"
    echo "   Creating namespace first..."
    kubectl apply -f k8s/00-namespace.yaml
    echo "✅ Namespace created"
fi

# Create secret
echo ""
echo "🔐 Creating Kubernetes secret 'app-secrets'..."
echo "   - postgres-db: $POSTGRES_DB"
echo "   - postgres-user: $POSTGRES_USER"
echo "   - postgres-password: ********"
echo "   - openai-api-key: ${OPENAI_API_KEY:0:20}... (${#OPENAI_API_KEY} chars)"
echo ""

kubectl create secret generic app-secrets \
  --namespace=packing-assistant \
  --from-literal=postgres-db="$POSTGRES_DB" \
  --from-literal=postgres-user="$POSTGRES_USER" \
  --from-literal=postgres-password="$POSTGRES_PASSWORD" \
  --from-literal=openai-api-key="$OPENAI_API_KEY" \
  --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "✅ Secrets created successfully!"
echo ""
echo "🔍 Verifying secret..."
kubectl get secret app-secrets -n packing-assistant &> /dev/null && echo "✅ Secret 'app-secrets' exists in namespace 'packing-assistant'"

echo ""
echo "======================================================================"
echo "  ✅ Setup Complete!"
echo "======================================================================"
echo ""
