package com.example.myapplication.util;

import java.util.Locale;

public final class FormatUtils {

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
}
