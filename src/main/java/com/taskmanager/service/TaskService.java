package com.taskmanager.service;

import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface defining business operations for Task management.
 */
public interface TaskService {

    /**
     * Creates a new task.
     */
    TaskDto createTask(TaskDto taskDto);

    /**
     * Retrieves a single task by its ID.
     */
    TaskDto getTaskById(Long id);

    /**
     * Retrieves a paginated list of tasks with optional filtering and search.
     *
     * @param status   optional status filter
     * @param search   optional case-insensitive title search
     * @param pageable pagination parameters
     * @return a page of TaskDto results
     */
    Page<TaskDto> getAllTasks(TaskStatus status, String search, Pageable pageable);

    /**
     * Fully updates an existing task.
     */
    TaskDto updateTask(Long id, TaskDto taskDto);

    /**
     * Updates only the status of an existing task.
     */
    TaskDto updateTaskStatus(Long id, TaskStatus status);

    /**
     * Deletes a task by its ID.
     */
    void deleteTask(Long id);
}
