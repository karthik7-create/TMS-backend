package com.taskmanager.mapper;

import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskMapper Unit Tests")
class TaskMapperTest {

    // ==================== toDto Tests ====================

    @Nested
    @DisplayName("toDto()")
    class ToDtoTests {

        @Test
        @DisplayName("Should map all fields from entity to DTO including user name")
        void toDto_WithUser() {
            User user = User.builder()
                    .id(1L)
                    .fullName("Jane Doe")
                    .email("jane@example.com")
                    .build();

            LocalDateTime now = LocalDateTime.now();
            Task task = Task.builder()
                    .id(10L)
                    .title("My Task")
                    .description("Some description")
                    .status(TaskStatus.IN_PROGRESS)
                    .createdAt(now)
                    .user(user)
                    .build();

            TaskDto dto = TaskMapper.toDto(task);

            assertEquals(10L, dto.getId());
            assertEquals("My Task", dto.getTitle());
            assertEquals("Some description", dto.getDescription());
            assertEquals(TaskStatus.IN_PROGRESS, dto.getStatus());
            assertEquals(now, dto.getCreatedAt());
            assertEquals("Jane Doe", dto.getUserName());
        }

        @Test
        @DisplayName("Should set userName to null when task has no user")
        void toDto_WithoutUser() {
            Task task = Task.builder()
                    .id(5L)
                    .title("Orphan Task")
                    .description("No user assigned")
                    .status(TaskStatus.TODO)
                    .createdAt(LocalDateTime.now())
                    .user(null)
                    .build();

            TaskDto dto = TaskMapper.toDto(task);

            assertEquals(5L, dto.getId());
            assertEquals("Orphan Task", dto.getTitle());
            assertNull(dto.getUserName());
        }
    }

    // ==================== toEntity Tests ====================

    @Nested
    @DisplayName("toEntity()")
    class ToEntityTests {

        @Test
        @DisplayName("Should map title, description, and status from DTO to entity")
        void toEntity_MapsCorrectFields() {
            TaskDto dto = TaskDto.builder()
                    .id(99L) // should NOT be mapped
                    .title("New Task")
                    .description("New Description")
                    .status(TaskStatus.DONE)
                    .createdAt(LocalDateTime.now()) // should NOT be mapped
                    .userName("Someone") // should NOT be mapped
                    .build();

            Task entity = TaskMapper.toEntity(dto);

            assertNull(entity.getId(), "ID should not be mapped to entity");
            assertEquals("New Task", entity.getTitle());
            assertEquals("New Description", entity.getDescription());
            assertEquals(TaskStatus.DONE, entity.getStatus());
            assertNull(entity.getCreatedAt(), "createdAt should not be mapped");
            assertNull(entity.getUser(), "user should not be mapped");
        }
    }

    // ==================== updateEntity Tests ====================

    @Nested
    @DisplayName("updateEntity()")
    class UpdateEntityTests {

        @Test
        @DisplayName("Should update existing entity fields from DTO")
        void updateEntity_UpdatesFields() {
            Task existingTask = Task.builder()
                    .id(1L)
                    .title("Old Title")
                    .description("Old Description")
                    .status(TaskStatus.TODO)
                    .createdAt(LocalDateTime.now())
                    .build();

            TaskDto updateDto = TaskDto.builder()
                    .title("New Title")
                    .description("New Description")
                    .status(TaskStatus.IN_PROGRESS)
                    .build();

            TaskMapper.updateEntity(existingTask, updateDto);

            assertEquals(1L, existingTask.getId(), "ID should remain unchanged");
            assertEquals("New Title", existingTask.getTitle());
            assertEquals("New Description", existingTask.getDescription());
            assertEquals(TaskStatus.IN_PROGRESS, existingTask.getStatus());
        }
    }
}
