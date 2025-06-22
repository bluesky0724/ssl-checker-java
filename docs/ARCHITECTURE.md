# SSL Certificate Monitor - Architecture Overview

## System Architecture

The SSL Certificate Monitor is a cloud-native application designed to monitor SSL certificate expiry dates across multiple domains. The system is built using a microservices architecture deployed on AWS infrastructure.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                AWS Cloud                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐         │
│  │   Route 53      │    │   CloudFront    │    │   API Gateway   │         │
│  │   (DNS)         │    │   (CDN)         │    │   (Optional)    │         │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘         │
│           │                       │                       │                 │
│           └───────────────────────┼───────────────────────┘                 │
│                                   │                                         │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐   │
│  │                    Application Load Balancer                          │   │
│  └─────────────────────────────────┼─────────────────────────────────────┘   │
│                                   │                                         │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐   │
│  │                           ECS Fargate                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │                    SSL Monitor Application                      │ │   │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │ │   │
│  │  │  │   REST API  │  │  Scheduled  │  │   SSL Cert  │            │ │   │
│  │  │  │  Controllers│  │   Tasks     │  │   Checker   │            │ │   │
│  │  │  └─────────────┘  └─────────────┘  └─────────────┘            │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────┼─────────────────────────────────────┘   │
│                                   │                                         │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐   │
│  │                           RDS PostgreSQL                             │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │   │
│  │  │   Domains   │  │ SSL Certs   │  │Notification│                  │   │
│  │  │   Table     │  │   Table     │  │   Logs      │                  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                   │                                         │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐   │
│  │                           Lambda Function                             │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐ │   │
│  │  │                Certificate Alert Handler                        │ │   │
│  │  │  • Daily scheduled execution                                    │ │   │
│  │  │  • Query API for expiring certificates                          │ │   │
│  │  │  • Send SNS notifications                                       │ │   │
│  │  └─────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────┼─────────────────────────────────────┘   │
│                                   │                                         │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐   │
│  │                           SNS Topic                                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │   │
│  │  │   Email     │  │   SMS       │  │   Webhook   │                  │   │
│  │  │ Subscribers │  │ Subscribers │  │ Subscribers │                  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                   │                                         │
│  ┌─────────────────────────────────┼─────────────────────────────────────┐   │
│  │                         CloudWatch                                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │   │
│  │  │   Logs      │  │  Metrics    │  │   Alarms    │                  │   │
│  │  │  (ECS/App)  │  │  (CPU/Mem)  │  │  (Health)   │                  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Application Layer (ECS Fargate)

**SSL Monitor Application**
- **Technology**: Spring Boot 3.x with Java 17
- **Container**: Docker containerized application
- **Runtime**: AWS ECS Fargate (serverless containers)
- **Scaling**: Auto-scaling based on CPU/Memory utilization

**Key Components**:
- **REST API Controllers**: Handle HTTP requests for domain and certificate management
- **Scheduled Tasks**: Automated SSL certificate checking using Spring Scheduler
- **SSL Certificate Checker**: Core business logic for certificate validation
- **Database Access**: JPA/Hibernate for data persistence

### 2. Data Layer (RDS PostgreSQL)

**Database Schema**:
- **Domains Table**: Stores domain information and monitoring configuration
- **SSL Certificates Table**: Historical certificate data and expiry information
- **Notification Logs Table**: Audit trail of notifications sent

**Features**:
- **High Availability**: Multi-AZ deployment for production
- **Backup**: Automated daily backups with 7-day retention
- **Security**: Encrypted at rest and in transit

### 3. Compute & Storage

**ECS Fargate Service**:
- **CPU**: 0.5 vCPU (512 CPU units)
- **Memory**: 1GB RAM
- **Network**: VPC with public subnets for internet access
- **Health Checks**: Application health monitoring via `/actuator/health`

**Load Balancer**:
- **Type**: Application Load Balancer (ALB)
- **Protocol**: HTTP/HTTPS
- **Health Checks**: 30-second intervals
- **Target Groups**: IP-based targeting for Fargate tasks

### 4. Monitoring & Alerting

**CloudWatch Integration**:
- **Logs**: Centralized logging for application and infrastructure
- **Metrics**: CPU, memory, and custom application metrics
- **Alarms**: Automated alerting for service health issues

**Lambda Function**:
- **Runtime**: Python 3.9
- **Schedule**: Daily execution at 6 AM UTC
- **Functionality**: 
  - Query application API for expiring certificates
  - Format and send notifications via SNS
  - Error handling and retry logic

### 5. Notification System

**SNS Topic**:
- **Protocol**: Supports multiple notification channels
- **Subscribers**: Email, SMS, HTTP webhooks
- **Message Format**: JSON with certificate details and expiry information

## Security Architecture

### Network Security
- **VPC**: Isolated network environment
- **Security Groups**: Fine-grained access control
- **Subnets**: Public subnets for ALB, private subnets for RDS
- **NACLs**: Network-level access control lists

### Application Security
- **HTTPS**: TLS encryption for all external communications
- **Authentication**: JWT-based authentication (extensible)
- **Input Validation**: SQL injection prevention and input sanitization
- **CORS**: Cross-origin resource sharing configuration

### Data Security
- **Encryption**: AES-256 encryption for data at rest
- **Transit**: TLS 1.2+ for data in transit
- **IAM**: Role-based access control for AWS services
- **Secrets**: AWS Secrets Manager for sensitive configuration

## Scalability & Performance

### Horizontal Scaling
- **ECS Service**: Auto-scaling based on CPU/memory metrics
- **Load Balancer**: Distributes traffic across multiple instances
- **Database**: Read replicas for read-heavy workloads

### Performance Optimization
- **Connection Pooling**: HikariCP for database connections
- **Caching**: Redis integration for frequently accessed data
- **Async Processing**: Non-blocking certificate checks
- **CDN**: CloudFront for static content delivery

## Disaster Recovery

### Backup Strategy
- **Database**: Daily automated backups with point-in-time recovery
- **Application**: Container images stored in ECR
- **Configuration**: Infrastructure as Code (CloudFormation)

### Recovery Procedures
- **RTO**: 15 minutes for application recovery
- **RPO**: 24 hours for data recovery
- **Multi-Region**: Cross-region backup replication

## Monitoring & Observability

### Application Metrics
- **Health Checks**: Application and infrastructure health monitoring
- **Custom Metrics**: Certificate check success/failure rates
- **Performance**: Response times and throughput monitoring

### Logging Strategy
- **Structured Logging**: JSON format for easy parsing
- **Log Levels**: DEBUG, INFO, WARN, ERROR
- **Log Retention**: 30 days for application logs
- **Log Analysis**: CloudWatch Insights for querying logs

## Deployment Strategy

### Environment Strategy
- **Development**: Local Docker Compose setup
- **Staging**: AWS environment for testing
- **Production**: Multi-AZ AWS deployment

### CI/CD Pipeline
- **Source Control**: GitHub with main branch protection
- **Build**: Maven-based build with Docker image creation
- **Test**: Unit tests, integration tests, and security scans
- **Deploy**: Automated deployment to ECS with blue-green strategy

## Cost Optimization

### Resource Optimization
- **Fargate Spot**: Use spot instances for non-critical workloads
- **Auto Scaling**: Scale down during low-usage periods
- **Reserved Instances**: For predictable workloads
- **Storage**: Use appropriate storage classes for different data types

### Monitoring Costs
- **CloudWatch**: Monitor and alert on cost thresholds
- **Tagging**: Comprehensive resource tagging for cost allocation
- **Optimization**: Regular review and optimization of resources

## Compliance & Governance

### Data Protection
- **GDPR**: Data privacy and protection compliance
- **Audit Logging**: Comprehensive audit trails
- **Data Retention**: Configurable data retention policies

### Security Compliance
- **SOC 2**: Security controls and monitoring
- **PCI DSS**: If handling payment data
- **Regular Audits**: Security assessments and penetration testing 