package com.taskmanager.service.impl;

import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl Unit Tests")
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User testUser;
    private Task testTask;
    private TaskDto testTaskDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .password("encodedPassword")
                .createdAt(LocalDateTime.now())
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .createdAt(LocalDateTime.now())
                .user(testUser)
                .build();

        testTaskDto = TaskDto.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .createdAt(LocalDateTime.now())
                .userName("John Doe")
                .build();
    }

    // ==================== createTask Tests ====================

    @Nested
    @DisplayName("createTask()")
    class CreateTaskTests {

        @BeforeEach
        void setUpSecurityContext() {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("john@example.com");
            SecurityContextHolder.setContext(securityContext);
        }

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should create task successfully and assign authenticated user")
        void createTask_Success() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);

            TaskDto result = taskService.createTask(testTaskDto);

            assertNotNull(result);
            assertEquals("Test Task", result.getTitle());
            assertEquals("Test Description", result.getDescription());
            assertEquals(TaskStatus.TODO, result.getStatus());
            assertEquals("John Doe", result.getUserName());

            verify(userRepository).findByEmail("john@example.com");
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void createTask_UserNotFound() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.createTask(testTaskDto));

            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    // ==================== getTaskById Tests ====================

    @Nested
    @DisplayName("getTaskById()")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Should return task DTO when task exists")
        void getTaskById_Success() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            TaskDto result = taskService.getTaskById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Test Task", result.getTitle());
            assertEquals(TaskStatus.TODO, result.getStatus());
            verify(taskRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task not found")
        void getTaskById_NotFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> taskService.getTaskById(99L));

            assertTrue(exception.getMessage().contains("99"));
            verify(taskRepository).findById(99L);
        }
    }

    // ==================== getAllTasks Tests ====================

    @Nested
    @DisplayName("getAllTasks()")
    class GetAllTasksTests {

        @Test
        @DisplayName("Should return paginated tasks without filters")
        void getAllTasks_NoFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(testTask), pageable, 1);

            when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);

            Page<TaskDto> result = taskService.getAllTasks(null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Test Task", result.getContent().get(0).getTitle());
            verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should return filtered tasks by status")
        void getAllTasks_WithStatusFilter() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(testTask), pageable, 1);

            when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);

            Page<TaskDto> result = taskService.getAllTasks(TaskStatus.TODO, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should return filtered tasks by search term")
        void getAllTasks_WithSearch() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(testTask), pageable, 1);

            when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);

            Page<TaskDto> result = taskService.getAllTasks(null, "Test", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should return empty page when no tasks match")
        void getAllTasks_EmptyResult() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

            Page<TaskDto> result = taskService.getAllTasks(null, null, pageable);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("Should ignore empty search string and not add search spec")
        void getAllTasks_EmptySearchString() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(testTask), pageable, 1);

            when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);

            Page<TaskDto> result = taskService.getAllTasks(null, "", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should ignore whitespace-only search string")
        void getAllTasks_WhitespaceOnlySearch() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(testTask), pageable, 1);

            when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);

            Page<TaskDto> result = taskService.getAllTasks(null, "   ", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("Should apply both status AND search filters when both provided")
        void getAllTasks_WithStatusAndSearch() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(List.of(testTask), pageable, 1);

            when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);

            Page<TaskDto> result = taskService.getAllTasks(TaskStatus.TODO, "Test", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        }
    }

    // ==================== updateTask Tests ====================

    @Nested
    @DisplayName("updateTask()")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should update task successfully")
        void updateTask_Success() {
            TaskDto updatedDto = TaskDto.builder()
                    .title("Updated Title")
                    .description("Updated Description")
                    .status(TaskStatus.IN_PROGRESS)
                    .build();

            Task updatedTask = Task.builder()
                    .id(1L)
                    .title("Updated Title")
                    .description("Updated Description")
                    .status(TaskStatus.IN_PROGRESS)
                    .user(testUser)
                    .build();

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

            TaskDto result = taskService.updateTask(1L, updatedDto);

            assertNotNull(result);
            assertEquals("Updated Title", result.getTitle());
            assertEquals("Updated Description", result.getDescription());
            assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());

            verify(taskRepository).findById(1L);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent task")
        void updateTask_NotFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.updateTask(99L, testTaskDto));

            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    // ==================== updateTaskStatus Tests ====================

    @Nested
    @DisplayName("updateTaskStatus()")
    class UpdateTaskStatusTests {

        @Test
        @DisplayName("Should update only the task status")
        void updateTaskStatus_Success() {
            Task statusUpdatedTask = Task.builder()
                    .id(1L)
                    .title("Test Task")
                    .description("Test Description")
                    .status(TaskStatus.DONE)
                    .user(testUser)
                    .build();

            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(statusUpdatedTask);

            TaskDto result = taskService.updateTaskStatus(1L, TaskStatus.DONE);

            assertNotNull(result);
            assertEquals(TaskStatus.DONE, result.getStatus());
            assertEquals("Test Task", result.getTitle()); // title unchanged

            verify(taskRepository).findById(1L);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when task not found")
        void updateTaskStatus_NotFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.updateTaskStatus(99L, TaskStatus.DONE));

            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    // ==================== deleteTask Tests ====================

    @Nested
    @DisplayName("deleteTask()")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should delete task successfully")
        void deleteTask_Success() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            doNothing().when(taskRepository).delete(testTask);

            assertDoesNotThrow(() -> taskService.deleteTask(1L));

            verify(taskRepository).findById(1L);
            verify(taskRepository).delete(testTask);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent task")
        void deleteTask_NotFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.deleteTask(99L));

            verify(taskRepository, never()).delete(any(Task.class));
        }
    }
}