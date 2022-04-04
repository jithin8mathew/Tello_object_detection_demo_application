package com.example.demoapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton droneControlScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        droneControlScreen = findViewById(R.id.introImage);
        droneControlScreen.setOnClickListener(v -> {
            Intent droneControlScreenIntent = new Intent(MainActivity.this, droneController.class);
            startActivity(droneControlScreenIntent);
        });


    }
}