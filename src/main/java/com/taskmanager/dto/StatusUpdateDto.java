package com.taskmanager.dto;

import com.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for updating only the status of a Task via PATCH endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateDto {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}



