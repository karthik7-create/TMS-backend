package com.taskmanager.service.impl;

import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.mapper.TaskMapper;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TaskSpecification;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link TaskService} providing CRUD, pagination,
 * filtering, and search functionality for Tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TaskDto createTask(TaskDto taskDto) {
        log.info("Creating new task with title: {}", taskDto.getTitle());
        Task task = TaskMapper.toEntity(taskDto);

        // Assign the currently authenticated user to the task
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        task.setUser(user);

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with id: {}", savedTask.getId());
        return TaskMapper.toDto(savedTask);
    }

    @Override
    public TaskDto getTaskById(Long id) {
        log.info("Fetching task with id: {}", id);
        Task task = findTaskOrThrow(id);
        return TaskMapper.toDto(task);
    }

    @Override
    public Page<TaskDto> getAllTasks(TaskStatus status, String search, Pageable pageable) {
        log.info("Fetching tasks - status: {}, search: {}, page: {}", status, search, pageable.getPageNumber());

        Specification<Task> spec = buildSpecification(status, search);
        Page<Task> taskPage = taskRepository.findAll(spec, pageable);

        return taskPage.map(TaskMapper::toDto);
    }

    @Override
    @Transactional
    public TaskDto updateTask(Long id, TaskDto taskDto) {
        log.info("Updating task with id: {}", id);
        Task existingTask = findTaskOrThrow(id);
        TaskMapper.updateEntity(existingTask, taskDto);
        Task updatedTask = taskRepository.save(existingTask);
        log.info("Task updated successfully with id: {}", updatedTask.getId());
        return TaskMapper.toDto(updatedTask);
    }

    @Override
    @Transactional
    public TaskDto updateTaskStatus(Long id, TaskStatus status) {
        log.info("Updating status of task id: {} to {}", id, status);
        Task task = findTaskOrThrow(id);
        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);
        log.info("Task status updated successfully for id: {}", updatedTask.getId());
        return TaskMapper.toDto(updatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        log.info("Deleting task with id: {}", id);
        Task task = findTaskOrThrow(id);
        taskRepository.delete(task);
        log.info("Task deleted successfully with id: {}", id);
    }

    /**
     * Finds a task by ID or throws ResourceNotFoundException.
     */
    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    /**
     * Builds a combined JPA Specification from optional status and search filters.
     */
    private Specification<Task> buildSpecification(TaskStatus status, String search) {
        Specification<Task> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and(TaskSpecification.hasStatus(status));
        }

        if (search != null && !search.trim().isEmpty()) {
            spec = spec.and(TaskSpecification.titleContains(search.trim()));
        }

        return spec;
    }
}
