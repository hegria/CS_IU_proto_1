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
import androidx.core.content.res.ResourcesCompat;

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
        ImageButton btn3 = activity.findViewById(R.id.btnRecord);
        ConstraintLayout guideLayout1 = activity.findViewById(R.id.gl1_layout);

        text1.setVisibility(View.GONE);
        btn1.setEnabled(true);
        circle.setVisibility(View.VISIBLE);
        text0.setText("목재 직경을 측정하기 위해 해당 버튼을 선택하세요");
        btn2.setBackgroundResource(R.drawable.round_rect_background3);
        btn3.setBackgroundResource(R.drawable.round_rect_background3);
        guideLayout1.setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), R.color.gl1_color));
    }

    //녹화 버튼 누르기
    public void gl2(){
        TextView text2 = activity.findViewById(R.id.gl_text);
        text2.setText("녹화 버튼을 눌러 스캔을 시작합니다.");
        text2.setVisibility(View.VISIBLE);
    }

    //촬영 화면에서 양옆으로 스마트폰 움직이고 정지 버튼 누르기
    public void gl3_1(){
        TextView text2 = activity.findViewById(R.id.gl_text);
        TextView text3 = activity.findViewById(R.id.layout_text1);
        TextView text4 = activity.findViewById(R.id.gl1_text2);
        ConstraintLayout guideLayout2 = activity.findViewById(R.id.gl_Layout);
        ImageView img = activity.findViewById(R.id.gl_image);

        text2.setVisibility(View.GONE);
        text3.setText("화면 스캔이 충분히 되면 전구가 노란색이 됩니다.");
        text4.setText("터치하여 다음으로 넘어가기");
        Glide.with(activity).load(R.drawable.light_on).into(img);
        guideLayout2.setVisibility(View.VISIBLE);
    }

    public void gl3_2(){
        TextView text3 = activity.findViewById(R.id.layout_text1);
        TextView text4 = activity.findViewById(R.id.gl1_text2);
        ImageView img = activity.findViewById(R.id.gl_image);
        text3.setText("노란색이 될 때까지 좌우로 천천히 움직인 후 정지 버튼을 눌러주세요.");
        text4.setText("터치하여 닫기");
        GlideDrawableImageViewTarget gifImage = new GlideDrawableImageViewTarget(img);
        Glide.with(activity).load(R.drawable.moving_image).into(gifImage);
    }

    //화면 터치하기
    public void gl4(){
        TextView text2 = activity.findViewById(R.id.gl_text);
        ConstraintLayout guideLayout2 = activity.findViewById(R.id.gl_Layout);

        guideLayout2.setVisibility(View.GONE);
        text2.setText("화면을 터치해주시기 바랍니다.\n(실패할 경우, 버튼을 눌러 화면을 스캔하는 과정부터 다시 해주세요.)");
        text2.setVisibility(View.VISIBLE);
    }

    //실시간 측정 화면
    public void gl5_1(){
        TextView text2 = activity.findViewById(R.id.gl_text);
        TextView text3 = activity.findViewById(R.id.layout_text1);
        TextView text4 = activity.findViewById(R.id.gl1_text2);
        ConstraintLayout guideLayout2 = activity.findViewById(R.id.gl_Layout);
        ImageView img = activity.findViewById(R.id.gl_image);

        text2.setVisibility(View.GONE);
        text3.setText("더 정확한 측정을 위해 안내문에 따라 목재와의 거리를 조절해주세요");
        text4.setText("터치하여 다음으로 넘어가기");
        Glide.with(activity).load(R.drawable.too_close).into(img);
        guideLayout2.setVisibility(View.VISIBLE);
    }

    public void gl5_2(){
        TextView text3 = activity.findViewById(R.id.layout_text1);
        TextView text4 = activity.findViewById(R.id.gl1_text2);
        ImageView img = activity.findViewById(R.id.gl_image);
        text3.setText("캡쳐 버튼을 눌러 해당 화면을 자세히 다룰 수 있습니다.");
        text4.setText("터치하여 닫기");
        GlideDrawableImageViewTarget gifImage = new GlideDrawableImageViewTarget(img);
        Glide.with(activity).load(R.drawable.result_view).into(gifImage);
    }



    //스위치, Seek bar 조정하기
    public void gl6(){
        ConstraintLayout guideLayout3 = activity.findViewById(R.id.gl_layout3);
        TextView text3 = activity.findViewById(R.id.gl_text3);
        TextView text4 = activity.findViewById(R.id.gl_text4);
        TextView text5 = activity.findViewById(R.id.gl_text5);
        ImageView img = activity.findViewById(R.id.gl_image2);

        guideLayout3.setVisibility(View.VISIBLE);
        text3.setText("스위치와 조정바를 이용해 직경에 따라 목재를 분류할 수 있습니다.");
        text4.setText("터치하여 다음으로 넘어가기");
        text5.setText("1/8 분류하기");
        img.setImageResource(R.drawable.slide_img1);

        Glide.with(activity).load(R.drawable.resultview_img1).into(img);
    }

    //목재 제외하기
    public void gl7(){
        TextView text3 = activity.findViewById(R.id.gl_text3);
        TextView text5 = activity.findViewById(R.id.gl_text5);
        ImageView img = activity.findViewById(R.id.gl_image2);
        text3.setText("화면의 목재를 터치하여 전체 정보에서 제외하거나 다시 보이게 할 수 있습니다.");
        text5.setText("2/8 제외하기");
        Glide.with(activity).load(R.drawable.resultview_img2).into(img);
    }

    //목재 추가하기1
    public void gl8_1(){
        TextView text3 = activity.findViewById(R.id.gl_text3);
        TextView text5 = activity.findViewById(R.id.gl_text5);
        ImageView img = activity.findViewById(R.id.gl_image2);
        text3.setText("Edit 모드에서 Add 버튼을 눌러 정중앙에 목재를 추가한 후, 스크롤바로 크기를 조절하거나");
        text5.setText("3/8 추가하기");
        Glide.with(activity).load(R.drawable.resultview_img3).into(img);
    }

    //목재 추가하기2
    public void gl8_2(){
        TextView text3 = activity.findViewById(R.id.gl_text3);
        TextView text5 = activity.findViewById(R.id.gl_text5);
        ImageView img = activity.findViewById(R.id.gl_image2);
        text3.setText("화면을 터치하여 위치를 바꾼 뒤, Apply 버튼을 눌러 고정합니다.");
        text5.setText("4/8 추가하기");
        Glide.with(activity).load(R.drawable.resultview_img4).into(img);
    }

    //기존 목재 편집하기
    public void gl9(){
        TextView text3 = activity.findViewById(R.id.gl_text3);
        TextView text5 = activity.findViewById(R.id.gl_text5);
        ImageView img = activity.findViewById(R.id.gl_image2);
        text3.setText("기존 목재 또한 길게 터치하면 크기나 위치를 바꿀 수 있습니다.");
        text5.setText("5/8 편집하기");
        Glide.with(activity).load(R.drawable.resultview_img5).into(img);
    }

    //추가 정보 보기
    public void gl10_1(){
        TextView text3 = activity.findViewById(R.id.gl_text3);
        TextView text5 = activity.findViewById(R.id.gl_text5);
        ImageView img = activity.findViewById(R.id.gl_image2);
        text3.setText("해당 버튼을 눌러 추가 정보를 볼 수 있습니다.");
        text5.setText("6/8 추가 정보 보기");
        Glide.with(activity).load(R.drawable.resultview_img6).into(img);
    }

    //정보 저장하기1
    public void gl10_2(){
        TextView text3 = activity.findViewById(R.id.gl_text3);
        TextView text5 = activity.findViewById(R.id.gl_text5);
        ImageView img = activity.findViewById(R.id.gl_image2);
        text3.setText("빈칸을 모두 입력한 후 Save 버튼을 통해 저장할 수 있습니다.");
        text5.setText("7/8 저장하기");
        Glide.with(activity).load(R.drawable.resultview_img7).into(img);
    }

    //정보 저장하기2
    public void gl10_3(){
        TextView text4 = activity.findViewById(R.id.gl_text4);
        TextView text3 = activity.findViewById(R.id.gl_text3);
        TextView text5 = activity.findViewById(R.id.gl_text5);
        ImageView img = activity.findViewById(R.id.gl_image2);
        text3.setText("저장 정보는 여기서 확인 가능합니다.");
        text5.setText("8/8 저장하기");
        Glide.with(activity).load(R.drawable.resultview_img8).into(img);
        text4.setText("터치하여 닫기");
    }



    //가이드라인 종료
    public void gl11(){
        ConstraintLayout guideLayout3 = activity.findViewById(R.id.gl_layout3);
        guideLayout3.setVisibility(View.GONE);
    }
}
