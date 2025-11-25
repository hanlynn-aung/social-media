package com.example.socialmedia.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private final String pattern;
    private final ZoneId zoneId;
    private final DateTimeFormatter formatter;

    // Default instance for static usage
    private static final DateTimeUtil DEFAULT_INSTANCE = new DateTimeUtilBuilder()
            .pattern("yyyy-MM-dd HH:mm:ss")
            .zoneId(ZoneId.systemDefault())
            .build();

    private DateTimeUtil(DateTimeUtilBuilder builder) {
        this.pattern = builder.pattern;
        this.zoneId = builder.zoneId;
        this.formatter = DateTimeFormatter.ofPattern(pattern).withZone(zoneId);
    }

    public static DateTimeUtilBuilder builder() {
        return new DateTimeUtilBuilder();
    }

    public String formatInstance(Instant instant) {
        if (instant == null) return null;
        return formatter.format(instant);
    }

    public String formatInstance(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(zoneId).format(formatter);
    }

    public Instant parseToInstantInstance(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(pattern))
                    .atZone(zoneId)
                    .toInstant();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing date string: " + dateString, e);
        }
    }

    public LocalDateTime toLocalDateTimeInstance(Instant instant) {
        if (instant == null) return null;
        return LocalDateTime.ofInstant(instant, zoneId);
    }

    public Instant toInstantInstance(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(zoneId).toInstant();
    }

    // Static wrappers calling the default instance
    public static String format(Instant instant) {
        return DEFAULT_INSTANCE.formatInstance(instant);
    }

    public static String format(LocalDateTime localDateTime) {
        return DEFAULT_INSTANCE.formatInstance(localDateTime);
    }

    public static Instant parseToInstant(String dateString) {
        return DEFAULT_INSTANCE.parseToInstantInstance(dateString);
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return DEFAULT_INSTANCE.toLocalDateTimeInstance(instant);
    }

    public static Instant toInstant(LocalDateTime localDateTime) {
        return DEFAULT_INSTANCE.toInstantInstance(localDateTime);
    }

    // Custom Builder
    public static class DateTimeUtilBuilder implements Builder<DateTimeUtil> {
        private String pattern = "yyyy-MM-dd HH:mm:ss";
        private ZoneId zoneId = ZoneId.systemDefault();

        public DateTimeUtilBuilder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public DateTimeUtilBuilder zoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        @Override
        public DateTimeUtil build() {
            return new DateTimeUtil(this);
        }
    }
}

