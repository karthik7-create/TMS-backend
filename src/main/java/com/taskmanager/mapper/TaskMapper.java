package com.taskmanager.mapper;

import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.Task;

/**
 * Utility class for mapping between Task entity and TaskDto.
 * Uses static methods to avoid unnecessary bean instantiation.
 */
public final class TaskMapper {

    private TaskMapper() {
        // Prevent instantiation
    }

    /**
     * Converts a Task entity to a TaskDto.
     *
     * @param task the entity to convert
     * @return the corresponding DTO
     */
    public static TaskDto toDto(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .build();
    }

    /**
     * Converts a TaskDto to a new Task entity.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity
     */
    public static Task toEntity(TaskDto dto) {
        return Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .build();
    }

    /**
     * Updates an existing Task entity with values from a TaskDto.
     *
     * @param task the entity to update
     * @param dto  the DTO containing updated values
     */
    public static void updateEntity(Task task, TaskDto dto) {
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
    }
}
