package com.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.StatusUpdateDto;
import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.security.JwtAuthenticationFilter;
import com.taskmanager.security.JwtService;
import com.taskmanager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters for unit testing
@DisplayName("TaskController Unit Tests")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private TaskDto sampleTaskDto;

    @BeforeEach
    void setUp() {
        sampleTaskDto = TaskDto.builder()
                .id(1L)
                .title("Sample Task")
                .description("Sample Description")
                .status(TaskStatus.TODO)
                .createdAt(LocalDateTime.of(2026, 2, 23, 10, 0, 0))
                .userName("John Doe")
                .build();
    }

    // ==================== POST /api/tasks ====================

    @Nested
    @DisplayName("POST /api/tasks")
    class CreateTaskEndpoint {

        @Test
        @DisplayName("Should return 201 Created with valid task data")
        void createTask_Success() throws Exception {
            when(taskService.createTask(any(TaskDto.class))).thenReturn(sampleTaskDto);

            TaskDto requestBody = TaskDto.builder()
                    .title("Sample Task")
                    .description("Sample Description")
                    .status(TaskStatus.TODO)
                    .build();

            mockMvc.perform(post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Sample Task"))
                    .andExpect(jsonPath("$.status").value("TODO"));

            verify(taskService).createTask(any(TaskDto.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for missing title")
        void createTask_MissingTitle() throws Exception {
            TaskDto invalidDto = TaskDto.builder()
                    .description("No title")
                    .status(TaskStatus.TODO)
                    .build();

            mockMvc.perform(post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any());
        }
    }

    // ==================== GET /api/tasks ====================

    @Nested
    @DisplayName("GET /api/tasks")
    class GetAllTasksEndpoint {

        @Test
        @DisplayName("Should return 200 OK with paginated response")
        void getAllTasks_Success() throws Exception {
            Page<TaskDto> page = new PageImpl<>(List.of(sampleTaskDto));
            when(taskService.getAllTasks(any(), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/tasks")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].title").value("Sample Task"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("Should pass status filter to service")
        void getAllTasks_WithStatusFilter() throws Exception {
            Page<TaskDto> page = new PageImpl<>(List.of(sampleTaskDto));
            when(taskService.getAllTasks(eq(TaskStatus.TODO), any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/tasks")
                    .param("status", "TODO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    // ==================== GET /api/tasks/{id} ====================

    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetTaskByIdEndpoint {

        @Test
        @DisplayName("Should return 200 OK for existing task")
        void getTaskById_Success() throws Exception {
            when(taskService.getTaskById(1L)).thenReturn(sampleTaskDto);

            mockMvc.perform(get("/api/tasks/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Sample Task"));
        }

        @Test
        @DisplayName("Should return 404 Not Found for non-existent task")
        void getTaskById_NotFound() throws Exception {
            when(taskService.getTaskById(99L))
                    .thenThrow(new ResourceNotFoundException("Task not found with id: 99"));

            mockMvc.perform(get("/api/tasks/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== PUT /api/tasks/{id} ====================

    @Nested
    @DisplayName("PUT /api/tasks/{id}")
    class UpdateTaskEndpoint {

        @Test
        @DisplayName("Should return 200 OK with updated task")
        void updateTask_Success() throws Exception {
            TaskDto updatedDto = TaskDto.builder()
                    .id(1L)
                    .title("Updated Task")
                    .description("Updated Description")
                    .status(TaskStatus.IN_PROGRESS)
                    .userName("John Doe")
                    .build();

            when(taskService.updateTask(eq(1L), any(TaskDto.class))).thenReturn(updatedDto);

            TaskDto requestBody = TaskDto.builder()
                    .title("Updated Task")
                    .description("Updated Description")
                    .status(TaskStatus.IN_PROGRESS)
                    .build();

            mockMvc.perform(put("/api/tasks/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Task"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating non-existent task")
        void updateTask_NotFound() throws Exception {
            when(taskService.updateTask(eq(99L), any(TaskDto.class)))
                    .thenThrow(new ResourceNotFoundException("Task not found with id: 99"));

            TaskDto requestBody = TaskDto.builder()
                    .title("Updated Task")
                    .description("Updated Description")
                    .status(TaskStatus.IN_PROGRESS)
                    .build();

            mockMvc.perform(put("/api/tasks/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== PATCH /api/tasks/{id}/status ====================

    @Nested
    @DisplayName("PATCH /api/tasks/{id}/status")
    class UpdateTaskStatusEndpoint {

        @Test
        @DisplayName("Should return 200 OK with updated status")
        void updateTaskStatus_Success() throws Exception {
            TaskDto statusUpdatedDto = TaskDto.builder()
                    .id(1L)
                    .title("Sample Task")
                    .status(TaskStatus.DONE)
                    .build();

            when(taskService.updateTaskStatus(1L, TaskStatus.DONE)).thenReturn(statusUpdatedDto);

            StatusUpdateDto requestBody = new StatusUpdateDto(TaskStatus.DONE);

            mockMvc.perform(patch("/api/tasks/1/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when updating status of non-existent task")
        void updateTaskStatus_NotFound() throws Exception {
            when(taskService.updateTaskStatus(eq(99L), any(TaskStatus.class)))
                    .thenThrow(new ResourceNotFoundException("Task not found with id: 99"));

            StatusUpdateDto requestBody = new StatusUpdateDto(TaskStatus.DONE);

            mockMvc.perform(patch("/api/tasks/99/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== DELETE /api/tasks/{id} ====================

    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTaskEndpoint {

        @Test
        @DisplayName("Should return 204 No Content on successful delete")
        void deleteTask_Success() throws Exception {
            doNothing().when(taskService).deleteTask(1L);

            mockMvc.perform(delete("/api/tasks/1"))
                    .andExpect(status().isNoContent());

            verify(taskService).deleteTask(1L);
        }

        @Test
        @DisplayName("Should return 404 Not Found for non-existent task")
        void deleteTask_NotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Task not found with id: 99"))
                    .when(taskService).deleteTask(99L);

            mockMvc.perform(delete("/api/tasks/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== GET /api/tasks with search ====================

    @Nested
    @DisplayName("GET /api/tasks with search")
    class GetAllTasksWithSearchEndpoint {

        @Test
        @DisplayName("Should pass search parameter to service")
        void getAllTasks_WithSearchParam() throws Exception {
            Page<TaskDto> page = new PageImpl<>(List.of(sampleTaskDto));
            when(taskService.getAllTasks(any(), eq("urgent"), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/tasks")
                    .param("search", "urgent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());

            verify(taskService).getAllTasks(any(), eq("urgent"), any(Pageable.class));
        }

        @Test
        @DisplayName("Should pass both status and search parameters")
        void getAllTasks_WithStatusAndSearch() throws Exception {
            Page<TaskDto> page = new PageImpl<>(List.of(sampleTaskDto));
            when(taskService.getAllTasks(eq(TaskStatus.IN_PROGRESS), eq("test"), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/tasks")
                    .param("status", "IN_PROGRESS")
                    .param("search", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }
}
