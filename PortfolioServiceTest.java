package com.portfolio.service;

import com.portfolio.service.PortfolioService;
import com.portfolio.dto.PortfolioDto;
import com.portfolio.exception.ResourceNotFoundException;
import com.portfolio.model.ErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PortfolioService.
 * 
 * Demonstrates:
 * - Testing service layer logic
 * - Following Arrange-Act-Assert pattern
 * - Mocking dependencies
 * - Testing both success and failure cases
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {
    
    @Mock
    private PortfolioRepository repository;
    
    @Mock
    private ValidationFramework validator;
    
    @Mock
    private AuditLogger auditLogger;
    
    @InjectMocks
    private PortfolioService service;
    
    @Test
    @DisplayName("Should return portfolio when ID exists")
    void findById_WhenExists_ReturnsPortfolio() {
        // Arrange
        Long portfolioId = 1L;
        Portfolio expected = new Portfolio();
        expected.setId(portfolioId);
        expected.setName("Test Portfolio");
        expected.setUserId(100L);
        
        when(repository.findById(portfolioId)).thenReturn(Optional.of(expected));
        
        // Act
        PortfolioDto result = service.findById(portfolioId);
        
        // Assert
        assertNotNull(result);
        assertEquals(portfolioId, result.getId());
        assertEquals("Test Portfolio", result.getName());
        verify(repository).findById(portfolioId);
        verify(auditLogger).logRead("Portfolio", portfolioId);
    }
    
    @Test
    @DisplayName("Should throw ResourceNotFoundException when portfolio not found")
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
    @DisplayName("Should create portfolio with valid request")
    void create_WithValidRequest_CreatesPortfolio() {
        // Arrange
        PortfolioCreateRequest request = new PortfolioCreateRequest();
        request.setName("New Portfolio");
        request.setUserId(100L);
        request.setRiskProfile(RiskProfile.MEDIUM);
        
        Portfolio created = new Portfolio();
        created.setId(1L);
        created.setName(request.getName());
        created.setUserId(request.getUserId());
        
        when(repository.save(any(Portfolio.class))).thenReturn(created);
        
        // Act
        PortfolioDto result = service.create(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New Portfolio", result.getName());
        verify(validator).validate(request);
        verify(repository).save(any(Portfolio.class));
        verify(auditLogger).logCreate(eq("Portfolio"), eq(1L), anyString());
    }
    
    @Test
    @DisplayName("Should throw exception when validation fails")
    void create_WithInvalidRequest_ThrowsException() {
        // Arrange
        PortfolioCreateRequest request = new PortfolioCreateRequest();
        request.setName(""); // Invalid: empty name
        
        doThrow(new IllegalArgumentException("Name is required"))
            .when(validator).validate(request);
        
        // Act & Assert
        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.create(request)
        );
        
        assertEquals(ErrorCodes.VALIDATION_FAILED, exception.getErrorCode());
        verify(validator).validate(request);
        verify(repository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should delete portfolio successfully")
    void delete_WhenExists_DeletesPortfolio() {
        // Arrange
        Long portfolioId = 1L;
        when(repository.existsById(portfolioId)).thenReturn(true);
        
        // Act
        service.delete(portfolioId);
        
        // Assert
        verify(repository).deleteById(portfolioId);
        verify(auditLogger).logDelete(eq("Portfolio"), eq(portfolioId), anyString());
    }
    
    @Test
    @DisplayName("Should throw exception when deleting non-existent portfolio")
    void delete_WhenNotExists_ThrowsException() {
        // Arrange
        Long portfolioId = 999L;
        when(repository.existsById(portfolioId)).thenReturn(false);
        
        // Act & Assert
        assertThrows(
            ResourceNotFoundException.class,
            () -> service.delete(portfolioId)
        );
        
        verify(repository, never()).deleteById(any());
    }
}
