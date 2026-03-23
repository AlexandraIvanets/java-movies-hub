package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final int PORT = 8080;
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, PORT);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        gson = new Gson();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp = createAndSendGetReq("/movies");
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertJsonCT(resp);

        List<Movie> bodyExpected = store.getMovies();
        List<Movie> bodyActual = parseMovieList(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsJsonArray() throws Exception {
        saveMoviesInStore();

        HttpResponse<String> resp = createAndSendGetReq("/movies");
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertJsonCT(resp);

        List<Movie> bodyExpected = store.getMovies();
        List<Movie> bodyActual = parseMovieList(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается JSON-массив");
    }

    @Test
    void postMovies_whenAllRight_returnsJsonObject() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":\"Онегин\",\"year\":1982}");
        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertJsonCT(resp);

        Movie bodyExpected = store.getMovieByID(1).orElseThrow();
        Movie bodyActual = gson.fromJson(resp.body().trim(), Movie.class);
        assertEquals(bodyExpected, bodyActual, "Ожидается объект movie");
    }

    @Test
    void postMovies_whenCTNotRight_returns415() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/html")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Онегин\",\"year\":1982}", StandardCharsets.UTF_8))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Неправильный Content-Type");
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается неправильный Content-Type");
    }

    @Test
    void postMovies_whenHappenedValidationException_returns422() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":\"\",\"year\":21000}");
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Ошибка валидации",
                new String[]{"название не должно быть пустым", "Год должен быть между 1888 и 2027"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается детализированное сообщение об ошибке");
    }

    @Test
    void postMovies_whenTitleTooLong_returns422() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq(
                "{\"title\":\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n\"," +
                        "\"year\":21000}");
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Ошибка валидации",
                new String[]{"название не должно быть длиннее 100 символов", "Год должен быть между 1888 и 2027"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается детализированное сообщение об ошибке");
    }

    @Test
    void postMovies_whenNotCorrectJsonBody_returns400() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq(
                "[{\"title\":\"Онегин\",\"year\":1982}]");

        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Некорректный JSON",
                new String[]{"тело не является JSON-объектом"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается детализированное сообщение об ошибке");
    }

    @Test
    void postMovies_whenThereAreNotRequiredFields_returns400() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":\"Онегин\"}");
        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Некорректный JSON",
                new String[]{"отсутствуют необходимые поля"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается детализированное сообщение об ошибке");
    }

    @Test
    void postMovies_whenThereAreNullRequiredFields_returns400() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":null,\"year\":1982}");
        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Некорректный JSON",
                new String[]{"отсутствуют необходимые поля"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается детализированное сообщение об ошибке");
    }

    @Test
    void postMovies_whenThereIsNotCorrectTypeOfYear_returns400() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":\"Онегин\",\"year\":\"III\"}");
        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Некорректный JSON",
                new String[]{"неверный тип year"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "неверный тип year");
    }

    @Test
    void getMovieByID_whenAllRight_returnsMovie() throws Exception {
        saveMoviesInStore();

        HttpResponse<String> resp = createAndSendGetReq("/movies/2");
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertJsonCT(resp);

        Movie bodyExpected = store.getMovieByID(2).orElseThrow();
        Movie bodyActual = gson.fromJson(resp.body().trim(), Movie.class);
        assertEquals(bodyExpected, bodyActual, "Ожидается возвращение объекта movie");
    }

    @Test
    void getMovieByID_whenNoMovieWithThisID_returns404() throws Exception {
        saveMoviesInStore();

        HttpResponse<String> resp = createAndSendGetReq("/movies/4");
        assertEquals(404, resp.statusCode(), "GET /movies должен вернуть 404");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Некорректный ID",
                new String[]{"Фильм по этому ID не найден"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается \"Фильм не найден\"");
    }

    @Test
    void getMovieByID_whenNotCorrectTypeOfID_returns400() throws Exception {
        saveMoviesInStore();

        HttpResponse<String> resp = createAndSendGetReq("/movies/I");
        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Некорректный ID",
                new String[]{"Неверный тип"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается ошибка \"Неверный тип\"");
    }

    @Test
    void deleteMovieByID_whenAllRight_returns204() throws Exception {
        saveMoviesInStore();

        HttpResponse<String> resp = createAndSendDeleteReq("/movies/1");
        assertEquals(204, resp.statusCode(), "DELETE /movies должен вернуть 204");
        assertJsonCT(resp);

        String bodyActual = resp.body().trim();
        assertEquals("", bodyActual, "Ожидается пустое тело");

        assertEquals(2, store.getMovies().size(), "Ожидается удаление фильма из store");
        assertTrue(store.getMovieByID(1).isEmpty(), "Фильм с id=1 должен быть удалён");
        assertTrue(store.getMovieByID(2).isPresent(), "Фильм с id=2 должен остаться");
        assertTrue(store.getMovieByID(3).isPresent(), "Фильм с id=3 должен остаться");
    }

    @Test
    void deleteMovieByID_whenNoMovieWithThisID_returns404() throws Exception {
        saveMoviesInStore();

        HttpResponse<String> resp = createAndSendDeleteReq("/movies/0");
        assertEquals(404, resp.statusCode(), "DELETE /movies должен вернуть 404");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Некорректный ID",
                new String[]{"Фильм по этому ID не найден"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Фильм по этому ID не найден");
    }

    @Test
    void deleteMovieByID_whenNotCorrectTypeOfID_returns400() throws Exception {
        saveMoviesInStore();

        HttpResponse<String> resp = createAndSendDeleteReq("/movies/m");
        assertEquals(400, resp.statusCode(), "DELETE /movies должен вернуть 400");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Некорректный ID",
                new String[]{"Неверный тип"});
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается ошибка \"Неверный тип\"");
    }

    @Test
    void getMoviesByYear_whenAllCorrect_returnsJsonArray() throws Exception {
        store.saveMovie(new Movie("Onegin", 1982));
        store.saveMovie(new Movie("Viy", 2006));
        store.saveMovie(new Movie("Gone with the wind", 1982));

        HttpResponse<String> resp = createAndSendGetReq("/movies?year=1982");
        assertEquals(200, resp.statusCode(), "GET /movies?year=1982 должен вернуть 200");
        assertJsonCT(resp);

        List<Movie> bodyExpected = store.getMoviesByYear(1982);
        List<Movie> bodyActual = parseMovieList(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается JSON-массив");
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyJsonArray() throws Exception {
        saveMoviesInStore();

        HttpResponse<String> resp = createAndSendGetReq("/movies?year=1983");
        assertEquals(200, resp.statusCode(), "GET /movies?year=1983 должен вернуть 200");
        assertJsonCT(resp);

        List<Movie> bodyExpected = store.getMoviesByYear(1983);
        List<Movie> bodyActual = parseMovieList(resp);
        assertEquals(bodyExpected, bodyActual, "Ожидается пустой JSON-массив");
    }

    @Test
    void getMovies_whenMethodIsNotCorrect_returns405() throws Exception {
        HttpResponse<String> resp = createAndSendDeleteReq("/movies");
        assertEquals(405, resp.statusCode(), "GET /movies должен вернуть 405");
        assertJsonCT(resp);

        ErrorResponse bodyExpected = new ErrorResponse("Неподдерживаемый метод");
        ErrorResponse bodyActual = parseErrorResponse(resp);
        assertEquals(bodyExpected, bodyActual, "Неподдерживаемый метод");
    }

    private HttpResponse<String> createAndSendGetReq(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        return client.send(req, responseBodyHandler);
    }

    private HttpResponse<String> createAndSendPostReq(String json)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        return client.send(req, responseBodyHandler);
    }

    private HttpResponse<String> createAndSendDeleteReq(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .DELETE()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        return client.send(req, responseBodyHandler);
    }

    private void assertJsonCT(HttpResponse<String> resp) {
        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType,
                "Content-Type должен содержать формат данных и кодировку");
    }

    private void saveMoviesInStore() {
        store.saveMovie(new Movie("Onegin", 1982));
        store.saveMovie(new Movie("Viy", 2006));
        store.saveMovie(new Movie("Gone with the wind", 1982));
    }

    private List<Movie> parseMovieList(HttpResponse<String> resp) {
        return gson.fromJson(resp.body().trim(), new ListOfMoviesTypeToken().getType());
    }

    private ErrorResponse parseErrorResponse(HttpResponse<String> resp) {
        return gson.fromJson(resp.body().trim(), ErrorResponse.class);
    }
}