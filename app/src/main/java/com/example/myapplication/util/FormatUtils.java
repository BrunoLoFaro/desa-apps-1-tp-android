package com.example.myapplication.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FormatUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm'hs'", new Locale("es", "ES"));

    private FormatUtils() {}

    public static String formatPrice(double price, String currency) {
        if (price <= 0) return "Gratis";
        String symbol = "ARS".equals(currency) ? "$" : (currency != null ? currency + " " : "");
        return symbol + String.format(Locale.US, "%.2f", price);
    }

    public static String formatShortDate(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return isoDate != null ? isoDate : "";
        try {
            String[] parts = isoDate.substring(0, 10).split("-");
            int month = Integer.parseInt(parts[1]);
            String[] months = {"Ene", "Feb", "Mar", "Abr", "May", "Jun",
                               "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
            return parts[2] + " " + months[month - 1];
        } catch (Exception e) {
            return isoDate.substring(0, Math.min(10, isoDate.length()));
        }
    }

    public static String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " min";
        int hours = minutes / 60;
        int remaining = minutes % 60;
        if (remaining == 0) return hours + (hours == 1 ? " hora" : " horas");
        return hours + " h " + remaining + " min";
    }

    public static String formatStartTime(String iso) {
        if (iso == null) return "";
        try {
            // Manejar tanto "2023-10-25T10:00" como "2023-10-25 10:00"
            String sanitizedIso = iso.replace(" ", "T");
            LocalDateTime dt = LocalDateTime.parse(sanitizedIso);
            String formatted = dt.format(DISPLAY_FORMATTER);
            // Capitalizar la primera letra (día de la semana)
            return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
        } catch (Exception e) {
            // Fallback simple si falla el parseo
            String value = iso.replace("T", " ");
            if (value.length() >= 16) return value.substring(0, 16);
            return value;
        }
    }

    public static String formatDate(String iso) {
        if (iso == null || iso.length() < 10) return iso != null ? iso : "";
        return iso.substring(0, 10);
    }
}
