package com.portfolio.service;

import com.portfolio.dto.PortfolioDto;
import com.portfolio.dto.PortfolioCreateRequest;
import com.portfolio.model.Portfolio;
import com.portfolio.model.ErrorCodes;
import com.portfolio.exception.ApiException;
import com.portfolio.exception.ResourceNotFoundException;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.validation.ValidationFramework;
import com.portfolio.logging.AuditLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for portfolio management business logic.
 * 
 * Demonstrates:
 * - Using shared validation framework
 * - Error handling with domain error codes
 * - Structured logging and audit trails
 * - Transaction management
 */
@Service
@Slf4j
public class PortfolioService {
    
    @Autowired
    private PortfolioRepository portfolioRepository;
    
    @Autowired
    private ValidationFramework validator;
    
    @Autowired
    private AuditLogger auditLogger;
    
    /**
     * Find portfolio by ID.
     * 
     * @param id Portfolio identifier
     * @return PortfolioDto
     * @throws ResourceNotFoundException if not found
     */
    public PortfolioDto findById(Long id) {
        log.debug("Service: Finding portfolio with id: {}", id);
        
        Portfolio portfolio = portfolioRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Portfolio not found: {}", id);
                return new ResourceNotFoundException(
                    ErrorCodes.RESOURCE_NOT_FOUND,
                    "Portfolio not found with id: " + id
                );
            });
        
        auditLogger.logRead("Portfolio", id);
        return mapToDto(portfolio);
    }
    
    /**
     * Create a new portfolio.
     * 
     * @param request Portfolio creation request
     * @return Created PortfolioDto
     */
    @Transactional
    public PortfolioDto create(PortfolioCreateRequest request) {
        log.info("Service: Creating new portfolio for user: {}", request.getUserId());
        
        // Validate request
        try {
            validator.validate(request);
        } catch (IllegalArgumentException e) {
            log.error("Validation failed for portfolio creation: {}", e.getMessage());
            throw new ApiException(
                ErrorCodes.VALIDATION_FAILED,
                "Portfolio creation validation failed: " + e.getMessage()
            );
        }
        
        // Create entity
        Portfolio portfolio = new Portfolio();
        portfolio.setName(request.getName());
        portfolio.setUserId(request.getUserId());
        portfolio.setRiskProfile(request.getRiskProfile());
        
        // Save to database
        Portfolio saved = portfolioRepository.save(portfolio);
        
        log.info("Successfully created portfolio",
            new Object[]{"portfolioId", saved.getId(), "userId", request.getUserId()});
        
        // Audit log
        auditLogger.logCreate("Portfolio", saved.getId(), "Created by user");
        
        return mapToDto(saved);
    }
    
    /**
     * Update an existing portfolio.
     * 
     * @param id Portfolio identifier
     * @param request Update request
     * @return Updated PortfolioDto
     */
    @Transactional
    public PortfolioDto update(Long id, PortfolioCreateRequest request) {
        log.info("Service: Updating portfolio with id: {}", id);
        
        Portfolio portfolio = portfolioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                ErrorCodes.RESOURCE_NOT_FOUND,
                "Portfolio not found with id: " + id
            ));
        
        // Validate request
        validator.validate(request);
        
        // Update fields
        portfolio.setName(request.getName());
        portfolio.setRiskProfile(request.getRiskProfile());
        
        Portfolio updated = portfolioRepository.save(portfolio);
        
        log.info("Successfully updated portfolio: {}", id);
        auditLogger.logUpdate("Portfolio", id, "Updated via API");
        
        return mapToDto(updated);
    }
    
    /**
     * Delete a portfolio.
     * 
     * @param id Portfolio identifier
     */
    @Transactional
    public void delete(Long id) {
        log.info("Service: Deleting portfolio with id: {}", id);
        
        if (!portfolioRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                ErrorCodes.RESOURCE_NOT_FOUND,
                "Portfolio not found with id: " + id
            );
        }
        
        portfolioRepository.deleteById(id);
        
        log.info("Successfully deleted portfolio: {}", id);
        auditLogger.logDelete("Portfolio", id, "Deleted via API");
    }
    
    /**
     * Find all portfolios for a user.
     * 
     * @param userId User identifier
     * @return List of PortfolioDtos
     */
    public List<PortfolioDto> findByUserId(Long userId) {
        log.debug("Service: Finding portfolios for user: {}", userId);
        
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        
        log.info("Found {} portfolios for user: {}", portfolios.size(), userId);
        
        return portfolios.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Map Portfolio entity to DTO.
     * 
     * @param portfolio Portfolio entity
     * @return PortfolioDto
     */
    private PortfolioDto mapToDto(Portfolio portfolio) {
        return PortfolioDto.builder()
            .id(portfolio.getId())
            .name(portfolio.getName())
            .userId(portfolio.getUserId())
            .riskProfile(portfolio.getRiskProfile())
            .createdAt(portfolio.getCreatedAt())
            .updatedAt(portfolio.getUpdatedAt())
            .build();
    }
}
