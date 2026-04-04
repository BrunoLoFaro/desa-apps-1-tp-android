package com.example.myapplication.util;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public final class ToolbarHelper {

    private ToolbarHelper() {
    }

    public static void setupBackToolbar(AppCompatActivity activity, MaterialToolbar toolbar) {
        activity.setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> activity.finish());
    }
}
