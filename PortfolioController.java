package com.portfolio.controller;

import com.portfolio.dto.PortfolioDto;
import com.portfolio.dto.PortfolioCreateRequest;
import com.portfolio.service.PortfolioService;
import com.portfolio.model.ErrorCodes;
import com.portfolio.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

/**
 * REST Controller for Portfolio management endpoints.
 * 
 * Demonstrates:
 * - Following shared REST patterns
 * - Using domain error codes
 * - Structured logging
 * - API documentation
 */
@RestController
@RequestMapping("/api/v1/portfolios")
@Slf4j
public class PortfolioController {
    
    @Autowired
    private PortfolioService portfolioService;
    
    /**
     * Retrieve a portfolio by ID.
     * 
     * @param id Portfolio identifier
     * @return Portfolio details
     * @throws ResourceNotFoundException if portfolio not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get portfolio by ID")
    @ApiResponse(responseCode = "200", description = "Portfolio found")
    @ApiResponse(responseCode = "404", description = "Portfolio not found")
    public ResponseEntity<PortfolioDto> getPortfolio(@PathVariable Long id) {
        log.info("REST: GET /portfolios/{}", id);
        
        try {
            PortfolioDto portfolio = portfolioService.findById(id);
            log.debug("Successfully retrieved portfolio", 
                new Object[]{"portfolioId", id});
            return ResponseEntity.ok(portfolio);
        } catch (ApiException e) {
            log.error("Error retrieving portfolio", 
                new Object[]{"portfolioId", id, "errorCode", e.getErrorCode()});
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    /**
     * Create a new portfolio.
     * 
     * @param request Portfolio creation request
     * @return Created portfolio with assigned ID
     */
    @PostMapping
    @Operation(summary = "Create new portfolio")
    @ApiResponse(responseCode = "201", description = "Portfolio created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<PortfolioDto> createPortfolio(
            @Valid @RequestBody PortfolioCreateRequest request) {
        
        log.info("REST: POST /portfolios - Creating new portfolio for user: {}", 
            request.getUserId());
        
        PortfolioDto created = portfolioService.create(request);
        
        log.info("Successfully created portfolio", 
            new Object[]{"portfolioId", created.getId(), "userId", request.getUserId()});
        
        return ResponseEntity
            .created(URI.create("/api/v1/portfolios/" + created.getId()))
            .body(created);
    }
    
    /**
     * Update an existing portfolio.
     * 
     * @param id Portfolio identifier
     * @param request Update request
     * @return Updated portfolio
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update portfolio")
    @ApiResponse(responseCode = "200", description = "Portfolio updated")
    @ApiResponse(responseCode = "404", description = "Portfolio not found")
    public ResponseEntity<PortfolioDto> updatePortfolio(
            @PathVariable Long id,
            @Valid @RequestBody PortfolioCreateRequest request) {
        
        log.info("REST: PUT /portfolios/{} - Updating portfolio", id);
        
        PortfolioDto updated = portfolioService.update(id, request);
        
        log.info("Successfully updated portfolio", 
            new Object[]{"portfolioId", id});
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Retrieve all portfolios for a user.
     * 
     * @param userId User identifier
     * @return List of portfolios
     */
    @GetMapping
    @Operation(summary = "Get portfolios for user")
    public ResponseEntity<List<PortfolioDto>> listPortfolios(
            @RequestParam Long userId) {
        
        log.info("REST: GET /portfolios - Listing portfolios for user: {}", userId);
        
        List<PortfolioDto> portfolios = portfolioService.findByUserId(userId);
        
        log.debug("Retrieved {} portfolios for user {}", 
            portfolios.size(), userId);
        
        return ResponseEntity.ok(portfolios);
    }
    
    /**
     * Delete a portfolio.
     * 
     * @param id Portfolio identifier
     * @return No content response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete portfolio")
    @ApiResponse(responseCode = "204", description = "Portfolio deleted")
    @ApiResponse(responseCode = "404", description = "Portfolio not found")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
        log.info("REST: DELETE /portfolios/{}", id);
        
        portfolioService.delete(id);
        
        log.info("Successfully deleted portfolio", 
            new Object[]{"portfolioId", id});
        
        return ResponseEntity.noContent().build();
    }
}
