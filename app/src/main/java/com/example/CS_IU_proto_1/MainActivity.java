package com.example.CS_IU_proto_1;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

  private static final String TAG = "opencv";

  // state 1 => 제일 처음, 2 => pointcollection 시작, 3=> pointcolleted 끝(findsurface 시작) => 4 findsurface 진행중 5 Plane Find

  private enum State {
    Idle, PointCollecting, PointCollected, FindingSurface, FoundSurface, Capture
  }
  State state = State.Idle;

  // ARCORE 관련

  boolean installRequested = false;
  boolean isBusy = false;
  GLSurfaceView glView; // 띄우기 위한 View
  Session session; // ??
  Camera camera; // 그냥 카메라
  CameraConfig cameraConfig;
  Image image;
  Mat img;
  Plane plane;


  PointCloudRenderer pointCloudRenderer; // PointCloud그림
  PointCollector pointCollector; // 모을거임

  ExecutorService worker;
  ExecutorService findPlaneworker;
  FindPlaneTask findPlaneTask;

  SimpleDraw forDebugging; // 선택한 점 그리는거
  Background background; // background
  DrawText drawText;

  OpenCVJNI jni;

  // 컨투어 그리기 위한거(디버그용)
//  ArrayList<ContourForDraw> contourForDraws;

  EllipsePool ellipsePool;
  ArrayList<Contour> contours;
  ArrayList<Ellipse> ellipses;

  ImageButton recordButton;
  TextView txtCount;

  int width = 1, height = 1;
  float[] projMX = {1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f};
  float[] viewMX = {1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f};

  //앱종료시간체크
  long backKeyPressedTime;    //앱종료 위한 백버튼 누른시간

  //뒤로가기 2번하면 앱종료
  @Override
  public void onBackPressed() {
    //1번째 백버튼 클릭
    //초기화
    initAll();
    recordButton.setImageResource(R.drawable.for_record_button);
    recordButton.setVisibility(View.VISIBLE);

    if(System.currentTimeMillis()>backKeyPressedTime+2000){
      backKeyPressedTime = System.currentTimeMillis();
      Toast.makeText(this, "초기화 되었습니다.\n한번 더 눌러 앱 종료", Toast.LENGTH_SHORT).show();
    }
    //2번째 백버튼 클릭 (종료)
    else{
      AppFinish();
    }
  }

  //앱종료
  public void AppFinish(){
    finish();
    System.exit(0);
    android.os.Process.killProcess(android.os.Process.myPid());
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    worker = Executors.newSingleThreadExecutor();
    findPlaneworker = Executors.newSingleThreadExecutor();
    findPlaneTask = new FindPlaneTask();
    jni = new OpenCVJNI();
    findPlaneTask.setFindPlaneTaskListener(new FindPlaneTask.FindPlaneTaskListener() {
      @Override
      public void onSuccessTask(Plane _plane) {
        if(state != State.FindingSurface)
          return;
        runOnUiThread(() -> {
          Toast.makeText(MainActivity.this,"평면을 찾았습니다.",Toast.LENGTH_SHORT).show();
          recordButton.setImageResource(R.drawable.for_capture_button);
        });
        state = State.FoundSurface;
        plane = _plane;
      }

      @Override
      public void onFailTask() {
        if(state != State.FindingSurface)
          return;
        runOnUiThread(() -> Toast.makeText(MainActivity.this,"평면을 못 찾았습니다. 다시 시도하여 주세요.",Toast.LENGTH_SHORT).show());
        state = State.PointCollected;
      }
    });

    setContentView(R.layout.activity_main);
    glView = (GLSurfaceView) findViewById(R.id.surfaceView);
    glView.setPreserveEGLContextOnPause(true);
    glView.setEGLContextClientVersion(2);
    glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    glView.setRenderer(this);
    glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    glView.setWillNotDraw(false);

    txtCount = findViewById(R.id.txtCount);
    recordButton = findViewById(R.id.recordButton);

    recordButton.setOnClickListener(l -> {
      // collecting 시작하기 위해 버튼 누름
      if(state == State.Idle) {
        recordButton.setImageResource(R.drawable.for_stop_button);
        state = State.PointCollecting;
      }
      // collecting 끝내기 위해 버튼 누름
      else if (state == State.PointCollecting) {
        recordButton.setImageResource(R.drawable.for_record_button);
        glView.queueEvent(() -> pointCloudRenderer.fix(pointCollector.getPointBuffer()));
        state = State.PointCollected;
      }else if(state == State.FoundSurface){
        state = State.Capture;
      }
      //초기화하고 다시 시작
      else{
        initAll();
        recordButton.setImageResource(R.drawable.for_stop_button);
        state = State.PointCollecting;
      }
    });

    glView.setOnTouchListener((view, event) -> {
      Ray ray = Myutil.GenerateRay(event.getX(), event.getY(), glView.getMeasuredWidth(), glView.getMeasuredHeight(), projMX,viewMX,camera.getPose().getTranslation());
      if (state == State.PointCollected) {
        state = State.FindingSurface;
        // 레코드버튼을 두번째 눌러서 다 점 수집을 끝낸 상태에서 화면을 터치하면 레이를 발사해서 점 선택. 그 점으로 바닥 찾기
        findPlaneTask.initTask(pointCollector.getPointBuffer(),ray,camera.getPose().getZAxis());
        findPlaneworker.execute(findPlaneTask);
      }
      return false;
    });
  }

  //
  // - Mark: MainActivity LifeCycle Override
  //


  @Override
  protected void onDestroy() {
    if (session != null) {
      session.close();
      session = null;
    }
    worker.shutdown();
    findPlaneworker.shutdown();
    super.onDestroy();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      glView.onPause();
      session.pause();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    //OPENCV 쓰려면 꼭 써야함.
    if (!OpenCVLoader.initDebug()) {
      Log.d(TAG, "onResume :: Internal OpenCV library not found.");
    } else {
      Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
    }
    if (session == null) {
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this);
        obtainCameraConfigs();
        session.setCameraConfig(cameraConfig);

        // 초점 자동으로 맞춰주기
        Config config = new Config(session);
        config.setFocusMode(Config.FocusMode.AUTO);
        session.configure(config);

      } catch (UnavailableArcoreNotInstalledException
              | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
      } catch (Exception e) {
        message = "Failed to create AR session";
      }

      if (message != null) {
        Toast.makeText(this, "TODO: handle exception " + message, Toast.LENGTH_LONG).show();
        return;
      }
    }

    try {
      session.resume();
    } catch (CameraNotAvailableException e) {
      Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show();
      session = null;
      return;
    }

    glView.onResume();
  }

  //
  // - Mark: GLSurfaceView.Rendrer implements..
  //

  // 새로운 시작
  @Override // GLSurfaceView.Renderer.onSurfaceCreated()
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
    forDebugging = new SimpleDraw();
    pointCollector = new PointCollector();
    pointCloudRenderer = new PointCloudRenderer();
    background = new Background();
    drawText = new DrawText();
    drawText.setTexture(width,height);
//    contourForDraws = new ArrayList<>();
    ellipsePool = new EllipsePool(100);
    contours = new ArrayList<>();
    ellipses = new ArrayList<>();
    //TODO Method 이름을 적확하게 해두기
  }


  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    this.width = width;
    this.height = height;
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    if (session == null) return;
    Frame frame = null;
    // 배경으로 카메라 화면 입히려면 어디다 정보 넣으면 되는지 알려줄 텍스쳐 번호
    session.setCameraTextureName(background.texID);
    // 화면 크기와 텍스쳐 크기를 맞춰주기 위한 그런.. ->
    session.setDisplayGeometry(((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation(), width, height);

    try {
      frame = session.update();
    } catch (CameraNotAvailableException e) {
      return;
    }

    //평면을 찾은 뒤에 이미지를 옮길것임.
    if ((state == State.FoundSurface||state == State.Capture) && !isBusy) {
      try {
        image = frame.acquireCameraImage();

        worker.execute(() -> {
          if (image == null)
            return;

          isBusy = true;

          // viewMX, ProjMax 훔침
          // SnapShot 과정..
          float[] snapprojMX = projMX.clone();
          float[] snapviewMX = viewMX.clone();
          float[] snapcameratrans = camera.getPose().getTranslation();

          contours.clear();
          ellipses.clear();

          // 컨투어 그리기 위한거(디버그용)
//          ArrayList<Contour> localcontours = new ArrayList<>();

          contours =  jni.findTimberContours(image);
          if(state == State.Capture){
            img = new Mat();
            img = Myutil.ArImg2CVImg(image);
          }
          image.close();
          //Contour 들을 ellipse로 변환
          for(Contour contour: contours){
            if(contour.points.length <= 10)
              continue;
            Contour localContour = contour.cliptolocal(snapprojMX,snapviewMX,snapcameratrans,plane,background.getTexCoord());
            Ellipse tempellipse = Myutil.findBoundingBox(localContour);
            tempellipse.setRottation(plane);
            ellipses.add(tempellipse);

            // 컨투어 그리기 위한거(디버그용)
//            localcontours.add(localContour);
          }

          //개수 표시
          runOnUiThread(() -> {
            if(state != State.Idle)
              txtCount.setText(String.format("개수: %d", ellipses.size()));
          });

          glView.queueEvent(() -> {
            ellipsePool.clear();
            drawText.clearEllipses();

            // 컨투어 그리기 위한거(디버그용)
//              contourForDraws.clear();

            for (Ellipse ellipse: ellipses)
            {
              ellipse.pivot_to_local(snapprojMX,snapviewMX);
              if(ellipsePool.isFull()) {
                //아예 new로 하나 새로 할당
                ellipsePool.addEllipse(ellipse);
              } else {
                //기존에 있는거에서 데이터만 바꿈
                ellipsePool.setEllipse(ellipse);
              }
              drawText.setEllipses(ellipse);
            }

            drawText.setTexture(width,height);

            // 컨투어 그리기 위한거(디버그용)
//            for (Contour localContor: localcontours)
//            {
//              ContourForDraw contourForDraw = new ContourForDraw();
//              contourForDraw.setContour(plane, localContor);
//              contourForDraws.add(contourForDraw);
//            }
            if(state== State.Capture&&img!=null){
              //Ellipse와 Image파일을 넘겨야함.

              Intent intent = new Intent(MainActivity.this, ResultActivity.class);
              intent.putParcelableArrayListExtra("Ellipse",ellipses);
              intent.putExtra("projMat",snapprojMX);
              intent.putExtra("viewMat",snapviewMX);
              Bitmap bmp = null;
              bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
              Utils.matToBitmap(img, bmp);
              ByteArrayOutputStream stream = new ByteArrayOutputStream();
              bmp.compress(Bitmap.CompressFormat.JPEG, 50, stream);
              byte[] byteArray = stream.toByteArray();
              intent.putExtra("image", byteArray);
              startActivity(intent);

            }

            isBusy = false;
          });
        });
      } catch (NotYetAvailableException e) {
        // Fail to access raw image...
      }
    }
    //한번만해 ㅅㅂ
    if (frame.hasDisplayGeometryChanged()) {
      background.transformCoordinate(frame);
    }

    camera = frame.getCamera();
    // view matrix, projection matrix 받아오기
    camera.getProjectionMatrix(projMX, 0, 0.1f, 100.0f);
    camera.getViewMatrix(viewMX, 0);

    // 그리기 전에 버퍼 초기화
    // drawing phase
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    background.draw();

    switch(state){
      case FoundSurface:

//        // 컨투어 그리기 (디버그용)
//        if(mode_contour) {
//          for (ContourForDraw contourForDraw : contourForDraws) {
//            contourForDraw.draw(viewMX, projMX);
//          }
//        }

        for(int i = 0; i < ellipsePool.useCount; i++) {
          ellipsePool.drawEllipses.get(i).draw(viewMX, projMX);
        }
        drawText.draw();
        break;
      case PointCollecting:
        // 일이 분리가 안된것 같긴한데 frame을 얻고 해야하므로 어쩔 수 없음.
        pointCollector.push(frame.acquirePointCloud());
        pointCloudRenderer.update(frame.acquirePointCloud());
        pointCloudRenderer.draw(viewMX, projMX);
        break;
      case FindingSurface:
        // 선택한 점 그리기.
        pointCloudRenderer.draw(viewMX, projMX);
        // TODO seed point 제거 해야할 지 정해야함.
        forDebugging.draw(findPlaneTask.seedPointArr, GLES20.GL_POINTS, 4, 1f, 0f, 0f, viewMX, projMX);
        break;
      case PointCollected:
        pointCloudRenderer.draw(viewMX, projMX);
        break;
    }

  }

  //
  // GL ends
  //

  // CameraCongfing 모두 끌고와서 1920x1080선택
  private void obtainCameraConfigs() {
    // First obtain the session handle before getting the list of various camera configs.
    if (session != null) {
      // Create filter here with desired fps filters.
      CameraConfigFilter cameraConfigFilter =
              new CameraConfigFilter(session)
                      .setTargetFps(
                              EnumSet.of(
                                      CameraConfig.TargetFps.TARGET_FPS_30, CameraConfig.TargetFps.TARGET_FPS_60));
      List<CameraConfig> cameraConfigs = session.getSupportedCameraConfigs(cameraConfigFilter);
      List<CameraConfig> cameraConfigsByResolution =
              new ArrayList<>(
                      cameraConfigs.subList(0, Math.min(cameraConfigs.size(), 3)));
      Collections.sort(
              cameraConfigsByResolution,
              (CameraConfig p1, CameraConfig p2) ->
                      Integer.compare(p1.getImageSize().getHeight(), p2.getImageSize().getHeight()));
      cameraConfig = cameraConfigsByResolution.get(2);
    }
  }

  @Override
  //카메라 권한 확인
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  //초기화
  private void initAll(){
    state = State.Idle;

    try {
      findPlaneworker.awaitTermination(100, TimeUnit.MILLISECONDS);
      worker.awaitTermination(100, TimeUnit.MILLISECONDS);
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    plane = null;
    ellipsePool.clear();
//    contourForDraws.clear();
    pointCollector = new PointCollector();
    txtCount.setText("개수:");
  }
}