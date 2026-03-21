package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private final Map<Integer, Movie> store;
    private int nextId = 0;

    public MoviesStore() {
        this.store = new LinkedHashMap<>();
    }

    public Movie saveMovie(String title, int year) {
        Movie movie = new Movie(title, year);
        int id = ++nextId;
        movie.setId(id);
        store.put(id, movie);
        return movie;
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(store.values());
    }

    public Optional<Movie> getMovieByID(int id) {
        return Optional.ofNullable(store.get(id));
    }

    public void deleteMovieByID(int id) {
        if (store.remove(id) == null) {
            throw new NoSuchElementException();
        }
    }

    public List<Movie> getMoviesByYear(int year) {
        return store.values().stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    private void resetId() {
        nextId = 0;
    }

    public void clear() {
        store.clear();
        resetId();
    }

    @Override
    public String toString() {
        return "MoviesStore{" +
                "store=" + store +
                ", nextId=" + nextId +
                '}';
    }
}