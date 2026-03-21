package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

abstract class BaseHttpHandler implements HttpHandler {
    protected final MoviesStore store;

    public BaseHttpHandler(MoviesStore store) {
        this.store = store;
    }

    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream outputStream = ex.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex, int status) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, -1);
    }
}