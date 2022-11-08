package com.homer.core.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateFormatUtil {
    public static LocalDateTime getLocalDateTime(String date, String pattern) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(date, formatter);
    }

    public static LocalDate getLocalDate(String date, String pattern) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDate.parse(date, formatter);
    }

    public static String toString(LocalDateTime dateTime, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(dateTime);
    }

    public static String toString(LocalDate dateTime, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(dateTime);
    }
}
