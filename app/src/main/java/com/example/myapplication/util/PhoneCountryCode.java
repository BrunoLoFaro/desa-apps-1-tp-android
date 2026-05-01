package com.example.myapplication.util;

public class PhoneCountryCode {
    private final String code;
    private final String countryName;
    private final String isoCode;   // ISO 3166-1 alpha-2, used for flag emoji generation
    private final int minDigits;
    private final int maxDigits;
    private final String defaultPrefix; // pre-filled in the number field when this country is selected

    public PhoneCountryCode(String code, String countryName, String isoCode,
                            int minDigits, int maxDigits, String defaultPrefix) {
        this.code = code;
        this.countryName = countryName;
        this.isoCode = isoCode;
        this.minDigits = minDigits;
        this.maxDigits = maxDigits;
        this.defaultPrefix = defaultPrefix != null ? defaultPrefix : "";
    }

    public String getCode() { return code; }
    public String getCountryName() { return countryName; }
    public String getIsoCode() { return isoCode; }
    public int getMinDigits() { return minDigits; }
    public int getMaxDigits() { return maxDigits; }
    public String getDefaultPrefix() { return defaultPrefix; }

    /** Generates a Unicode flag emoji from the 2-letter ISO code. */
    public String getFlagEmoji() {
        if (isoCode == null || isoCode.length() != 2) return "";
        String upper = isoCode.toUpperCase();
        int first  = upper.codePointAt(0) - 'A' + 0x1F1E6;
        int second = upper.codePointAt(1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

    @Override
    public String toString() {
        return code + " " + countryName;
    }
}
