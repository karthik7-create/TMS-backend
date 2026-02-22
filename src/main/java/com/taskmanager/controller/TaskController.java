package com.taskmanager.controller;

import com.taskmanager.dto.StatusUpdateDto;
import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for Task management operations.
 * All business logic is delegated to the {@link TaskService}.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final TaskService taskService;

    /**
     * Creates a new task.
     *
     * @param taskDto the task data
     * @return the created task with 201 status
     */
    @PostMapping
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody TaskDto taskDto) {
        log.info("POST /api/tasks - Creating task");
        TaskDto createdTask = taskService.createTask(taskDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    /**
     * Retrieves a paginated list of tasks with optional filtering and search.
     *
     * @param page   page number (0-indexed, default 0)
     * @param size   page size (default 10)
     * @param status optional status filter
     * @param search optional case-insensitive title search
     * @return paginated response with task data and metadata
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) String search) {

        log.info("GET /api/tasks - page: {}, size: {}, status: {}, search: {}", page, size, status, search);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TaskDto> taskPage = taskService.getAllTasks(status, search, pageable);

        Map<String, Object> response = buildPageResponse(taskPage);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single task by its ID.
     *
     * @param id the task ID
     * @return the task data
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long id) {
        log.info("GET /api/tasks/{} - Fetching task", id);
        TaskDto task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    /**
     * Fully updates an existing task.
     *
     * @param id      the task ID
     * @param taskDto the updated task data
     * @return the updated task
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskDto> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskDto taskDto) {

        log.info("PUT /api/tasks/{} - Updating task", id);
        TaskDto updatedTask = taskService.updateTask(id, taskDto);
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Updates only the status of an existing task.
     *
     * @param id              the task ID
     * @param statusUpdateDto the new status
     * @return the updated task
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDto> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto statusUpdateDto) {

        log.info("PATCH /api/tasks/{}/status - Updating status to {}", id, statusUpdateDto.getStatus());
        TaskDto updatedTask = taskService.updateTaskStatus(id, statusUpdateDto.getStatus());
        return ResponseEntity.ok(updatedTask);
    }

    /**
     * Deletes a task by its ID.
     *
     * @param id the task ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        log.info("DELETE /api/tasks/{} - Deleting task", id);
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Builds a structured pagination response wrapping the page data.
     */
    private Map<String, Object> buildPageResponse(Page<TaskDto> taskPage) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", taskPage.getContent());
        response.put("currentPage", taskPage.getNumber());
        response.put("totalElements", taskPage.getTotalElements());
        response.put("totalPages", taskPage.getTotalPages());
        response.put("size", taskPage.getSize());
        response.put("last", taskPage.isLast());
        response.put("first", taskPage.isFirst());
        return response;
    }
}
