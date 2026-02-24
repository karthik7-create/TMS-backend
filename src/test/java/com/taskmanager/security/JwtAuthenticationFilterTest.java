package com.taskmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== No Auth Header ====================

    @Nested
    @DisplayName("When Authorization header is missing")
    class NoAuthHeader {

        @Test
        @DisplayName("Should pass through filter chain when no Authorization header")
        void doFilterInternal_NoHeader_PassesThrough() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn(null);

            // Act
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).extractUsername(anyString());
        }

        @Test
        @DisplayName("Should pass through filter chain when header does not start with Bearer")
        void doFilterInternal_NonBearerHeader_PassesThrough() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Basic some-credentials");

            // Act
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtService, never()).extractUsername(anyString());
        }
    }

    // ==================== Valid JWT Scenarios ====================

    @Nested
    @DisplayName("When Authorization header has valid Bearer token")
    class ValidBearerToken {

        @Test
        @DisplayName("Should authenticate user when JWT is valid and no existing authentication")
        void doFilterInternal_ValidToken_SetsAuthentication() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
            when(jwtService.extractUsername("valid.jwt.token")).thenReturn("john@example.com");

            UserDetails userDetails = mock(UserDetails.class);
            when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());
            when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid("valid.jwt.token", userDetails)).thenReturn(true);

            // Act
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(jwtService).extractUsername("valid.jwt.token");
            verify(userDetailsService).loadUserByUsername("john@example.com");
            verify(jwtService).isTokenValid("valid.jwt.token", userDetails);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should NOT set authentication when JWT is invalid")
        void doFilterInternal_InvalidToken_DoesNotSetAuth() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid.jwt.token");
            when(jwtService.extractUsername("invalid.jwt.token")).thenReturn("john@example.com");

            UserDetails userDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid("invalid.jwt.token", userDetails)).thenReturn(false);

            // Act
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should skip authentication when user is already authenticated")
        void doFilterInternal_AlreadyAuthenticated_Skips() throws ServletException, IOException {
            // Arrange — pre-set an authentication in SecurityContext
            Authentication existingAuth = mock(Authentication.class);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(existingAuth);
            SecurityContextHolder.setContext(securityContext);

            when(request.getHeader("Authorization")).thenReturn("Bearer some.jwt.token");
            when(jwtService.extractUsername("some.jwt.token")).thenReturn("john@example.com");

            // Act
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Assert — should NOT load user details since already authenticated
            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge cases and exception handling")
    class EdgeCases {

        @Test
        @DisplayName("Should skip authentication when extractUsername returns null")
        void doFilterInternal_NullUsername_Skips() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer some.jwt.token");
            when(jwtService.extractUsername("some.jwt.token")).thenReturn(null);

            // Act
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Assert
            verify(filterChain).doFilter(request, response);
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should catch exception and continue filter chain when JWT parsing fails")
        void doFilterInternal_ExceptionThrown_ContinuesChain() throws ServletException, IOException {
            // Arrange
            when(request.getHeader("Authorization")).thenReturn("Bearer malformed.token");
            when(jwtService.extractUsername("malformed.token"))
                    .thenThrow(new RuntimeException("Malformed JWT"));

            // Act
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // Assert — filter chain should still be called despite exception
            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }
}
