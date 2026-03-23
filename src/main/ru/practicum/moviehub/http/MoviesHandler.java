package ru.practicum.moviehub.http;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.exceptions.ValidationException;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

class MoviesHandler extends BaseHttpHandler {

    public MoviesHandler(MoviesStore store) {
        super(store);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        Endpoint endpoint = getEndpoint(ex.getRequestURI(), ex.getRequestMethod());
        switch (endpoint) {
            case GET_MOVIES -> sendJson(ex, 200, getMoviesJson());
            case POST_MOVIES -> addMovie(ex);
            case GET_MOVIE_BY_ID -> getMovieById(ex);
            case DELETE_MOVIE_BY_ID -> deleteMovieById(ex);
            case GET_MOVIES_BY_YEAR -> getMoviesByYear(ex);
            case UNKNOWN -> sendJson(ex, 405, gson.toJson(new ErrorResponse("Неподдерживаемый метод")));
        }
    }

    private Endpoint getEndpoint(URI requestURI, String requestMethod) {
        String requestPath = requestURI.getPath();
        String[] paths = requestPath.split("/");

        switch (requestMethod) {
            case "GET":
                if (paths.length == 2) {
                    if (requestURI.getQuery() != null) {
                        return Endpoint.GET_MOVIES_BY_YEAR;
                    }
                    return Endpoint.GET_MOVIES;
                }
                if (paths.length == 3) {
                    return Endpoint.GET_MOVIE_BY_ID;
                }
                return Endpoint.UNKNOWN;
            case "POST":
                if (paths.length == 2) {
                    return Endpoint.POST_MOVIES;
                }
                return Endpoint.UNKNOWN;
            case "DELETE":
                if (paths.length == 3) {
                    return Endpoint.DELETE_MOVIE_BY_ID;
                }
                return Endpoint.UNKNOWN;
            default:
                return Endpoint.UNKNOWN;
        }
    }

    private String getMoviesJson() {
        return gson.toJson(store.getMovies());
    }

    private void addMovie(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            sendJson(ex, 415, gson.toJson(new ErrorResponse("Неправильный Content-Type")));
            return;
        }

        try (InputStream inputStream = ex.getRequestBody()) {
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonElement jsonElement = JsonParser.parseString(body);

            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("тело не является JSON-объектом");
            }
            JsonObject json = jsonElement.getAsJsonObject();

            if (!hasNonNull(json, "title") || !hasNonNull(json, "year")) {
                throw new JsonSyntaxException("отсутствуют необходимые поля");
            }

            String title = json.get("title").getAsString();
            int year = json.get("year").getAsInt();

            Movie movie = store.saveMovie(new Movie(title, year));
            sendJson(ex, 201, gson.toJson(movie));

        } catch (ValidationException e) {
            sendJson(ex, 422, gson.toJson(
                    new ErrorResponse("Ошибка валидации", e.getDetails().toArray(new String[0]))));
        } catch (JsonSyntaxException e) {
            sendJson(ex, 400, gson.toJson(
                    new ErrorResponse("Некорректный JSON", new String[]{e.getMessage()})));
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(
                    new ErrorResponse("Некорректный JSON", new String[]{"неверный тип year"})));
        }
    }

    private boolean hasNonNull(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull();
    }

    private void getMovieById(HttpExchange ex) throws IOException {
        try {
            int id = Integer.parseInt(ex.getRequestURI().getPath().split("/")[2]);
            Movie movie = store.getMovieByID(id).orElseThrow(() -> new NoSuchElementException("Фильм с id=" + id + " не найден"));
            sendJson(ex, 200, gson.toJson(movie));
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(
                    new ErrorResponse("Некорректный ID", new String[]{"Неверный тип"})));
        } catch (NoSuchElementException e) {
            sendJson(ex, 404, gson.toJson(
                    new ErrorResponse("Некорректный ID", new String[]{"Фильм по этому ID не найден"})));
        }
    }

    private void deleteMovieById(HttpExchange ex) throws IOException {
        try {
            int id = Integer.parseInt(ex.getRequestURI().getPath().split("/")[2]);
            store.deleteMovieByID(id);
            sendNoContent(ex, 204);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(
                    new ErrorResponse("Некорректный ID", new String[]{"Неверный тип"})));
        } catch (NoSuchElementException e) {
            sendJson(ex, 404, gson.toJson(
                    new ErrorResponse("Некорректный ID", new String[]{"Фильм по этому ID не найден"})));
        }
    }

    private void getMoviesByYear(HttpExchange ex) throws IOException {
        try {
            Optional<String> yearOptional = getQueryParam(ex, "year");
            String yearString = yearOptional.orElseThrow(() -> new IllegalArgumentException("Некорректный параметр запроса — 'year'"));
            int year = Integer.parseInt(yearString);

            if (year < 1888 || year > (LocalDate.now().getYear() + 1)) {
                throw new IllegalArgumentException("Год должен быть между 1888 и " + (LocalDate.now().getYear() + 1));
            }
            sendJson(ex, 200, gson.toJson(store.getMoviesByYear(year)));

        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(
                    new ErrorResponse("Некорректный параметр запроса — 'year'", new String[]{"Неверный тип"})));
        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, gson.toJson(
                    new ErrorResponse("Некорректный параметр запроса — 'year'", new String[]{e.getMessage()})));
        }
    }

    private Optional<String> getQueryParam(HttpExchange ex, String param) {
        String query = ex.getRequestURI().getQuery();
        return Arrays.stream(query.split("&"))
                .map(pair -> pair.split("=", 2))
                .filter(parts -> parts.length == 2 && parts[0].equals(param))
                .map(parts -> parts[1])
                .findFirst();
    }
}