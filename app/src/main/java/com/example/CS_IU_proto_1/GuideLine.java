package com.example.CS_IU_proto_1;

import android.app.Activity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

public class GuideLine{

    public Activity activity;
    public GuideLine(Activity _activity){
        this.activity = _activity;
    }

    //안내 화면
    public void gl0(){
        TextView text0 = activity.findViewById(R.id.gl1_text);
        TextView text1 = activity.findViewById(R.id.gl1_text2);
        ImageButton btn1 = activity.findViewById(R.id.btnMeasure);
        ImageButton btn2 = activity.findViewById(R.id.btnGuide);
        ConstraintLayout textLayout = activity.findViewById(R.id.gl1_layout2);

        btn1.setEnabled(false);
        btn2.setEnabled(false);
        text0.setText("어서오세요. 사용법을 간단하게 안내해드리겠습니다.");
        text1.setText("터치하여 다음으로 넘어가기");
        Animation anim_fadeOut;
        anim_fadeOut = AnimationUtils.loadAnimation(activity.getApplicationContext(),R.anim.fade_out);
        textLayout.setVisibility(View.VISIBLE);
        textLayout.startAnimation(anim_fadeOut);

    }

    //메인 화면에서 Measure 선택
    public void gl1(){
        ImageView circle = activity.findViewById(R.id.gl1_circle);
        TextView text0 = activity.findViewById(R.id.gl1_text);
        TextView text1 = activity.findViewById(R.id.gl1_text2);
        ImageButton btn1 = activity.findViewById(R.id.btnMeasure);
        ImageButton btn2 = activity.findViewById(R.id.btnGuide);
        ConstraintLayout guideLayout1 = activity.findViewById(R.id.gl1_layout);

        text1.setVisibility(View.GONE);
        btn1.setEnabled(true);
        circle.setVisibility(View.VISIBLE);
        text0.setText("목재 직경을 측정하기 위해 Measure을 선택하세요");
        btn2.setBackgroundResource(R.drawable.round_rect_background3);
        guideLayout1.setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), R.color.gl1_color));
    }

    //녹화 버튼 누르기
    public void gl2(){
        TextView text2 = activity.findViewById(R.id.gl_text);
        text2.setText("녹화 버튼을 눌러 스캔을 시작합니다.");
        text2.setVisibility(View.VISIBLE);
    }

    //촬영 화면에서 양옆으로 스마트폰 움직이고 정지 버튼 누르기
    public void gl3(){
        TextView text2 = activity.findViewById(R.id.gl_text);
        TextView text3 = activity.findViewById(R.id.layout_text1);
        TextView text4 = activity.findViewById(R.id.gl1_text2);
        ConstraintLayout guideLayout2 = activity.findViewById(R.id.gl_Layout);
        ImageView gifImg = activity.findViewById(R.id.gl_image);

        text2.setVisibility(View.GONE);
        text3.setText("하얀 점이 많이 인식되도록 좌우로 천천히 움직인 후 정지 버튼을 눌러주세요.");
        text4.setText("터치하여 닫기");
        GlideDrawableImageViewTarget gifImage = new GlideDrawableImageViewTarget(gifImg);
        Glide.with(activity).load(R.drawable.moving_image).into(gifImage);
        guideLayout2.setVisibility(View.VISIBLE);
    }

    //화면 터치하기
    public void gl4(){
        TextView text2 = activity.findViewById(R.id.gl_text);
        ConstraintLayout guideLayout2 = activity.findViewById(R.id.gl_Layout);

        guideLayout2.setVisibility(View.GONE);
        text2.setText("하얀 점들을 터치해주시기 바랍니다.\n(실패할 경우, 버튼을 눌러 점들을 인식하는 과정부터 다시 해주세요.)");
        text2.setVisibility(View.VISIBLE);
    }

    //캡쳐 버튼 누르기
    public void gl5(){
        TextView text2 = activity.findViewById(R.id.gl_text);
        text2.setText("캡쳐 버튼을 눌러 해당 화면을 자세히 다룹니다.");
    }
}
