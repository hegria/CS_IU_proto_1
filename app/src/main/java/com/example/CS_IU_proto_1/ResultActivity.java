package com.example.CS_IU_proto_1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ResultActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    GLSurfaceView glView;
    Bitmap image;

    ArrayList<Ellipse> ellipses;

    DrawText drawText;

    float[] projMX;
    float[] viewMX;
    float[] cameratrans;

    int width = 1;
    int height = 1;

    int maxVal, minVal;

    float offset;

    //경계 값 (원래라면 각각 15, 30)
    final int b1 = 2;
    final int b2 = 3;

    boolean isBusy = false;
    boolean correctionmode = false;

    TextView textCont;
    TextView textAvgdia;
    Switch switch1, switch2, switch3;
    RangeSeekBar<Integer> seekBar;
    SeekBar seekBar2;
    Button button;
    Plane plane;
    Ellipse nowellipse;

    BackgroundImage backgroundImage;
    ExecutorService worker;
    EllipsePool ellipsePool;

    //가이드라인 진행 상태
    private enum Gl_State {Idle, Filtering, VisibilityControl, Adding1, Adding2}
    Gl_State gl_state = Gl_State.Idle;
    GuideLine guideLine;
    PrefManager pf;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        worker = Executors.newSingleThreadExecutor();
        setContentView(R.layout.activity_result);

        guideLine = new GuideLine(this);
        pf = new PrefManager(this);
        if(pf.isFirstTimeLaunch2()) {
            guideLine.gl6();
            gl_state = Gl_State.Filtering;
        }


        // Image랑 Ellipse를 받아내고, 이를 다시 그려내야함.
        // 그려내는 부분에서 차라리 Ellipse를 평면에 정사영 시키는 편이 낫지 않을까?
        // Background 같은경우도 새로운 자료형을 만들어내야함( Image를 Bitmap을 통해서 그려낼 수 있는
        Intent intent = getIntent();
        ellipses = intent.getParcelableArrayListExtra("Ellipse");
        plane = intent.getParcelableExtra("plane");
        projMX = intent.getFloatArrayExtra("projMat");
        viewMX = intent.getFloatArrayExtra("viewMat");
        cameratrans = intent.getFloatArrayExtra("cameratrans");
        offset = intent.getFloatExtra("offset",0);
        byte[] byteArray = intent.getByteArrayExtra("image");

        image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        Log.i("img",""+byteArray.length);
        glView = (GLSurfaceView) findViewById(R.id.subsurface);
        glView.setPreserveEGLContextOnPause(true);
        glView.setEGLContextClientVersion(2);
        glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glView.setRenderer(this);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        glView.setWillNotDraw(false);

        textCont = findViewById(R.id.text_logCount);
        textAvgdia = findViewById(R.id.text_avgDiameter);
        switch1 = findViewById(R.id.switch1);
        switch2 = findViewById(R.id.switch2);
        switch3 = findViewById(R.id.switch3);
        seekBar = findViewById(R.id.seekBar);
        seekBar2 = findViewById(R.id.seekBar2);
        seekBar2.setVisibility(View.INVISIBLE);
        button = findViewById(R.id.correction);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!correctionmode){
                    correctionmode = true;
                    Ray originray = Myutil.GenerateRay(glView.getMeasuredWidth()/2,glView.getMeasuredHeight()/2,glView.getMeasuredWidth(),glView.getMeasuredHeight(),projMX,viewMX,cameratrans);
                    nowellipse = new Ellipse(originray, plane,projMX,viewMX);
                    ellipses.add(nowellipse);
                    button.setText("Apply");
                    seekBar2.setVisibility(View.VISIBLE);

                }else{
                    correctionmode = false;
                    nowellipse = null;
                    button.setText("ADD");
                    seekBar2.setVisibility(View.INVISIBLE);
                }
                setText();
            }
        });


        glView.setOnTouchListener((View view, MotionEvent event) -> {

            if(pf.isFirstTimeLaunch2())
                return false;

            float xPx, yPx;
            int screenWidth, screenHeight;
            xPx = event.getX();
            yPx = event.getY();
            screenWidth = glView.getMeasuredWidth();
            screenHeight = glView.getMeasuredHeight();


            float x = 2.0f * xPx / screenWidth - 1.0f;
            float y = 1.0f - 2.0f * yPx / screenHeight;
            Log.i("xpx,ypx", Float.toString(xPx)+ Float.toString(yPx));

            if(!correctionmode){

                worker.execute(()->{
                    float minDistanceSq = Float.MAX_VALUE;
                    int idx = -1;
                    int i = 0;
                    float[] point;
                    for(Ellipse ellipse : ellipses){
                        point = new float[]{ellipse.resultpivot[0], ellipse.resultpivot[1]};
                        float distanceSq = (x-point[0])*(x-point[0]) + (y-point[1])*(y-point[1]);
                        Log.i("distance",""+distanceSq);
                        if(distanceSq<0.01f&& distanceSq<minDistanceSq){
                            idx = i;
                            minDistanceSq = distanceSq;
                        }
                        i++;
                    }
                    if(idx != -1){
                        ellipses.get(idx).istoggled = !ellipses.get(idx).istoggled;
                    }
                });
                setText();
            }else{
                //button으로 넘겨야함.
                Ray ray = Myutil.GenerateRay(xPx,yPx,screenWidth,screenHeight,projMX,viewMX,cameratrans);
                nowellipse.movepivot(ray,plane,projMX,viewMX);

            }
            return false;
        });

        switch1.setOnClickListener(l -> {
            if (switch1.isChecked()) {
                for(Ellipse ellipse : ellipses){
                    seekBar.setSelectedMinValue(b1);
                    if(ellipse.size < b1)
                        ellipse.istoggled = true;
                }
            } else {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size < b1)
                        ellipse.istoggled = false;
                }
            }
            setRange();
            setText();
        });

        switch2.setOnClickListener(l -> {
            if (switch2.isChecked()) {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size >= b1 && ellipse.size < b2)
                        ellipse.istoggled = true;
                }
            } else {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size >= b1 && ellipse.size < b2)
                        ellipse.istoggled = false;
                }
            }
            setRange();
            setText();
        });

        switch3.setOnClickListener(l -> {
            if (switch3.isChecked()) {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size >= b2)
                        ellipse.istoggled = true;
                }
            } else {
                for(Ellipse ellipse : ellipses){
                    if(ellipse.size >= b2)
                        ellipse.istoggled = false;
                }
            }
            setRange();
            setText();
        });

        maxVal = 0;
        minVal = 100;

        for(Ellipse ellipse : ellipses){
            if(ellipse.size < minVal)
                minVal = ellipse.size;
            if(ellipse.size > maxVal)
                maxVal = ellipse.size;
        }

        seekBar.setRangeValues(minVal, maxVal);
        seekBar.setOnRangeSeekBarChangeListener((bar, minValue, maxValue) -> {
            int selectedMinVal = (Integer)(bar.getSelectedMinValue());
            int selectedMaxVal = (Integer)(bar.getSelectedMaxValue());

            for(Ellipse ellipse : ellipses){
                ellipse.istoggled =
                        (ellipse.size >= selectedMinVal) && (ellipse.size <= selectedMaxVal);
            }
            setText();
        });
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(correctionmode){
                    nowellipse.changerad(seekBar.getProgress()/10f,plane);
                    setText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        backgroundImage = new BackgroundImage();
        backgroundImage.updatImage(image);
        drawText = new DrawText(offset);
        drawText.setTexture(width,height);
        ellipsePool = new EllipsePool(100);
        setText();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        GLES20.glViewport(0, 0, width, height);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(!isBusy) {
            isBusy =true;
            glView.queueEvent(() -> {
                ellipsePool.clear();
                drawText.clearEllipses();
                for (Ellipse ellipse : ellipses) {
                    if (ellipse.istoggled) {
                        if(ellipsePool.isFull())
                            ellipsePool.addEllipse(ellipse);
                        else
                            ellipsePool.setEllipse(ellipse);
                        drawText.setEllipses(ellipse);
                    }
                }
                drawText.setTexture(width, height);
                isBusy = false;
            });
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        backgroundImage.draw();

        for(int i = 0; i < ellipsePool.useCount; i++) {
            ellipsePool.drawEllipses.get(i).draw(viewMX, projMX,offset);
        }
        drawText.draw();
    }

    void setRange(){
        if(switch1.isChecked() && switch2.isChecked() && switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(maxVal);
        }else if(switch1.isChecked() && switch2.isChecked() && !switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(b2-1);
        }else if(switch1.isChecked() && !switch2.isChecked() && switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(maxVal);
        }else if(!switch1.isChecked() && switch2.isChecked() && switch3.isChecked()) {
            seekBar.setSelectedMinValue(b1);
            seekBar.setSelectedMaxValue(maxVal);
        }else if(switch1.isChecked() && !switch2.isChecked() && !switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(b1-1);
        }else if(!switch1.isChecked() && switch2.isChecked() && !switch3.isChecked()) {
            seekBar.setSelectedMinValue(b1);
            seekBar.setSelectedMaxValue(b2-1);
        }else if(!switch1.isChecked() && !switch2.isChecked() && switch3.isChecked()) {
            seekBar.setSelectedMinValue(b2);
            seekBar.setSelectedMaxValue(maxVal);
        }else if(!switch1.isChecked() && !switch2.isChecked() && !switch3.isChecked()) {
            seekBar.setSelectedMinValue(minVal);
            seekBar.setSelectedMaxValue(minVal);
        }
    }

    @SuppressLint("DefaultLocale")
    void setText(){
        int count = 0;
        float dia = 0;
        for (Ellipse ellipse : ellipses) {
            if(ellipse.istoggled){
                count++;
                dia += ellipse.size;
            }
        }
        if(count != 0)
            dia /= count;
        int finalCount = count;
        float finalDia = dia;
        runOnUiThread(() -> {
            textCont.setText(String.format("개수 : %d개", finalCount));
            textAvgdia.setText(String.format("평균 직경 : %.1fcm", finalDia));
        });
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            switch (gl_state){
                case Filtering:
                    guideLine.gl7();
                    gl_state = Gl_State.VisibilityControl;
                    break;
                case VisibilityControl:
                    guideLine.gl8_1();
                    gl_state = Gl_State.Adding1;
                    break;
                case Adding1:
                    guideLine.gl8_2();
                    gl_state = Gl_State.Adding2;
                    break;
                case Adding2:
                    guideLine.gl9();
                    pf.setFirstTimeLaunch2(false);
                    break;
            }
        }
        return true;
    }
}
