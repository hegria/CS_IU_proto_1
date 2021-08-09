package com.example.CS_IU_proto_1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ResultActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    GLSurfaceView glView;
    Bitmap image;

    ArrayList<Ellipse> ellipses;
    ArrayList<DrawEllipse> drawEllipses;

    DrawText drawText;

    float[] projMX;
    float[] viewMX;

    int width = 1;
    int height = 1;

    int count;
    float dia;


    boolean isBusy = false;

    TextView textCont;
    TextView textAvgdia;

    BackgroundImage backgroundImage;

    ExecutorService worker;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        worker = Executors.newSingleThreadExecutor();
        setContentView(R.layout.activity_result);
        // Image랑 Ellipse를 받아내고, 이를 다시 그려내야함.
        // 그려내는 부분에서 차라리 Ellipse를 평면에 정사영 시키는 편이 낫지 않을까?
        // Background 같은경우도 새로운 자료형을 만들어내야함( Image를 Bitmap을 통해서 그려낼 수 있는
        Intent intent = getIntent();
        ellipses = intent.getParcelableArrayListExtra("Ellipse");
        projMX = intent.getFloatArrayExtra("projMat");
        viewMX = intent.getFloatArrayExtra("viewMat");
        byte[] byteArray = getIntent().getByteArrayExtra("image");
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

        glView.setOnTouchListener((View view, MotionEvent event) -> {
            float xPx, yPx;
            int screenWidth, screenHeight;
            xPx = event.getX();
            yPx = event.getY();
            screenWidth = glView.getMeasuredWidth();
            screenHeight = glView.getMeasuredHeight();


            float x = 2.0f * xPx / screenWidth - 1.0f;
            float y = 1.0f - 2.0f * yPx / screenHeight;
            worker.execute(()->{
                float minDistanceSq = Float.MAX_VALUE;
                int idx = -1;
                int i = 0;
                float[] point;
                for(Ellipse ellipse : ellipses){
                    point = new float[]{ellipse.resultprivot[0], ellipse.resultprivot[1]};
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

            return false;
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
        drawEllipses = new ArrayList<>();
        drawText = new DrawText();
        drawText.setTexture(width,height);
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

                drawEllipses.clear();
                count = 0;
                dia = 0;

                for (Ellipse ellipse : ellipses) {
                    DrawEllipse drawEllipse = new DrawEllipse();
                    drawEllipse.setContour(ellipse);
                    drawText.setEllipses(ellipse);
                    drawEllipses.add(drawEllipse);
                    if (ellipse.istoggled) {
                        count++;
                        dia += ellipse.size;
                    }
                }
                dia /= count;
                drawText.setTexture(width, height);
                runOnUiThread(() -> {
                    textCont.setText(String.format("개수 : %d개", count));
                    textAvgdia.setText(String.format("평균 직경 : %.1fcm", dia));
                });
                isBusy = false;
            });

        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        backgroundImage.draw();

        for (DrawEllipse drawellipse: drawEllipses) {
            drawellipse.draw(viewMX,projMX);
        }
        drawText.draw();
    }
}
