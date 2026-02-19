# Coding Standards & Patterns

All code in this repository follows these standards to ensure consistency and maintainability.

## Code Structure

### REST Controllers

All controllers should follow this pattern:

```
@RestController
@RequestMapping("/api/v1/{resource}")
@Slf4j
public class {Resource}Controller {
    
    @Autowired
    private {Resource}Service service;
    
    @GetMapping("/{id}")
    @Operation(summary = "Get {resource} by ID")
    public ResponseEntity<{ResourceDto}> get(@PathVariable Long id) {
        log.info("Fetching {} with id: {}", "{resource}", id);
        try {
            {ResourceDto} result = service.findById(id);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            log.error("Resource not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping
    @Operation(summary = "Create new {resource}")
    public ResponseEntity<{ResourceDto}> create(@Valid @RequestBody {ResourceCreateRequest} request) {
        log.info("Creating new {resource}");
        {ResourceDto} created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/{resource}/" + created.getId()))
            .body(created);
    }
}
```

### Service Layer

Services handle business logic and validation:

```
@Service
@Slf4j
public class {Resource}Service {
    
    @Autowired
    private {Resource}Repository repository;
    
    @Autowired
    private ValidationFramework validator;
    
    public {ResourceDto} findById(Long id) {
        log.debug("Service: Finding {} by id: {}", "{resource}", id);
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCodes.RESOURCE_NOT_FOUND, 
                "{resource} not found with id: " + id
            ));
    }
    
    @Transactional
    public {ResourceDto} create({ResourceCreateRequest} request) {
        validator.validate(request);
        
        {Resource} entity = new {Resource}();
        // populate from request
        
        {Resource} saved = repository.save(entity);
        log.info("Successfully created {resource} with id: {}", saved.getId());
        
        return mapToDto(saved);
    }
}
```

### Domain Models

Shared models from `shared-libraries/common-models`:

```
@Data
@Entity
@Table(name = "portfolios")
public class Portfolio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Portfolio name is required")
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;
    
    @Enumerated(EnumType.STRING)
    private RiskProfile riskProfile;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### Error Handling

Always use domain-specific error codes:

```
public class ErrorCodes {
    public static final String RESOURCE_NOT_FOUND = "ERR_001";
    public static final String VALIDATION_FAILED = "ERR_002";
    public static final String RISK_PROFILE_INVALID = "ERR_003";
    public static final String ADVICE_GENERATION_FAILED = "ERR_004";
    public static final String COMPLIANCE_VIOLATION = "ERR_005";
}

public class ApiException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;
    
    public ApiException(String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? details : new HashMap<>();
    }
}
```

### Testing

Unit tests should follow this pattern:

```
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    
    @Mock
    private PortfolioRepository repository;
    
    @InjectMocks
    private PortfolioService service;
    
    @Test
    @DisplayName("Should return portfolio when ID exists")
    void findById_WhenExists_ReturnsPortfolio() {
        // Arrange
        Portfolio expected = new Portfolio();
        expected.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(expected));
        
        // Act
        PortfolioDto result = service.findById(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(repository).findById(1L);
    }
    
    @Test
    @DisplayName("Should throw exception when portfolio not found")
    void findById_WhenNotExists_ThrowsException() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
            () -> service.findById(999L));
    }
}
```

## Logging Standards

Use structured logging:

```
@Slf4j
public class MyService {
    public void processPortfolio(Long portfolioId) {
        log.info("Processing portfolio", 
            kv("portfolioId", portfolioId),
            kv("timestamp", LocalDateTime.now()));
        
        try {
            // process
            log.debug("Portfolio processing completed", 
                kv("portfolioId", portfolioId));
        } catch (Exception e) {
            log.error("Failed to process portfolio", 
                kv("portfolioId", portfolioId),
                kv("error", e.getMessage()), e);
        }
    }
}
```

## API Response Format

All endpoints should return consistent response format:

```json
{
  "success": true,
  "data": {
    "id": 123,
    "name": "My Portfolio",
    "riskProfile": "MEDIUM",
    "createdAt": "2026-02-19T10:30:00Z"
  },
  "metadata": {
    "timestamp": "2026-02-19T10:30:00Z",
    "requestId": "req-abc123"
  }
}
```

Error responses:

```json
{
  "success": false,
  "error": {
    "code": "ERR_001",
    "message": "Portfolio not found",
    "details": {
      "portfolioId": 999
    }
  },
  "metadata": {
    "timestamp": "2026-02-19T10:30:00Z",
    "requestId": "req-abc123"
  }
}
```

## Code Quality Targets

- **Test Coverage**: Minimum 80% at package level
- **Code Smells**: SonarQube score A
- **Security**: No critical vulnerabilities (CodeQL)
- **Performance**: API response times < 500ms (p95)

## Documentation

Every public API endpoint must include:
- `@Operation(summary = "...")`
- `@ApiResponse` annotations
- Example request/response in JavaDoc

Every service method should include:
- Clear JavaDoc describing behavior
- `@param` and `@return` documentation
- Exceptions thrown

---

**Note**: When using Copilot with MCP context, reference these standards in your prompts:
*"Generate code following the standards in CODING-STANDARDS.md"*
