package com.homer.core.common.utils;

import com.homer.core.common.utils.validator.IValidator;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.TimeZone;

public class DefaultUtils {
    private DefaultUtils() {
    }

    public static Integer NORMAL_FETCH_COUNT = 20;
    public static Integer LOAD_FETCH_COUNT = 100;
    public static String DATE_FORMAT_STR = "yyyyMMdd";
    public static String TIME_FORMAT_STR = "HHmmss";
    public static String DATE_TIME_FORMAT_STR = "yyyyMMddHHmmss";

    public static final DateTimeFormatter TIME_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter();

    public static final SimpleDateFormat DATE_FORMAT() {
        return new SimpleDateFormat(DATE_FORMAT_STR);
    }

    public static final SimpleDateFormat DATE_FORMAT_UTC() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STR);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    public static final SimpleDateFormat DATETIME_FORMAT() {
        return new SimpleDateFormat(DATE_TIME_FORMAT_STR);
    }

    public static final SimpleDateFormat DATETIME_FORMAT_UTC() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT_STR);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    public static final SimpleDateFormat TIME_FORMAT_UTC() {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT_STR);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    public static final SimpleDateFormat TIME_FORMAT() {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT_STR);
        return sdf;
    }

    public static final SimpleDateFormat TIME_DATE_FORMAT() {
        return new SimpleDateFormat(DATE_TIME_FORMAT_STR);
    }

    public static final SimpleDateFormat ES_DATE_FORMAT() {
        return new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ");
    }

    public static final SimpleDateFormat FB_DATE_FORMAT() {
        return new SimpleDateFormat("MM/dd/yyyy");
    }

    public static final DateTimeFormatter FRIEDNLY_DATE_FORMAT() {
        return DateTimeFormatter.ISO_DATE_TIME;
    }

    public static final String friendlyFormat(Date date) {
        return FRIEDNLY_DATE_FORMAT().toFormat().format(date);
    }

    public static final IValidator<String> DATE_FORMAT_VALIDATOR = i -> {
        try {
            return DATE_FORMAT().parse(i);
        } catch (ParseException e) {
            return null;
        }
    };
    public static final IValidator<String> DATETIME_FORMAT_VALIDATOR = i -> {
        try {
            return DATETIME_FORMAT().parse(i);
        } catch (ParseException e) {
            return null;
        }
    };

    public static Date currentDateWithTimeUtc(String time) throws ParseException {
        return DATETIME_FORMAT_UTC().parse(DATE_FORMAT().format(new Date()) + time);
    }

    public static LocalTime convertTimeStringToLocalTime(String time) {
        return LocalTime.parse(time, TIME_FORMAT);
    }

    public static LocalTime convertTimeStampToLocalTime(Timestamp time) {
        return time.toLocalDateTime().toLocalTime();
    }

    public static Timestamp convertLocalTimeToTimeStamp(LocalTime time) {
        Timestamp t = new Timestamp(System.currentTimeMillis());
        LocalDateTime lct = time.atDate(t.toLocalDateTime().toLocalDate());
        return Timestamp.valueOf(lct);
    }

//    public static void main(String[] args) {
//        try {
//            LocalTime.parse()
//            System.out.println(convertTimeStringToLocalTime("101625"));
//            System.out.println(convertTimeStringToLocalTime("224523"));
//            Locale.setDefault(Locale.CANADA);
//            System.out.println(convertTimeStringToLocalTime("101625"));
//            System.out.println(convertTimeStringToLocalTime("224523"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

}
