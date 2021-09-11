package com.example.CS_IU_proto_1;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

public class GuideLine{

    public Activity activity;
    public GuideLine(Activity _activity){
        this.activity = _activity;
    }

    //메인 화면에서 Measure 선택 유도
    public void gl1(){
        ImageView circle = activity.findViewById(R.id.gl1_circle);
        ConstraintLayout layout = activity.findViewById(R.id.gl1_layout);
        circle.setVisibility(View.VISIBLE);
        layout.setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), R.color.gl1_color));
    }
}
