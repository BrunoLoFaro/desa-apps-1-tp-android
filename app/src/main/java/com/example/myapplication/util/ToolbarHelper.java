package com.example.myapplication.util;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.appbar.MaterialToolbar;

public final class ToolbarHelper {

    private ToolbarHelper() {
    }

    public static void setupBackToolbar(FragmentActivity activity, MaterialToolbar toolbar) {
        if (activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).setSupportActionBar(toolbar);
        }
        // El listener de navegación lo configura cada Fragment con navController.navigateUp()
    }
}
