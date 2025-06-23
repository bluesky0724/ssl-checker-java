# SSL Certificate Monitor

A comprehensive SSL certificate monitoring system built with Spring Boot, featuring automated certificate expiry checks, REST API endpoints, scheduled tasks, and AWS cloud infrastructure.

## 🏗️ Architecture Overview

The SSL Certificate Monitor is a cloud-native application designed to monitor SSL certificate expiry dates across multiple domains. The system is built using a microservices architecture deployed on AWS infrastructure.

### System Components

- **Application Layer**: Spring Boot 3.x application running on ECS Fargate
- **Data Layer**: PostgreSQL database on RDS
- **Compute**: AWS ECS Fargate for serverless container management
- **Monitoring**: CloudWatch for logs, metrics, and alarms
- **Notifications**: Lambda function with SNS for automated alerts
- **Load Balancing**: Application Load Balancer for traffic distribution

For detailed architecture information, see [ARCHITECTURE.md](docs/ARCHITECTURE.md).

## 🚀 Features

### Core Functionality
- ✅ SSL certificate expiry monitoring for multiple domains
- ✅ Configurable expiry thresholds (7, 14, 30, 60, 90 days)
- ✅ Database storage with historical tracking
- ✅ REST API endpoints for domain and certificate management
- ✅ Scheduled tasks for automated certificate checking
- ✅ JPA/Hibernate persistence with PostgreSQL
- ✅ Comprehensive error handling and retry logic
- ✅ Metrics and logging with structured output
- ✅ Unit and integration tests with 80%+ coverage
- ✅ Async processing for bulk certificate checks
- ✅ Webhook notifications for certificate alerts
- ✅ Swagger/OpenAPI documentation
- ✅ Docker containerization with multi-stage builds

### AWS Infrastructure
- ✅ ECS Fargate service for application hosting
- ✅ RDS PostgreSQL instance for data persistence
- ✅ Lambda function for automated certificate alerts
- ✅ CloudWatch Log Groups for monitoring
- ✅ SNS topic for notifications
- ✅ Application Load Balancer for traffic distribution
- ✅ VPC with public/private subnets
- ✅ Security groups and IAM roles

### DevOps & CI/CD
- ✅ GitHub Actions CI/CD pipeline
- ✅ Automated testing and security scanning
- ✅ Infrastructure as Code (CloudFormation)
- ✅ Blue-green deployment strategy
- ✅ Automated rollback capabilities

## 📋 Prerequisites

### Local Development
- Java 17 or higher
- Maven 3.8+
- Docker and Docker Compose
- PostgreSQL 15+ (for local development)

### AWS Deployment
- AWS CLI configured
- AWS account with appropriate permissions
- ECR repository for Docker images
- CloudFormation deployment permissions

## 🛠️ Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd spring-docker-boilerplate
```

### 2. Start Local Environment
```bash
# Using Docker Compose (recommended)
docker-compose up -d

# Or using the provided scripts
./run.sh  # Linux/Mac
# or
run.bat   # Windows
```

### 3. Verify Installation
- Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/api/v1/actuator/health

### 4. Database Setup
The application will automatically create the database schema on startup. For manual setup:

```sql
-- Connect to PostgreSQL
psql -h localhost -U postgres -d sslmonitor

-- Verify tables
\dt
```

## 🏗️ AWS Infrastructure Deployment

### 1. Prerequisites
```bash
# Install AWS CLI
aws --version

# Configure AWS credentials
aws configure
```

### 2. Deploy Infrastructure
```bash
# Navigate to infrastructure directory
cd infrastructure

# Make deployment script executable
chmod +x deploy.sh

# Deploy CloudFormation stack
./deploy.sh
```

### 3. Deploy Lambda Function
```bash
# Navigate to lambda directory
cd infrastructure/lambda

# Make deployment script executable
chmod +x deploy.sh

# Deploy Lambda function
./deploy.sh
```

### 4. Build and Push Docker Image
```bash
# Build Docker image
docker build -t ssl-monitor:latest .

# Tag for ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag ssl-monitor:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/ssl-monitor:latest

# Push to ECR
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/ssl-monitor:latest
```

## 🔧 Configuration

### Application Configuration

#### `application.yml`
```yaml
spring:
  application:
    name: ssl-monitor
  
  datasource:
    url: jdbc:postgresql://localhost:5432/sslmonitor
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

ssl:
  monitor:
    webhook:
      enabled: true
      url: ${WEBHOOK_URL:}
    certificate:
      check-interval: 3600000  # 1 hour
      expiry-thresholds: 7,14,30,60,90
```

#### Environment Variables
```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/sslmonitor
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your-password

# SSL Monitor
SSL_MONITOR_WEBHOOK_ENABLED=true
SSL_MONITOR_WEBHOOK_URL=https://your-webhook-url.com/notifications
```

### AWS Configuration

#### CloudFormation Parameters
- `Environment`: dev/staging/prod
- `DatabasePassword`: PostgreSQL database password
- `ApplicationImageUri`: ECR image URI
- `ApplicationPort`: Application port (default: 8080)

#### Lambda Environment Variables
- `API_ENDPOINT`: Application API endpoint
- `SNS_TOPIC_ARN`: SNS topic ARN for notifications
- `ALERT_DAYS`: Days threshold for alerts (default: 30)

## 📚 API Documentation

### Base URL
- Local: `http://localhost:8080/api/v1`
- AWS: `http://your-alb-dns/api/v1`

### Authentication
Currently, the API is open for development. For production, implement JWT authentication.

### Endpoints

#### Domain Management

**Get All Domains**
```http
GET /domains
```

**Response:**
```json
[
  {
    "id": 1,
    "domainName": "example.com",
    "enabled": true,
    "checkInterval": 3600000,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

**Add Domain**
```http
POST /domains
Content-Type: application/json

{
  "domainName": "example.com",
  "enabled": true,
  "checkInterval": 3600000
}
```

**Update Domain**
```http
PUT /domains/{id}
Content-Type: application/json

{
  "domainName": "example.com",
  "enabled": true,
  "checkInterval": 3600000
}
```

**Delete Domain**
```http
DELETE /domains/{id}
```

#### Certificate Management

**Get All Certificates**
```http
GET /certificates
```

**Get Expiring Certificates**
```http
GET /certificates/expiring?days=30
```

**Response:**
```json
[
  {
    "id": 1,
    "domain": "example.com",
    "issuer": "Let's Encrypt",
    "validFrom": "2024-01-01T00:00:00Z",
    "validTo": "2024-04-01T00:00:00Z",
    "daysUntilExpiry": 25,
    "lastChecked": "2024-01-01T00:00:00Z"
  }
]
```

**Check Certificate for Domain**
```http
POST /certificates/check
Content-Type: application/json

{
  "domainName": "example.com"
}
```

#### Health Check
```http
GET /actuator/health
```

### Swagger UI
Access the interactive API documentation at:
- Local: http://localhost:8080/swagger-ui.html
- AWS: http://your-alb-dns/swagger-ui.html

## 🧪 Testing

### Run Tests
```bash
# Unit tests only
mvn test

# Integration tests
mvn verify

# All tests with coverage
mvn clean test jacoco:report
```

### Test Coverage
- Unit Tests: 80%+ coverage
- Integration Tests: Database and API testing
- End-to-End Tests: Complete workflow testing

### Test Configuration
Tests use an embedded PostgreSQL database via Testcontainers for integration testing.

## 🚀 CI/CD Pipeline

### Pipeline Overview
The CI/CD pipeline is implemented using GitHub Actions and includes:

1. **Source Code Management**: Git flow with branch protection
2. **Build & Test**: Maven build with comprehensive testing
3. **Security Scanning**: Trivy vulnerability scanning
4. **Deployment**: Automated deployment to AWS ECS

### Pipeline Stages
```
Source Code → Build & Test → Security Scan → Deploy
     │            │              │            │
   Git Flow    Maven Build    Trivy Scan   ECS Deploy
   Branching   Unit Tests     SAST         Blue-Green
   Strategy    Integration    Dependency   Rollback
```

### Deployment Strategy
- **Blue-Green Deployment**: Zero-downtime deployments
- **Automated Rollback**: Health check-based rollbacks
- **Environment Promotion**: Development → Staging → Production

For detailed CI/CD information, see [CI-CD-DESIGN.md](docs/CI-CD-DESIGN.md).

## 📊 Monitoring & Alerting

### CloudWatch Integration
- **Logs**: Centralized application and infrastructure logging
- **Metrics**: CPU, memory, and custom application metrics
- **Alarms**: Automated alerting for service health issues

### Lambda Alert Function
- **Schedule**: Daily execution at 6 AM UTC
- **Functionality**: 
  - Query application API for expiring certificates
  - Format and send notifications via SNS
  - Error handling and retry logic

### SNS Notifications
- **Protocol**: Supports multiple notification channels
- **Subscribers**: Email, SMS, HTTP webhooks
- **Message Format**: JSON with certificate details

## 🔒 Security

### Network Security
- VPC with public/private subnets
- Security groups for fine-grained access control
- Network ACLs for additional network-level protection

### Application Security
- HTTPS/TLS encryption for all communications
- Input validation and SQL injection prevention
- CORS configuration for cross-origin requests

### Data Security
- AES-256 encryption for data at rest
- TLS 1.2+ for data in transit
- IAM role-based access control
- AWS Secrets Manager for sensitive configuration

## 📈 Performance & Scalability

### Horizontal Scaling
- ECS Fargate auto-scaling based on CPU/memory metrics
- Load balancer traffic distribution
- Database read replicas for read-heavy workloads

### Performance Optimization
- Connection pooling with HikariCP
- Async processing for certificate checks
- Caching strategies for frequently accessed data

## 🛠️ Troubleshooting

### Common Issues

**Application Won't Start**
```bash
# Check logs
docker-compose logs ssl-monitor

# Verify database connectivity
docker-compose exec postgres psql -U postgres -d sslmonitor -c "SELECT 1;"
```

**Certificate Check Failures**
```bash
# Check application logs
docker-compose logs ssl-monitor

# Verify domain accessibility
curl -I https://example.com
```

**AWS Deployment Issues**
```bash
# Check CloudFormation events
aws cloudformation describe-stack-events --stack-name ssl-monitor-infrastructure

# Verify ECS service status
aws ecs describe-services --cluster dev-ssl-monitor-cluster --services dev-ssl-monitor-service
```

### Log Locations
- **Application Logs**: CloudWatch Log Groups
- **ECS Logs**: `/ecs/dev-ssl-monitor`
- **Lambda Logs**: `/aws/lambda/dev-ssl-monitor-alerts`

## 🤝 Contributing

### Development Workflow
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make your changes
4. Add tests for new functionality
5. Run the test suite: `mvn clean test`
6. Commit your changes: `git commit -m 'feat: add your feature'`
7. Push to the branch: `git push origin feature/your-feature`
8. Create a Pull Request

### Code Standards
- Follow Java coding conventions
- Use conventional commit messages
- Maintain 80%+ test coverage
- Update documentation for new features

## 🆘 Support

### Getting Help
- **Documentation**: Check this README and the docs/ directory
- **Issues**: Create an issue on GitHub
- **Discussions**: Use GitHub Discussions for questions

### Useful Links
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Documentation](https://docs.docker.com/)

## 🗺️ Roadmap

### Planned Features
- [ ] Multi-region deployment
- [ ] Advanced notification channels (Slack, Teams)
- [ ] Certificate renewal automation
- [ ] Performance dashboard
- [ ] Advanced security features
- [ ] Mobile application

### Infrastructure Improvements
- [ ] Multi-AZ database deployment
- [ ] Auto-scaling policies
- [ ] Advanced monitoring and alerting
- [ ] Disaster recovery procedures
