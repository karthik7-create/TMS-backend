package com.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.LoginRequest;
import com.taskmanager.dto.RegisterRequest;
import com.taskmanager.entity.User;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtAuthenticationFilter;
import com.taskmanager.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .password("encodedPassword")
                .build();
    }

    // ==================== POST /api/auth/register ====================

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("Should return 201 Created for new user registration")
        void register_Success() throws Exception {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

            RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt.token.here"))
                    .andExpect(jsonPath("$.fullName").value("John Doe"))
                    .andExpect(jsonPath("$.email").value("john@example.com"));

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should return 409 Conflict when email already exists")
        void register_DuplicateEmail() throws Exception {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.token").isEmpty());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for missing fields")
        void register_MissingFields() throws Exception {
            // Missing fullName and password
            String invalidJson = "{\"email\": \"john@example.com\"}";

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /api/auth/login ====================

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("Should return 200 OK with JWT for valid credentials")
        void login_Success() throws Exception {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null); // successful auth
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(testUser)).thenReturn("jwt.token.here");

            LoginRequest request = new LoginRequest("john@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt.token.here"))
                    .andExpect(jsonPath("$.fullName").value("John Doe"))
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized for bad credentials")
        void login_BadCredentials() throws Exception {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.token").isEmpty());
        }

        @Test
        @DisplayName("Should return 400 Bad Request for invalid email format")
        void login_InvalidEmail() throws Exception {
            LoginRequest request = new LoginRequest("not-an-email", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
