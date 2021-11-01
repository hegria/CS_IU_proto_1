package com.example.CS_IU_proto_1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
        ImageButton btnRecord = findViewById(R.id.btnRecord);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},100);
        }

        if(pm.isFirstTimeLaunch1())
            guideLine.gl0();

        btnMeasure.setOnClickListener(l -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        btnGuide.setOnClickListener(l -> {
            Intent intent = new Intent(this, GuideSlide.class);
            startActivity(intent);
        });

        btnRecord.setOnClickListener(l -> {
            Intent intent = new Intent(this, MyRecord.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(pm.isFirstTimeLaunch1() && event.getAction() == MotionEvent.ACTION_UP){
            guideLine.gl1();
        }
        return super.onTouchEvent(event);
    }
}