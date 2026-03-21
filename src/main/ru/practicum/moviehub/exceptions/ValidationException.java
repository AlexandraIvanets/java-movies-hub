package ru.practicum.moviehub.exceptions;

import java.util.List;

public class ValidationException extends IllegalArgumentException {
    private final List<String> details;

    public ValidationException(List<String> details) {
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}
