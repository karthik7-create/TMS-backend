package com.taskmanager.repository;

import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Provides reusable JPA Specifications for filtering and searching Tasks.
 */
public final class TaskSpecification {

    private TaskSpecification() {
        // Prevent instantiation
    }

    /**
     * Filters tasks by their status.
     *
     * @param status the status to filter by
     * @return a Specification that matches tasks with the given status
     */
    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Searches tasks by title (case-insensitive, partial match).
     *
     * @param search the search term
     * @return a Specification that matches tasks whose title contains the search
     *         term
     */
    public static Specification<Task> titleContains(String search) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")),
                "%" + search.toLowerCase() + "%");
    }
}
