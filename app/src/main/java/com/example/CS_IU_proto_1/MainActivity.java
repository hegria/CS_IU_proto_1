package com.example.CS_IU_proto_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
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
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

  private static final String TAG = "opencv";


  // state 1 => 제일 처음, 2 => pointcollection 시작, 3=> pointcolleted 끝(findsurface 시작) => 4 findsurface 진행중 5 Plane Find

  private enum State {
    Idle, PointCollecting, PointCollected, FindingSurface, FoundSurface
  }
  State state = State.Idle;

  // ARCORE 관련

  boolean installRequested = false;
  Session session; // ??
  Camera camera; // 그냥 카메라
  CameraConfig cameraConfig;

  boolean isBusy = false;
  Image image;

  ExecutorService worker;
  ExecutorService findPlaneworker;
  FindPlaneTask findPlaneTask;

  SimpleDraw forDebugging; // 선택한 점 그리는거
  Background background; // background
  ArrayList<ContourForDraw> contourForDraws;

  ArrayList<Contour> jniContours;
  OpenCVJNI jni;

//  ArrayList<Cube> cubes; // 클릭하면 cubes가 만들어질거임
  ArrayList<Circle> circles; // 클릭하면 cubes가 만들어질거임
  PointCloudRenderer pointCloudRenderer; // PointCloud그림
  PointCollector pointCollector; // 모을거임
  Plane plane;

  GLSurfaceView glView; // 띄우기 위한 View
  Button recordButton; // 레코딩~

  int width = 1, height = 1;
  float[] projMX = {1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f};
  float[] viewMX = {1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f};

  @SuppressLint("ClickableViewAccessibility")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    worker = Executors.newSingleThreadExecutor();
    findPlaneworker = Executors.newSingleThreadExecutor();
    findPlaneTask = new FindPlaneTask();
    findPlaneTask.setFindPlaneTaskListener(new FindPlaneTask.FindPlaneTaskListener() {
      @Override
      public void onSuccessTask(Plane _plane) {
        runOnUiThread(() -> {
          Toast.makeText(MainActivity.this,"I got it",Toast.LENGTH_SHORT).show();
        });
        state = State.FoundSurface;
        plane = _plane;
      }

      @Override
      public void onFailTask() {
        runOnUiThread(() -> {

          Toast.makeText(MainActivity.this,"I can't got it",Toast.LENGTH_SHORT).show();
        });
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

    recordButton = (Button) findViewById(R.id.recordButton);
    recordButton.setOnClickListener(l -> {
      if (state == State.PointCollecting) {
        // collecting 끝내기 위해 버튼 누름
        glView.queueEvent(() -> {
          pointCloudRenderer.fix(pointCollector.getPointBuffer());
        });

        state = State.PointCollected;
        recordButton.setText("Reset");
      } else if(state == State.Idle) {
        recordButton.setText("Fix");
        // collecting 시작하기 위해 버튼 누름
        state = State.PointCollecting;
      }else{
        // TODO  Reset 있는거 싹다 치워야함!!!!!! (왠지 모르겠는데 오버해드 발생) 아직 잘모르겠음
        recordButton.setText("Record");
        state = State.Idle;
        circles.clear();
        contourForDraws.clear();

        plane = null;
        pointCollector = new PointCollector();
      }
    });

    glView.setOnTouchListener((view, event) -> {
      Ray ray = Myutil.GenerateRay(event.getX(), event.getY(), glView.getMeasuredWidth(), glView.getMeasuredHeight(), projMX,viewMX,camera.getPose().getTranslation());

      if (state == State.FoundSurface) {
        float[] point = Myutil.pickSurfacePoints(plane,ray);
        glView.queueEvent(() -> {
//          Cube cube = new Cube();
//          cube.xyz = new float[]{point[0],point[1],point[2]};
//          cubes.add(cube);
          Circle circle = new Circle();
          circle.setCircle(plane, point);
          circles.add(circle);
//          ContourForDraw contourForDraw = new ContourForDraw();
//          float[] newlocal = plane.transintolocal(point);
//          Log.i("Point", Float.toString(newlocal[0])+Float.toString(newlocal[1]));
//          contourForDraw.setContour(plane,  new float[]{0+0.01f,0,
//                  0,0+0.01f,0-0.01f,0,0,0-0.01f});
//          contourForDraws.add(contourForDraw);
        });
        return false;
      } else if (state == State.PointCollected) {
        state = State.FindingSurface;
        // 레코드버튼을 두번째 눌러서 다 점 수집을 끝낸 상태에서 화면을 터치하면 레이를 발사해서 점 선택. 그 점으로 바닥 찾기
        findPlaneTask.initTask(pointCollector.getPointBuffer(),ray,camera.getPose().getZAxis());
        findPlaneworker.execute(findPlaneTask);
        return false;
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
      // Explicitly close ARCore Session to release native resources.
      // Review the API reference for important considerations before calling close() in apps with
      // more complicated lifecycle requirements:
      // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
      session.close();
      session = null;
    }

    super.onDestroy();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
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

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this);
        obtainCameraConfigs();
        session.setCameraConfig(cameraConfig);

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
    circles = new ArrayList<>();
    contourForDraws = new ArrayList<>();
    //TODO Method 이름을 적확하게 해두기
    background.SetsplitterPosition(1.0f);
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
    try {
      frame = session.update();
    } catch (CameraNotAvailableException e) {
      return;
    }
    // 배경으로 카메라 화면 입히려면 어디다 정보 넣으면 되는지 알려줄 텍스쳐 번호
    session.setCameraTextureName(background.texID);
    // 화면 크기와 텍스쳐 크기를 맞춰주기 위한 그런.. ->
    session.setDisplayGeometry(((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation(), width, height);
    //평면을 찾은 뒤에 이미지를 옮길것임.
    if (state == State.FoundSurface) {

      if (!isBusy) {
        try {
          image = frame.acquireCameraImage();
          // viewMX, ProjMax 훔침
          // SnapShot 과정..
          float[] snapviewMX = viewMX; // 복사가 되나???
          float[] snapprojMX = projMX;
          float[] snapcameratrans = camera.getPose().getTranslation();


          worker.execute(() -> {
              if (image == null) {
                return;
              }
              isBusy = true;
              Mat img = Myutil.ArImg2CVImg(image);

              // ADDED BY OPENCV TEAM
              jniContours = jni.findTimberContours(image);
              // ADDED BY OPENCV TEAM

              image.close();
              glView.queueEvent(() -> {
                if(contourForDraws.size() == 20){
                  contourForDraws.clear();
                }
                background.updateCVImage(img);
                ContourForDraw contourForDraw = new ContourForDraw();

                float[] newpoints = new float[10];
                for(int i = 0; i<10;i++){
                  newpoints[i] = (float)Math.random()-0.5f;
                }
                contourForDraw.setContour(plane,newpoints);
                contourForDraws.add(contourForDraw);
              });
              isBusy = false;
          });


        } catch (NotYetAvailableException e) {
          // Fail to access raw image...

        }
      }
  }
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
        forDebugging.draw(plane.planeVertex, GLES20.GL_TRIANGLES, 3, 0.5f, 0.5f, 0f, viewMX, projMX);
//        for (Cube cube : cubes) {
//          cube.update(dt, findPlane.plane);
//          cube.draw(viewMX, projMX);
//        }
        for (Circle circle : circles) {
          circle.draw(viewMX, projMX);
        }
        for (ContourForDraw contourForDraw : contourForDraws){
          contourForDraw.draw(viewMX,projMX);
        }
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

  @Override // FragmentAcitvity.onRequestPermissionsResult()
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
              .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }
}