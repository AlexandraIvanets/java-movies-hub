package ru.practicum.moviehub.model;

import ru.practicum.moviehub.exceptions.ValidationException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Movie {
    private int id;
    private String title;
    private int year;

    public Movie(String title, int year) {
        validateArguments(title, year);
        this.title = title;
        this.year = year;
    }

    private void validateArguments(String title, int year) {
        List<String> errors = new ArrayList<>();

        if (title == null || title.isBlank()) {
            errors.add("название не должно быть пустым");
        } else if (title.length() > 100) {
            errors.add("название не должно быть длиннее 100 символов");
        }
        if (year < 1888 || year > (LocalDate.now().getYear() + 1)) {
            errors.add("Год должен быть между 1888 и " + (LocalDate.now().getYear() + 1));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public int getYear() {
        return year;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", year=" + year +
                '}';
    }
}
