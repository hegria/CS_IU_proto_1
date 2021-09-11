package com.example.CS_IU_proto_1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

public class StartScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        PrefManager pm = new PrefManager(this);
        GuideLine guideLine = new GuideLine(this);
        ImageButton btnMeasure = findViewById(R.id.btnMeasure);
        ImageButton btnGuide = findViewById(R.id.btnGuide);

        if(pm.isFirstTimeLaunch())
            guideLine.gl1();

        btnMeasure.setOnClickListener(l -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        btnGuide.setOnClickListener(l -> {
            Intent intent = new Intent(this, GuideSlide.class);
            startActivity(intent);
        });
    }
}