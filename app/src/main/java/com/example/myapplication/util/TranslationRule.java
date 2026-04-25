package com.example.myapplication.util;

import java.util.regex.Pattern;

class TranslationRule {
    final Pattern pattern;
    final int resId;

    TranslationRule(Pattern pattern, int resId) {
        this.pattern = pattern;
        this.resId = resId;
    }
}