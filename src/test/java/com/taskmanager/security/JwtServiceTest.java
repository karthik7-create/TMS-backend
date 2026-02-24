package com.taskmanager.security;

import com.taskmanager.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        // Use reflection to set the secret key (256-bit key encoded in Base64)
        String rawSecret = "ThisIsATestSecretKeyThatIsAtLeast256BitsLong!12345";
        String base64Secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());

        Field secretKeyField = JwtService.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        secretKeyField.set(jwtService, base64Secret);

        // Set expiration to 1 hour (3600000 ms)
        Field expirationField = JwtService.class.getDeclaredField("jwtExpiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtService, 3600000L);

        testUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .password("encodedPassword")
                .build();
    }

    // ==================== generateToken Tests ====================

    @Nested
    @DisplayName("generateToken()")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate a non-null, non-empty token")
        void generateToken_ReturnsToken() {
            String token = jwtService.generateToken(testUser);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("Should generate a token with three parts (header.payload.signature)")
        void generateToken_HasThreeParts() {
            String token = jwtService.generateToken(testUser);

            String[] parts = token.split("\\.");
            assertEquals(3, parts.length, "JWT should have 3 parts separated by dots");
        }
    }

    // ==================== extractUsername Tests ====================

    @Nested
    @DisplayName("extractUsername()")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Should extract the correct email as username")
        void extractUsername_ReturnsEmail() {
            String token = jwtService.generateToken(testUser);

            String username = jwtService.extractUsername(token);

            assertEquals("john@example.com", username);
        }
    }

    // ==================== isTokenValid Tests ====================

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValidTests {

        @Test
        @DisplayName("Should return true for a valid token with matching user")
        void isTokenValid_ValidToken() {
            String token = jwtService.generateToken(testUser);

            assertTrue(jwtService.isTokenValid(token, testUser));
        }

        @Test
        @DisplayName("Should return false when username does not match")
        void isTokenValid_WrongUser() {
            String token = jwtService.generateToken(testUser);

            User otherUser = User.builder()
                    .id(2L)
                    .fullName("Jane Doe")
                    .email("jane@example.com")
                    .password("encodedPassword")
                    .build();

            assertFalse(jwtService.isTokenValid(token, otherUser));
        }
    }

    // ==================== Token Expiration Tests ====================

    @Nested
    @DisplayName("Token Expiration")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should reject an expired token with ExpiredJwtException")
        void isTokenValid_ExpiredToken() throws Exception {
            // Set expiration to 0 ms (token expires immediately)
            Field expirationField = JwtService.class.getDeclaredField("jwtExpiration");
            expirationField.setAccessible(true);
            expirationField.set(jwtService, 0L);

            String token = jwtService.generateToken(testUser);

            // Small delay to ensure the token is expired
            Thread.sleep(100);

            // JJWT library throws ExpiredJwtException during token parsing,
            // which means the token is correctly rejected as expired
            assertThrows(ExpiredJwtException.class,
                    () -> jwtService.isTokenValid(token, testUser));
        }
    }

    // ==================== Extra Claims Tests ====================

    @Nested
    @DisplayName("generateToken() with extra claims")
    class ExtraClaimsTests {

        @Test
        @DisplayName("Should generate token with extra claims and extract them")
        void generateToken_WithExtraClaims() {
            java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
            extraClaims.put("role", "ADMIN");
            extraClaims.put("userId", 42);

            String token = jwtService.generateToken(extraClaims, testUser);

            assertNotNull(token);
            assertFalse(token.isEmpty());
            // Verify username still extractable
            assertEquals("john@example.com", jwtService.extractUsername(token));
        }

        @Test
        @DisplayName("Should extract custom claims via extractClaim")
        void extractClaim_CustomClaim() {
            java.util.Map<String, Object> extraClaims = new java.util.HashMap<>();
            extraClaims.put("role", "ADMIN");

            String token = jwtService.generateToken(extraClaims, testUser);

            String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
            assertEquals("ADMIN", role);
        }
    }

    // ==================== Malformed Token Tests ====================

    @Nested
    @DisplayName("Malformed/Invalid tokens")
    class MalformedTokenTests {

        @Test
        @DisplayName("Should throw exception for malformed JWT token")
        void extractUsername_MalformedToken_ThrowsException() {
            assertThrows(Exception.class,
                    () -> jwtService.extractUsername("not.a.valid.jwt"));
        }

        @Test
        @DisplayName("Should throw exception for tampered JWT signature")
        void isTokenValid_TamperedToken_ThrowsException() {
            String token = jwtService.generateToken(testUser);
            // Tamper with the signature by modifying the last character
            String tamperedToken = token.substring(0, token.length() - 1) + "X";

            assertThrows(Exception.class,
                    () -> jwtService.isTokenValid(tamperedToken, testUser));
        }
    }
}
