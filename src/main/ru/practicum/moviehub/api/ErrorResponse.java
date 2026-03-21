package ru.practicum.moviehub.api;

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
}