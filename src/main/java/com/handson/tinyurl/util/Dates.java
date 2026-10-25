
package com.handson.tinyurl.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class Dates {

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Jerusalem");

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM");

    private Dates() {
    }

    public static String getCurMonth() {
        return LocalDate.now(ZONE).format(MONTH_FORMAT);
    }
}
