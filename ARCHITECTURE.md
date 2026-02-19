# Architecture Overview

## System Design

The portfolio advisory system uses a microservices architecture with the following characteristics:

### Design Principles

1. **Service Independence**: Each service has its own database and deployment
2. **Shared Libraries**: Common patterns encapsulated in reusable libraries
3. **Async Communication**: Event-driven where appropriate
4. **Resilience**: Circuit breakers and retry logic for service calls
5. **Security**: OAuth2 for service authentication

### Inter-Service Communication

```
Portfolio Service ──┐
                    ├─→ Advice Service ──→ Risk Service
Risk Service ───────┘
```

Services communicate via REST APIs with:
- Request/response timeouts (5s default)
- Circuit breaker pattern (Resilience4j)
- Retry logic (exponential backoff)
- Correlation IDs for tracing

### Data Flow: Portfolio Advisory

1. **User Request**: Client requests investment advice
2. **Portfolio Service**: Retrieves portfolio and customer data
3. **Risk Service**: Evaluates risk profile and compliance
4. **Advice Service**: Generates recommendations
5. **Audit**: All steps logged for compliance

```
Client Request
    ↓
Portfolio Service (Get Portfolio & Customer)
    ↓
Risk Service (Evaluate Risk Profile)
    ↓
Advice Service (Generate Advice)
    ↓
Portfolio Service (Update Portfolio with Advice)
    ↓
Audit Log (Compliance Record)
    ↓
Client Response
```

## Shared Libraries Architecture

```
shared-libraries/
├── common-models/
│   ├── Portfolio (Entity, DTO)
│   ├── RiskProfile (Enum, Validator)
│   ├── User (Entity, basic info)
│   └── AdviceRequest/Response (DTOs)
│
├── error-handling/
│   ├── ErrorCodes (Constants)
│   ├── ApiException (Base exception)
│   └── GlobalExceptionHandler (Spring advice)
│
├── logging/
│   ├── AuditLogger (Compliance logging)
│   ├── CorrelationIdFilter (Request tracking)
│   └── StructuredLogging (JSON logs)
│
├── validation/
│   ├── ValidationFramework (Main validator)
│   ├── RiskProfileValidator (Domain-specific)
│   └── PortfolioValidator (Domain-specific)
│
└── integration/
    ├── ServiceClient (Base HTTP client)
    └── CircuitBreakerConfig (Resilience patterns)
```

### Why Shared Libraries Matter

**Without MCP**: Developers manually look up shared library usage
```
Developer: "Where's the error code for validation failure?"
→ Search codebase, find ErrorCodes.java
→ Copy error code
→ Implement error handling
→ Time: 10 minutes
```

**With MCP**: Copilot automatically knows the patterns
```
Prompt: "Generate error handling using our error framework"
→ Copilot generates code with correct error codes
→ Includes proper exception handling
→ Time: 2 minutes
```

## Integration Patterns

### Pattern 1: Synchronous REST Calls

Portfolio Service → Risk Service:

```java
@Service
public class PortfolioService {
    
    @Autowired
    private RiskServiceClient riskClient;
    
    public Portfolio analyzePortfolio(Long portfolioId) {
        Portfolio portfolio = repository.findById(portfolioId)
            .orElseThrow(() -> new ResourceNotFoundException(...));
        
        // Call Risk Service
        RiskAssessment assessment = riskClient.assessRisk(
            new RiskAssessmentRequest(portfolio)
        );
        
        portfolio.setRiskScore(assessment.getScore());
        return repository.save(portfolio);
    }
}
```

**Pattern benefits**:
- Uses shared RiskServiceClient
- Handles errors via shared ErrorCodes
- Logs via shared AuditLogger
- Validates using shared ValidationFramework

### Pattern 2: Event-Driven Updates

When advice is generated, publish event:

```java
@Service
public class AdviceService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public Advice generateAdvice(AdviceRequest request) {
        Advice advice = /* generate advice */;
        
        // Publish domain event
        eventPublisher.publishEvent(
            new AdviceGeneratedEvent(advice.getId())
        );
        
        return repository.save(advice);
    }
}

// Portfolio Service listens
@Component
public class AdviceGeneratedEventListener {
    @EventListener
    public void onAdviceGenerated(AdviceGeneratedEvent event) {
        // Update portfolio with advice
        portfolioService.linkAdviceToPortfolio(event.getAdviceId());
    }
}
```

## Deployment Architecture

### Service Topology

```
┌─────────────────────────────────────┐
│ Load Balancer / API Gateway         │
└────────────┬────────────────────────┘
             │
    ┌────────┼────────┐
    │        │        │
┌───▼──┐ ┌──▼──┐ ┌───▼───┐
│Port- │ │Adv- │ │Risk   │
│folio │ │ice  │ │Service│
└───┬──┘ └──┬──┘ └───┬───┘
    │       │        │
┌───▼───────▼────────▼───┐
│  PostgreSQL Database    │
└────────────────────────┘

│  Redis Cache (optional) │
└────────────────────────┘
```

### Environment Configuration

Each service configures via `application-{env}.yml`:

```yaml
# Development
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/portfolio_dev
  jpa:
    hibernate:
      ddl-auto: update

# Production  
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
  jpa:
    hibernate:
      ddl-auto: validate
```

## Scalability Considerations

### Horizontal Scaling

Each service can scale independently:

```
Portfolio Service: 3 instances
Advice Service: 5 instances (higher load)
Risk Service: 2 instances (lightweight)
```

### Caching Strategy

- Portfolio data: 5-minute TTL
- Risk profiles: 1-hour TTL
- Advice templates: 24-hour TTL
- Cache invalidation on updates

### Performance Targets

- Portfolio retrieval: < 100ms
- Risk assessment: < 300ms
- Advice generation: < 1000ms
- End-to-end advisory flow: < 2 seconds (p95)

## Security Architecture

### Authentication & Authorization

```
Client
  ↓
API Gateway (OAuth2 token validation)
  ↓
Service (Verify scopes)
  ↓
Database (Row-level security via audit context)
```

### Data Protection

- PII encrypted at rest
- TLS 1.3 for transit
- Audit logging for compliance
- Field-level encryption for sensitive data

---

**Using this with Copilot + MCP**:
When generating service-to-service communication code, reference this architecture:
*"Generate code for calling Risk Service following our integration patterns from ARCHITECTURE.md"*
