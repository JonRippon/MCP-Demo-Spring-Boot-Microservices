---
title: Demo Scenarios for Copilot + MCP
description: Real-world prompts to demonstrate context-aware code generation
---

# Demo Scenarios: Copilot with MCP Context

These scenarios demonstrate how MCP enhances Copilot's ability to generate code aligned with your architecture and standards.

---

## Scenario 1: REST Endpoint Generation

### The Setup
You're building the Advice Service and need to add an endpoint to validate advice against a customer's risk profile.

### Prompt (WITHOUT MCP)
```
Generate a REST POST endpoint in Spring Boot that takes an advice ID and 
returns validation results
```

**Result**: Generic Spring Boot controller with no domain knowledge
```java
@PostMapping("/validate")
public ResponseEntity<Map<String, Object>> validate(@RequestBody ValidationRequest req) {
    // Generic implementation
    return ResponseEntity.ok(new HashMap<>());
}
```

**Problems**:
- ❌ Doesn't know about your AdviceRequest model
- ❌ No error code handling
- ❌ Missing audit logging
- ❌ Generic validation

### Prompt (WITH MCP)
```
Generate a REST POST endpoint in AdviceService following our patterns in 
CODING-STANDARDS.md. It should validate advice against risk profile using the 
RiskProfileValidator from shared-libraries. Include error handling and audit logging.
```

**Result**: Context-aware code using your actual patterns
```java
@PostMapping("/validate")
@Operation(summary = "Validate advice against risk profile")
public ResponseEntity<AdviceValidationResponse> validateAdvice(
        @Valid @RequestBody AdviceValidationRequest request) {
    
    log.info("REST: POST /api/v1/advice/validate - Validating advice: {}", 
        request.getAdviceId());
    
    try {
        validator.validate(request);
        
        AdviceValidationResponse response = adviceService.validateAdviceAgainstRisk(
            request.getAdviceId(),
            request.getRiskProfileId()
        );
        
        auditLogger.logValidation("Advice", request.getAdviceId(), "PASSED");
        return ResponseEntity.ok(response);
        
    } catch (IllegalArgumentException e) {
        log.error("Validation failed", 
            new Object[]{"adviceId", request.getAdviceId(), "error", e.getMessage()});
        auditLogger.logValidation("Advice", request.getAdviceId(), "FAILED");
        
        throw new ApiException(
            ErrorCodes.VALIDATION_FAILED,
            "Advice validation failed: " + e.getMessage()
        );
    }
}
```

**Advantages**:
- ✅ Uses correct AdviceValidationRequest model
- ✅ Uses shared RiskProfileValidator
- ✅ Includes domain error codes
- ✅ Structured logging
- ✅ Audit trail
- ✅ Follows CODING-STANDARDS
- ✅ Ready for production (90% vs 10%)

---

## Scenario 2: Service-to-Service Integration

### The Setup
The Advice Service needs to call the Risk Service to assess portfolio risk before generating advice.

### Prompt (WITHOUT MCP)
```
How do I call another microservice in Spring Boot?
```

**Result**: Generic example with RestTemplate
```java
RestTemplate restTemplate = new RestTemplate();
ResponseEntity<String> response = restTemplate.getForEntity(
    "http://risk-service:8083/assess", 
    String.class
);
```

**Problems**:
- ❌ Generic URL hardcoding
- ❌ No error handling
- ❌ No timeout configuration
- ❌ No circuit breaker
- ❌ String response (not typed)

### Prompt (WITH MCP)
```
Generate code to call RiskService.assessRisk() following our integration 
patterns in ARCHITECTURE.md. Use the ServiceClient from shared-libraries 
and include circuit breaker and error handling with domain error codes.
```

**Result**: Production-ready integration
```java
@Service
@Slf4j
public class AdviceService {
    
    @Autowired
    private RiskServiceClient riskClient;
    
    @Autowired
    private ValidationFramework validator;
    
    @CircuitBreaker(name = "riskServiceBreaker", 
        fallbackMethod = "assessRiskFallback")
    public RiskAssessment assessPortfolioRisk(Portfolio portfolio) {
        log.debug("Service: Calling RiskService to assess portfolio: {}", 
            portfolio.getId());
        
        try {
            RiskAssessmentRequest request = new RiskAssessmentRequest(portfolio);
            validator.validate(request);
            
            RiskAssessment assessment = riskClient.assessRisk(request);
            
            log.info("Risk assessment completed", 
                new Object[]{"portfolioId", portfolio.getId(), 
                "riskScore", assessment.getScore()});
            
            return assessment;
            
        } catch (ResourceNotFoundException e) {
            log.error("Risk assessment failed - portfolio not found",
                new Object[]{"portfolioId", portfolio.getId()});
            throw new ApiException(
                ErrorCodes.RISK_PROFILE_INVALID,
                "Unable to assess risk: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Risk assessment service error",
                new Object[]{"portfolioId", portfolio.getId(), "error", e.getMessage()});
            throw new ApiException(
                ErrorCodes.INTEGRATION_ERROR,
                "Risk service unavailable"
            );
        }
    }
    
    // Fallback when circuit is open
    public RiskAssessment assessRiskFallback(Portfolio portfolio, Exception e) {
        log.warn("Using fallback for risk assessment: {}", e.getMessage());
        return RiskAssessment.createDefault(portfolio);
    }
}
```

**Advantages**:
- ✅ Uses typed RiskServiceClient
- ✅ Circuit breaker pattern (resilience)
- ✅ Domain-specific error codes
- ✅ Proper logging correlation
- ✅ Fallback strategy
- ✅ Validation included
- ✅ Production-ready

---

## Scenario 3: Error Handling

### The Setup
Your Portfolio Service needs to handle different types of errors consistently.

### Prompt (WITHOUT MCP)
```
How do I handle exceptions in a Spring Boot service?
```

**Result**: Generic try-catch
```java
try {
    // do something
} catch (Exception e) {
    return ResponseEntity.status(500).body("Error: " + e.getMessage());
}
```

**Problems**:
- ❌ Swallows error type information
- ❌ Generic 500 errors
- ❌ No structured logging
- ❌ No audit trail

### Prompt (WITH MCP)
```
Generate error handling for portfolio retrieval using the ErrorCodes and 
ApiException from shared-libraries. Include proper HTTP status codes and 
structured logging from our AuditLogger.
```

**Result**: Consistent, traceable error handling
```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex, 
        HttpServletRequest request) {
    
    String errorCode = ex.getErrorCode();
    String message = ex.getMessage();
    
    log.error("Resource not found error",
        new Object[]{"errorCode", errorCode, 
        "path", request.getRequestURI(),
        "message", message});
    
    auditLogger.logError(errorCode, message);
    
    ErrorResponse response = ErrorResponse.builder()
        .code(errorCode)
        .message(message)
        .timestamp(LocalDateTime.now())
        .path(request.getRequestURI())
        .build();
    
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
}

@ExceptionHandler(ApiException.class)
public ResponseEntity<ErrorResponse> handleApiException(
        ApiException ex,
        HttpServletRequest request) {
    
    log.error("API exception",
        new Object[]{"errorCode", ex.getErrorCode(),
        "message", ex.getMessage()});
    
    auditLogger.logError(ex.getErrorCode(), ex.getMessage());
    
    ErrorResponse response = ErrorResponse.builder()
        .code(ex.getErrorCode())
        .message(ex.getMessage())
        .timestamp(LocalDateTime.now())
        .path(request.getRequestURI())
        .build();
    
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
}
```

**Advantages**:
- ✅ Consistent error codes across services
- ✅ Proper HTTP status codes
- ✅ Structured logging
- ✅ Audit trail for compliance
- ✅ Reusable patterns

---

## Scenario 4: Test Generation

### The Setup
You need comprehensive unit tests for the PortfolioService.

### Prompt (WITHOUT MCP)
```
Generate unit tests for a service class
```

**Result**: Basic test structure
```java
@Test
public void testGetPortfolio() {
    Portfolio p = service.findById(1L);
    assertEquals(1L, p.getId());
}
```

**Problems**:
- ❌ Incomplete test coverage
- ❌ No mocking setup
- ❌ No error cases
- ❌ Poor naming

### Prompt (WITH MCP)
```
Generate comprehensive unit tests for PortfolioService following the patterns 
in our PortfolioServiceTest.java example. Include success cases, error cases 
with domain error codes, and verify audit logging calls.
```

**Result**: Production-quality tests
```java
@Test
@DisplayName("Should throw exception when portfolio not found")
void findById_WhenNotExists_ThrowsException() {
    // Arrange
    Long portfolioId = 999L;
    when(repository.findById(portfolioId)).thenReturn(Optional.empty());
    
    // Act & Assert
    ResourceNotFoundException exception = assertThrows(
        ResourceNotFoundException.class,
        () -> service.findById(portfolioId)
    );
    
    assertEquals(ErrorCodes.RESOURCE_NOT_FOUND, exception.getErrorCode());
    verify(repository).findById(portfolioId);
}

@Test
@DisplayName("Should create portfolio and log audit trail")
void create_WithValidRequest_CreatesAndAudits() {
    // Arrange
    PortfolioCreateRequest request = validRequest();
    Portfolio created = new Portfolio();
    when(repository.save(any())).thenReturn(created);
    
    // Act
    PortfolioDto result = service.create(request);
    
    // Assert
    verify(validator).validate(request);
    verify(repository).save(any());
    verify(auditLogger).logCreate(eq("Portfolio"), any(), anyString());
}
```

**Advantages**:
- ✅ Comprehensive coverage
- ✅ Tests error paths with correct codes
- ✅ Verifies audit logging
- ✅ Clear test names (Display Names)
- ✅ Arrange-Act-Assert pattern
- ✅ Better maintainability

---

## How to Use These in Your Demo

### Demo Flow:
1. **Show the problem** - Generic Copilot output without context
2. **Ask the prompt** - Same prompt WITHOUT MCP
3. **Show result** - Generic, incomplete code
4. **Now with MCP** - "Let me give Copilot more context..."
5. **Show MCP-enhanced result** - Production-ready code
6. **Highlight differences** - Point out quality improvements
7. **Time comparison** - "This would take you 30 minutes to write; Copilot did it in 30 seconds"

### Talking Points:
- *"Without MCP, Copilot is like a talented intern who doesn't know your codebase"*
- *"With MCP, Copilot knows your patterns, standards, and libraries as well as a senior developer"*
- *"This scales across teams - everyone gets the same quality and consistency"*

---

## Next Steps for Your Demo

1. Create a GitHub repository with this content
2. Set up a Copilot Space (you can do this even without MCP approved)
3. Show these prompts and results side-by-side
4. Then say: *"This is what happens with public repos. With GitHub MCP approved for KBC, we'd point to your Atlas and Maya repos instead, and Copilot would generate code specific to your DIPA architecture."*

---
