package ru.practicum.moviehub.http;

import org.junit.jupiter.api.*;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final int PORT = 8080;
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, PORT);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
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
        String body = resp.body().trim();
        assertEquals("[]", body, "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsJsonArray() throws Exception {
        store.saveMovie("Onegin", 1982);
        store.saveMovie("Viy", 2006);

        HttpResponse<String> resp = createAndSendGetReq("/movies");
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertJsonCT(resp);

        String bodyExpected = "[{\"id\":1,\"title\":\"Onegin\",\"year\":1982}," +
                "{\"id\":2,\"title\":\"Viy\",\"year\":2006}]";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается JSON-массив");
    }

    @Test
    void postMovies_whenAllRight_returnsJsonObject() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":\"Онегин\",\"year\":1982}");
        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertJsonCT(resp);

        String bodyExpected = "{\"id\":1,\"title\":\"Онегин\",\"year\":1982}";
        String bodyActual = resp.body().trim();
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

        String bodyExpected = "{\"error\":\"Неправильный Content-Type\"}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается неправильный Content-Type");
    }

    @Test
    void postMovies_whenHappenedValidationException_returns422() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":\"\",\"year\":21000}");
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Ошибка валидации\",\"details\":[\"название не должно быть пустым\"," +
                "\"Год должен быть между 1888 и 2027\"]}";
        String bodyActual = resp.body().trim();
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

        String bodyExpected = "{\"error\":\"Ошибка валидации\",\"details\":[\"название не должно быть " +
                "длиннее 100 символов\",\"Год должен быть между 1888 и 2027\"]}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается детализированное сообщение об ошибке");
    }

    @Test
    void postMovies_whenNotCorrectJsonBody_returns400() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq(
                "[{\"title\":\"Онегин\",\"year\":1982}]");

        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Некорректный JSON\",\"details\":[\"тело не является JSON-объектом\"]}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается детализированное сообщение об ошибке");
    }

    @Test
    void postMovies_whenThereAreNotRequiredFields_returns400() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":\"Онегин\"}");
        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Некорректный JSON\",\"details\":[\"отсутствуют необходимые поля\"]}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual,
                "Ожидается детализированное сообщение об ошибке");
    }

    @Test
    void postMovies_whenThereAreNullRequiredFields_returns400() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":null,\"year\":1982}");
        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Некорректный JSON\",\"details\":[\"отсутствуют необходимые поля\"]}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual,
                "Ожидается детализированное сообщение об ошибке");
    }

    @Test
    void postMovies_whenThereIsNotCorrectTypeOfYear_returns400() throws Exception {
        HttpResponse<String> resp = createAndSendPostReq("{\"title\":\"Онегин\",\"year\":\"III\"}");
        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Некорректный JSON\",\"details\":[\"неверный тип year\"]}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "неверный тип year");
    }

    @Test
    void getMovieByID_whenAllRight_returnsMovie() throws Exception {
        store.saveMovie("Onegin", 1982);
        store.saveMovie("Viy", 2006);
        store.saveMovie("Gone with the wind", 1946);

        HttpResponse<String> resp = createAndSendGetReq("/movies/2");
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertJsonCT(resp);

        String bodyExpected = "{\"id\":2,\"title\":\"Viy\",\"year\":2006}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается возвращение объекта movie");
    }

    @Test
    void getMovieByID_whenNoMovieWithThisID_returns404() throws Exception {
        store.saveMovie("Onegin", 1982);
        store.saveMovie("Viy", 2006);
        store.saveMovie("Gone with the wind", 1946);

        HttpResponse<String> resp = createAndSendGetReq("/movies/4");
        assertEquals(404, resp.statusCode(), "GET /movies должен вернуть 404");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Некорректный ID\",\"details\":[\"Фильм по этому ID не найден\"]}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается \"Фильм не найден\"");
    }

    @Test
    void getMovieByID_whenNotCorrectTypeOfID_returns400() throws Exception {
        store.saveMovie("Onegin", 1982);
        store.saveMovie("Viy", 2006);
        store.saveMovie("Gone with the wind", 1946);

        HttpResponse<String> resp = createAndSendGetReq("/movies/I");
        assertEquals(400, resp.statusCode(), "GET /movies должен вернуть 400");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Некорректный ID\",\"details\":[\"Неверный тип\"]}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается {\"error\":\"Некорректный ID\",\"details\":[\"Неверный тип\"]}");
    }

    @Test
    void deleteMovieByID_whenAllRight_returns204() throws Exception {
        store.saveMovie("Onegin", 1982);
        store.saveMovie("Viy", 2006);
        store.saveMovie("Gone with the wind", 1946);

        HttpResponse<String> resp = createAndSendDeleteReq("/movies/1");
        assertEquals(204, resp.statusCode(), "DELETE /movies должен вернуть 204");
        assertJsonCT(resp);

        String bodyExpected = "";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается пустое тело");

        String storeExpected = "MoviesStore{store={2=Movie{id=2, title='Viy', year=2006}, " +
                "3=Movie{id=3, title='Gone with the wind', year=1946}}, nextId=3}";
        String storeActual = store.toString();
        assertEquals(storeExpected, storeActual, "Ожидается удаление фильма по индексу");
    }

    @Test
    void deleteMovieByID_whenNoMovieWithThisID_returns404() throws Exception {
        store.saveMovie("Onegin", 1982);
        store.saveMovie("Viy", 2006);
        store.saveMovie("Gone with the wind", 1946);

        HttpResponse<String> resp = createAndSendDeleteReq("/movies/0");
        assertEquals(404, resp.statusCode(), "DELETE /movies должен вернуть 404");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Некорректный ID\",\"details\":[\"Фильм по этому ID не найден\"]}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Фильм по этому ID не найден");
    }

    @Test
    void deleteMovieByID_whenNotCorrectTypeOfID_returns400() throws Exception {
        store.saveMovie("Onegin", 1982);
        store.saveMovie("Viy", 2006);
        store.saveMovie("Gone with the wind", 1946);

        HttpResponse<String> resp = createAndSendDeleteReq("/movies/m");
        assertEquals(400, resp.statusCode(), "DELETE /movies должен вернуть 400");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Некорректный ID\",\"details\":[\"Неверный тип\"]}";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается {\"error\":\"Некорректный ID\",\"details\":[\"Неверный тип\"]}");
    }

    @Test
    void getMoviesByYear_whenAllCorrect_returnsJsonArray() throws Exception {
        store.saveMovie("Onegin", 1982);
        store.saveMovie("Viy", 2006);
        store.saveMovie("Gone with the wind", 1982);

        HttpResponse<String> resp = createAndSendGetReq("/movies?year=1982");
        assertEquals(200, resp.statusCode(), "GET /movies?year=1982 должен вернуть 200");
        assertJsonCT(resp);

        String bodyExpected = "[{\"id\":1,\"title\":\"Onegin\",\"year\":1982}," +
                "{\"id\":3,\"title\":\"Gone with the wind\",\"year\":1982}]";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается JSON-массив");
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyJsonArray() throws Exception {
        store.saveMovie("Onegin", 1982);
        store.saveMovie("Viy", 2006);
        store.saveMovie("Gone with the wind", 1982);

        HttpResponse<String> resp = createAndSendGetReq("/movies?year=1983");
        assertEquals(200, resp.statusCode(), "GET /movies?year=1982 должен вернуть 200");
        assertJsonCT(resp);

        String bodyExpected = "[]";
        String bodyActual = resp.body().trim();
        assertEquals(bodyExpected, bodyActual, "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenMethodIsNotCorrect_returns405() throws Exception {
        HttpResponse<String> resp = createAndSendDeleteReq("/movies");
        assertEquals(405, resp.statusCode(), "GET /movies должен вернуть 405");
        assertJsonCT(resp);

        String bodyExpected = "{\"error\":\"Неподдерживаемый метод\"}";
        String bodyActual = resp.body().trim();
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
}