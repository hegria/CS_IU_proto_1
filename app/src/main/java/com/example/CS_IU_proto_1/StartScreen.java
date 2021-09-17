package com.example.CS_IU_proto_1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.TextView;

public class StartScreen extends AppCompatActivity {

    PrefManager pm;
    GuideLine guideLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        pm = new PrefManager(this);
        guideLine = new GuideLine(this);
        ImageButton btnMeasure = findViewById(R.id.btnMeasure);
        ImageButton btnGuide = findViewById(R.id.btnGuide);

        if(pm.isFirstTimeLaunch())
            guideLine.gl0();

        btnMeasure.setOnClickListener(l -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        btnGuide.setOnClickListener(l -> {
            Intent intent = new Intent(this, GuideSlide.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(pm.isFirstTimeLaunch() && event.getAction() == MotionEvent.ACTION_UP){
            guideLine.gl1();
        }
        return super.onTouchEvent(event);
    }
}