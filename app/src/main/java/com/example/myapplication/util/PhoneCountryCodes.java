package com.example.myapplication.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Registry of supported phone country codes with digit-length rules and ISO codes.
 * All validation rules live here — do not hardcode lengths or codes elsewhere.
 */
public class PhoneCountryCodes {

    public static final List<PhoneCountryCode> ALL = Collections.unmodifiableList(Arrays.asList(
            new PhoneCountryCode("+54",  "Argentina",       "AR", 10, 10),
            new PhoneCountryCode("+49",  "Alemania",        "DE",  3, 12),
            new PhoneCountryCode("+591", "Bolivia",         "BO",  8,  8),
            new PhoneCountryCode("+55",  "Brasil",          "BR", 10, 11),
            new PhoneCountryCode("+56",  "Chile",           "CL",  8,  9),
            new PhoneCountryCode("+57",  "Colombia",        "CO", 10, 10),
            new PhoneCountryCode("+593", "Ecuador",         "EC",  9,  9),
            new PhoneCountryCode("+34",  "España",          "ES",  9,  9),
            new PhoneCountryCode("+1",   "EE.UU./Canadá",  "US", 10, 10),
            new PhoneCountryCode("+33",  "Francia",         "FR",  9,  9),
            new PhoneCountryCode("+39",  "Italia",          "IT",  6, 11),
            new PhoneCountryCode("+52",  "México",          "MX", 10, 10),
            new PhoneCountryCode("+595", "Paraguay",        "PY",  9,  9),
            new PhoneCountryCode("+51",  "Perú",            "PE",  9,  9),
            new PhoneCountryCode("+351", "Portugal",        "PT",  9,  9),
            new PhoneCountryCode("+44",  "Reino Unido",     "GB", 10, 10),
            new PhoneCountryCode("+598", "Uruguay",         "UY",  8,  9),
            new PhoneCountryCode("+58",  "Venezuela",       "VE", 10, 10)
    ));

    /** Returns codes sorted longest-first so prefix matching doesn't give false positives. */
    public static List<PhoneCountryCode> sortedByCodeLength() {
        List<PhoneCountryCode> sorted = new ArrayList<>(ALL);
        sorted.sort((a, b) -> b.getCode().length() - a.getCode().length());
        return sorted;
    }

    public static PhoneCountryCode findByCode(String code) {
        if (code == null) return null;
        for (PhoneCountryCode cc : ALL) {
            if (cc.getCode().equals(code)) return cc;
        }
        return null;
    }
}
