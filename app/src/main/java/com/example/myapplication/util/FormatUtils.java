package com.example.myapplication.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class FormatUtils {

    private FormatUtils() {}

    public static String formatPrice(double price, String currency) {
        if (price <= 0) return "Gratis";
        String symbol = "ARS".equals(currency) ? "$" : (currency != null ? currency + " " : "");

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "AR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        df.setGroupingUsed(true);
        df.setMaximumFractionDigits(0);
        df.setMinimumFractionDigits(0);
        return symbol + df.format(price);
    }

    /**
     * Parsea un precio formateado para AR (ej: "$16.000" o "16.000") a double (16000).
     * Soporta separador de miles '.' y separador decimal ','.
     */
    public static Double parsePriceToDouble(String formattedPrice) {
        if (formattedPrice == null) return null;
        String s = formattedPrice.trim();
        if (s.isEmpty()) return null;
        // Remover símbolo y espacios (ej: "$", "ARS ")
        s = s.replace("$", "").trim();
        // Quitar separadores de miles y normalizar decimal
        s = s.replace(".", "");
        s = s.replace(",", ".");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
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
        String value = iso.replace("T", " ");
        if (value.length() >= 16) return value.substring(0, 16);
        return value;
    }

    public static String formatDate(String iso) {
        if (iso == null || iso.length() < 10) return iso != null ? iso : "";
        return iso.substring(0, 10);
    }

    /** Extrae solo la hora "HH:mm" de un ISO timestamp. */
    public static String extractTime(String iso) {
        if (iso == null) return "";
        String s = formatStartTime(iso);
        return s.length() >= 16 ? s.substring(11, 16) : "";
    }

    /**
     * Convierte un timestamp ISO a formato local con zona horaria UTC+3.
     * Ejemplo entrada: "2024-01-15T14:30:00Z"
     * Ejemplo salida: "15/01/2024 17:30" (UTC+3)
     */
    public static String formatStartTimeWithUTC3(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            // Parsear el ISO timestamp en UTC
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(iso);
            
            if (date == null) {
                return formatStartTime(iso);
            }
            
            // Convertir a UTC+3
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("es", "AR"));
            outputFormat.setTimeZone(TimeZone.getTimeZone("UTC+03:00"));
            return outputFormat.format(date);
        } catch (Exception e) {
            // Si hay error en el parsing, retornar el formato simple
            return formatStartTime(iso);
        }
    }
}
