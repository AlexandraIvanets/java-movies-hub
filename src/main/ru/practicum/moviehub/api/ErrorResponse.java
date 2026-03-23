package ru.practicum.moviehub.api;

import java.util.Arrays;
import java.util.Objects;

public class ErrorResponse {
    private String error;
    private String[] details;

    public ErrorResponse(String error, String[] details) {
        this.error = error;
        this.details = details;
    }

    public ErrorResponse(String error) {
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return Objects.equals(error, that.error) && Arrays.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(error);
        result = 31 * result + Arrays.hashCode(details);
        return result;
    }
}