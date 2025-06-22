#!/bin/bash

# CloudFormation Deployment Script for SSL Certificate Monitor Infrastructure

set -e

# Configuration
STACK_NAME="ssl-monitor-infrastructure"
TEMPLATE_FILE="cloudformation/main.yaml"
REGION="us-east-1"
ENVIRONMENT="dev"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Starting CloudFormation deployment...${NC}"

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo -e "${RED}❌ AWS CLI is not installed. Please install it first.${NC}"
    exit 1
fi

# Check if template file exists
if [ ! -f "$TEMPLATE_FILE" ]; then
    echo -e "${RED}❌ Template file $TEMPLATE_FILE not found.${NC}"
    exit 1
fi

# Validate CloudFormation template
echo -e "${YELLOW}🔍 Validating CloudFormation template...${NC}"
if aws cloudformation validate-template --template-body file://"$TEMPLATE_FILE" --region "$REGION" > /dev/null; then
    echo -e "${GREEN}✅ Template validation successful${NC}"
else
    echo -e "${RED}❌ Template validation failed${NC}"
    exit 1
fi

# Check if stack exists
if aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" > /dev/null 2>&1; then
    echo -e "${YELLOW}🔄 Stack exists. Updating...${NC}"
    OPERATION="update-stack"
    WAIT_OPERATION="stack-update-complete"
else
    echo -e "${YELLOW}🆕 Stack does not exist. Creating...${NC}"
    OPERATION="create-stack"
    WAIT_OPERATION="stack-create-complete"
fi

# Prompt for database password
echo -e "${YELLOW}🔐 Please enter the database password (minimum 8 characters):${NC}"
read -s -r DATABASE_PASSWORD

if [ ${#DATABASE_PASSWORD} -lt 8 ]; then
    echo -e "${RED}❌ Database password must be at least 8 characters long.${NC}"
    exit 1
fi

# Deploy the stack
echo -e "${YELLOW}📦 Deploying CloudFormation stack...${NC}"
aws cloudformation "$OPERATION" \
    --stack-name "$STACK_NAME" \
    --template-body file://"$TEMPLATE_FILE" \
    --parameters \
        ParameterKey=Environment,ParameterValue="$ENVIRONMENT" \
        ParameterKey=DatabasePassword,ParameterValue="$DATABASE_PASSWORD" \
        ParameterKey=ApplicationImageUri,ParameterValue="ssl-monitor:latest" \
    --capabilities CAPABILITY_IAM \
    --region "$REGION"

# Wait for stack operation to complete
echo -e "${YELLOW}⏳ Waiting for stack operation to complete...${NC}"
aws cloudformation wait "$WAIT_OPERATION" \
    --stack-name "$STACK_NAME" \
    --region "$REGION"

# Get stack outputs
echo -e "${GREEN}📋 Stack deployment completed! Getting outputs...${NC}"
aws cloudformation describe-stacks \
    --stack-name "$STACK_NAME" \
    --region "$REGION" \
    --query 'Stacks[0].Outputs' \
    --output table

echo -e "${GREEN}🎉 Infrastructure deployment completed successfully!${NC}"
echo -e "${YELLOW}📝 Next steps:${NC}"
echo -e "   1. Build and push your Docker image to ECR"
echo -e "   2. Update the ECS service with the new image"
echo -e "   3. Configure SNS topic subscriptions for notifications"
echo -e "   4. Test the Lambda function manually" 