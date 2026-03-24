package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        TextView welcomeText = findViewById(R.id.welcome_text);
        // You can customize the welcome message here, e.g., by passing data via Intent
        welcomeText.setText(R.string.welcome_message);
    }
}
