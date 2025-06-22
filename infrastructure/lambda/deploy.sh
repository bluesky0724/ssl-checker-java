#!/bin/bash

# Lambda Function Deployment Script
# This script packages and deploys the SSL Certificate Alert Lambda function

set -e

# Configuration
FUNCTION_NAME="ssl-monitor-alerts"
RUNTIME="python3.9"
HANDLER="certificate-alert.lambda_handler"
REGION="us-east-1"
ZIP_FILE="lambda-package.zip"

echo "🚀 Starting Lambda function deployment..."

# Create a temporary directory for packaging
TEMP_DIR=$(mktemp -d)
echo "📁 Created temporary directory: $TEMP_DIR"

# Copy the Lambda function code
cp certificate-alert.py "$TEMP_DIR/"

# Install dependencies
echo "📦 Installing dependencies..."
pip install -r requirements.txt -t "$TEMP_DIR/"

# Create deployment package
echo "📦 Creating deployment package..."
cd "$TEMP_DIR"
zip -r "$ZIP_FILE" .
cd - > /dev/null

# Move the zip file to the current directory
mv "$TEMP_DIR/$ZIP_FILE" .

# Clean up temporary directory
rm -rf "$TEMP_DIR"

echo "✅ Deployment package created: $ZIP_FILE"

# Check if function exists
if aws lambda get-function --function-name "$FUNCTION_NAME" --region "$REGION" > /dev/null 2>&1; then
    echo "🔄 Updating existing Lambda function..."
    aws lambda update-function-code \
        --function-name "$FUNCTION_NAME" \
        --zip-file "fileb://$ZIP_FILE" \
        --region "$REGION"
    
    aws lambda update-function-configuration \
        --function-name "$FUNCTION_NAME" \
        --runtime "$RUNTIME" \
        --handler "$HANDLER" \
        --timeout 30 \
        --memory-size 128 \
        --region "$REGION"
else
    echo "🆕 Creating new Lambda function..."
    # Note: You'll need to provide the role ARN when creating a new function
    echo "⚠️  Please provide the IAM role ARN for the Lambda function:"
    read -r ROLE_ARN
    
    aws lambda create-function \
        --function-name "$FUNCTION_NAME" \
        --runtime "$RUNTIME" \
        --role "$ROLE_ARN" \
        --handler "$HANDLER" \
        --zip-file "fileb://$ZIP_FILE" \
        --timeout 30 \
        --memory-size 128 \
        --region "$REGION"
fi

# Clean up the zip file
rm "$ZIP_FILE"

echo "✅ Lambda function deployment completed!"
echo "📋 Function details:"
echo "   Name: $FUNCTION_NAME"
echo "   Runtime: $RUNTIME"
echo "   Handler: $HANDLER"
echo "   Region: $REGION"

# Optional: Set environment variables
echo "🔧 Setting environment variables..."
aws lambda update-function-configuration \
    --function-name "$FUNCTION_NAME" \
    --environment "Variables={API_ENDPOINT=http://your-alb-dns/api/v1,ALERT_DAYS=30}" \
    --region "$REGION"

echo "🎉 Deployment completed successfully!" 