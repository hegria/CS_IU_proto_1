package com.example.CS_IU_proto_1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

public class StartScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        PrefManager prefManager = new PrefManager(getApplicationContext());
        if(prefManager.isFirstTimeLaunch()){
            prefManager.setFirstTimeLaunch(false);
            startActivity(new Intent(StartScreen.this, GuideActivity.class));
            finish();
        }

        ImageButton btnMeasure = findViewById(R.id.btnMeasure);
        ImageButton btnGuide = findViewById(R.id.btnGuide);

        btnMeasure.setOnClickListener(l -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        btnGuide.setOnClickListener(l -> {
            prefManager.setFirstTimeLaunch(true);
            Intent intent = new Intent(this, GuideActivity.class);
            intent.putExtra("type", "from_start_screen");
            startActivity(intent);
        });
    }
}