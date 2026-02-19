---
title: Spring Boot Microservices Demo
description: Demonstrates MCP-enhanced context for Copilot code generation
---

# Portfolio Advisory Microservices Demo

A Spring Boot microservices reference application demonstrating how GitHub Copilot with MCP can generate context-aware code aligned to architectural standards and shared library patterns.

## Architecture Overview

This project simulates a financial portfolio advisory system with independent microservices:

```
┌─────────────────────────────────────────────────────────────┐
│ API Gateway (Port 8080)                                     │
└──────────┬──────────────┬──────────────┬────────────────────┘
           │              │              │
    ┌──────▼──────┐ ┌────▼──────┐ ┌────▼──────┐
    │ Portfolio   │ │ Advice     │ │ Risk      │
    │ Service     │ │ Service    │ │ Service   │
    │ (8081)      │ │ (8082)     │ │ (8083)    │
    └─────┬───────┘ └────┬───────┘ └────┬──────┘
          │              │              │
    ┌─────▼──────────────▼──────────────▼─────┐
    │ Shared Libraries                        │
    │ ├─ Common Models (Portfolio, User)    │
    │ ├─ Error Handling                     │
    │ ├─ Logging & Audit                    │
    │ └─ Validation Framework               │
    └─────────────────────────────────────────┘
          │
    ┌─────▼─────────────┐
    │ Databases         │
    │ (PostgreSQL)      │
    └───────────────────┘
```

## Key Services

### Portfolio Service
Manages customer investment portfolios and accounts.

- **Port**: 8081
- **Key endpoints**:
  - `GET /api/v1/portfolios/{id}` - Retrieve portfolio
  - `POST /api/v1/portfolios` - Create new portfolio
  - `PUT /api/v1/portfolios/{id}` - Update portfolio

### Advice Service
Generates investment advice based on portfolio analysis.

- **Port**: 8082
- **Key endpoints**:
  - `POST /api/v1/advice/generate` - Generate advice
  - `GET /api/v1/advice/{id}` - Retrieve advice
  - `POST /api/v1/advice/{id}/validate` - Validate advice

### Risk Service
Evaluates risk profiles and compliance requirements.

- **Port**: 8083
- **Key endpoints**:
  - `POST /api/v1/risk/assess` - Assess risk
  - `GET /api/v1/risk/profile/{id}` - Get risk profile
  - `POST /api/v1/risk/validate` - Validate against compliance

## Technology Stack

- **Runtime**: Java 17+
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL
- **Build**: Maven
- **Testing**: JUnit 5, Mockito
- **Code Quality**: SonarQube, CodeQL
- **Logging**: Logback, ELK Stack

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 13+
- Docker (optional)

### Build & Run

```bash
# Clone repository
git clone https://github.com/demo-org/portfolio-advisory-microservices.git

# Build all services
mvn clean install

# Run Portfolio Service
cd portfolio-service
mvn spring-boot:run

# Run Advice Service (new terminal)
cd advice-service
mvn spring-boot:run

# Run Risk Service (new terminal)
cd risk-service
mvn spring-boot:run
```

## Project Structure

```
portfolio-advisory-microservices/
├── shared-libraries/
│   ├── common-models/
│   │   ├── Portfolio.java
│   │   ├── RiskProfile.java
│   │   └── AdviceRequest.java
│   ├── error-handling/
│   │   ├── ErrorCodes.java
│   │   └── ApiException.java
│   ├── logging/
│   │   └── AuditLogger.java
│   └── validation/
│       └── ValidationFramework.java
├── portfolio-service/
│   ├── src/main/java/com/portfolio/
│   │   ├── controller/PortfolioController.java
│   │   ├── service/PortfolioService.java
│   │   └── repository/PortfolioRepository.java
│   └── pom.xml
├── advice-service/
│   ├── src/main/java/com/advice/
│   │   ├── controller/AdviceController.java
│   │   ├── service/AdviceGenerationService.java
│   │   └── model/AdviceEngine.java
│   └── pom.xml
├── risk-service/
│   ├── src/main/java/com/risk/
│   │   ├── controller/RiskController.java
│   │   ├── service/RiskAssessmentService.java
│   │   └── compliance/ComplianceValidator.java
│   └── pom.xml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── CODING-STANDARDS.md
│   ├── INTEGRATION-PATTERNS.md
│   └── API-CONTRACTS.md
├── examples/
│   ├── rest-endpoint-example.java
│   ├── service-layer-example.java
│   └── test-example.java
└── .github/
    └── copilot-instructions.md
```

## Demo Scenarios

See `examples/` directory for detailed use cases:

1. **Scenario 1**: Generate REST endpoint using shared library patterns
2. **Scenario 2**: Create validation logic with framework
3. **Scenario 3**: Write integration tests following standards
4. **Scenario 4**: Handle errors using domain error codes

## Using This with Copilot Spaces + MCP

This repo is designed to demonstrate how MCP enhances code generation:

**Without MCP**: Copilot generates generic Spring Boot code
**With MCP**: Copilot generates code that:
- Uses shared domain models correctly
- Follows architectural patterns
- Includes proper error handling
- References existing utilities
- Matches your coding standards

### Quick Start with Copilot
1. Create a Copilot Space
2. Connect this repo via MCP
3. Ask Copilot: *"Generate a REST controller method to calculate portfolio risk using our shared models"*
4. Compare output with/without MCP context

## Contributing

This is a demo repository. For contributions or questions, see `CONTRIBUTING.md`

## License

MIT License - Safe for public demo and adaptation

## Contact

For questions about this demo or MCP integration: [contact info]
