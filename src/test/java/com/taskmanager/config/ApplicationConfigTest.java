package com.taskmanager.config;

import com.taskmanager.entity.User;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationConfig Unit Tests")
class ApplicationConfigTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationConfig applicationConfig;

    // ==================== UserDetailsService ====================

    @Nested
    @DisplayName("userDetailsService()")
    class UserDetailsServiceTests {

        @Test
        @DisplayName("Should return UserDetails when user exists")
        void userDetailsService_UserFound_ReturnsUserDetails() {
            // Arrange
            User user = User.builder()
                    .id(1L)
                    .fullName("John Doe")
                    .email("john@example.com")
                    .password("encodedPassword")
                    .build();
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

            UserDetailsService uds = applicationConfig.userDetailsService();

            // Act
            UserDetails result = uds.loadUserByUsername("john@example.com");

            // Assert
            assertNotNull(result);
            assertEquals("john@example.com", result.getUsername());
            verify(userRepository).findByEmail("john@example.com");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user does not exist")
        void userDetailsService_UserNotFound_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            UserDetailsService uds = applicationConfig.userDetailsService();

            // Act & Assert
            UsernameNotFoundException exception = assertThrows(
                    UsernameNotFoundException.class,
                    () -> uds.loadUserByUsername("unknown@example.com"));

            assertTrue(exception.getMessage().contains("unknown@example.com"));
            verify(userRepository).findByEmail("unknown@example.com");
        }
    }

    // ==================== PasswordEncoder ====================

    @Nested
    @DisplayName("passwordEncoder()")
    class PasswordEncoderTests {

        @Test
        @DisplayName("Should return BCryptPasswordEncoder instance")
        void passwordEncoder_ReturnsBCrypt() {
            // Act
            PasswordEncoder encoder = applicationConfig.passwordEncoder();

            // Assert
            assertNotNull(encoder);
            assertInstanceOf(BCryptPasswordEncoder.class, encoder);
        }

        @Test
        @DisplayName("BCryptPasswordEncoder should encode and match passwords")
        void passwordEncoder_EncodesAndMatches() {
            // Arrange
            PasswordEncoder encoder = applicationConfig.passwordEncoder();

            // Act
            String encoded = encoder.encode("password123");

            // Assert
            assertTrue(encoder.matches("password123", encoded));
            assertFalse(encoder.matches("wrongPassword", encoded));
        }
    }

    // ==================== AuthenticationProvider ====================

    @Nested
    @DisplayName("authenticationProvider()")
    class AuthenticationProviderTests {

        @Test
        @DisplayName("Should return a non-null AuthenticationProvider")
        void authenticationProvider_ReturnsProvider() {
            // Act
            AuthenticationProvider provider = applicationConfig.authenticationProvider();

            // Assert
            assertNotNull(provider);
        }
    }
}
