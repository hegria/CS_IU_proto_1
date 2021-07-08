package com.example.CS_IU_proto_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

  private static final String TAG = "opencv";

  GLSurfaceView glView; // 띄우기 위한 View


  private static final int IDLE = 1;
  private static final int POINT_COLLECTING = 2;
  private static final int POINT_COLLECTED = 3;
  private static final int FINDING_SURFACE = 4;
  private static final int FOUND_SURFACE = 5;

  // state 1 => 제일 처음, 2 => pointcollection 시작, 3=> pointcolleted 끝(findsurface 시작) => 4 findsurface 진행중 5 => 6
  // plane 시작.
  int state = IDLE;

  boolean installRequested = false;
  Session session; // ??
  Camera camera; // 그냥 카메라

  boolean isBusy = false;

  Image image;
  ExecutorService worker;
  ExecutorService findPlaneworker;

  SimpleDraw forDebugging; // 선택한 점 그리는거
  Background background; // background
  ArrayList<Cube> cubes; // 클릭하면 cubes가 만들어질거임
  ArrayList<Circle> circles; // 클릭하면 cubes가 만들어질거임

  PointCloudRenderer pointCloudRenderer; // PointCloud그림

  PointCollector pointCollector; // 모을거임

  Button recordButton; // 레코딩~

  CameraConfig cameraConfig;
  FindPlaneTask findPlaneTask;
  Future<Boolean> isFoundPlane;


  int width = 1, height = 1;
  float[] projMX = {1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f};
  float[] viewMX = {1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f};

  @SuppressLint("ClickableViewAccessibility")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    worker = Executors.newSingleThreadExecutor();
    findPlaneworker = Executors.newSingleThreadExecutor();
    // 전체화면
    findPlaneTask = new FindPlaneTask();
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
      if (state == POINT_COLLECTING) {
        // collecting 끝내기 위해 버튼 누름
        glView.queueEvent(() -> {
          pointCloudRenderer.fix(pointCollector.getPointBuffer());
        });

        state = POINT_COLLECTED;
        recordButton.setText("Reset");
      } else if(state == IDLE) {
        recordButton.setText("Fix");
        // collecting 시작하기 위해 버튼 누름
        state = POINT_COLLECTING;
      }else{
        recordButton.setText("Record");
        state = IDLE;
        circles.clear();
        pointCollector = new PointCollector();
      }
    });

    glView.setOnTouchListener((view, event) -> {
      if (state == FOUND_SURFACE) {
        // 바닥을 찾은 후 화면을 터치하면 카메라의 world space 좌표만큼 translate 되는 큐브 생성
//        glView.queueEvent(() -> {
//          Cube cube = new Cube();
//          cube.xyz = new float[]{camera.getPose().tx(), camera.getPose().ty(), camera.getPose().tz()};
//          cubes.add(cube);
//
//        });
        // ray 찾음

        // 카메라를 lIst로 빼두는건 나중에 생각하는걸루하고..
        float[] rayInfo = Myutil.rayPicking(event.getX(), event.getY(), glView.getMeasuredWidth(), glView.getMeasuredHeight(), camera);
        float[] ray_origin = new float[]{rayInfo[0], rayInfo[1], rayInfo[2]}; //원점좌표
        float[] ray_dir = new float[]{rayInfo[3], rayInfo[4], rayInfo[5]};
        float[] point = Myutil.pickSurfacePoints(findPlaneTask.plane,ray_origin,ray_dir);
        glView.queueEvent(() -> {
//          Cube cube = new Cube();
//          cube.xyz = new float[]{point[0],point[1],point[2]};
//          cubes.add(cube);
          Circle circle = new Circle();
          circle.setCircle(findPlaneTask.plane, point);
          circles.add(circle);

        });
        return false;
      } else if (state == POINT_COLLECTED) {
        state = FINDING_SURFACE;
        // 레코드버튼을 두번째 눌러서 다 점 수집을 끝낸 상태에서 화면을 터치하면 레이를 발사해서 점 선택. 그 점으로 바닥 찾기
        float[] rayInfo = Myutil.rayPicking(event.getX(), event.getY(), glView.getMeasuredWidth(), glView.getMeasuredHeight(), camera);
        findPlaneTask.initTask(pointCollector.getPointBuffer(),rayInfo,camera.getPose().getZAxis());
        isFoundPlane = findPlaneworker.submit(findPlaneTask);
        // 일할때까지 숨 참음
        try {
          if(isFoundPlane.get()){
            Toast.makeText(this,"I got it",Toast.LENGTH_SHORT).show();
            state = FOUND_SURFACE;
          }else{
            Toast.makeText(this,"I can't got it",Toast.LENGTH_SHORT).show();
            state = POINT_COLLECTED;
          }
        } catch (ExecutionException | InterruptedException e) {
          e.printStackTrace();
        }
        return false;
      }
      return false;
    });
  }

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

  // 새로운 시작
  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
    forDebugging = new SimpleDraw();
    pointCollector = new PointCollector();
    pointCloudRenderer = new PointCloudRenderer();
    background = new Background();
    circles = new ArrayList<>();
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
    if (state == FOUND_SURFACE) {

      if (!isBusy) {
        try {
          image = frame.acquireCameraImage();


          worker.execute(() -> {
              if (image == null) {
                return;
              }

              isBusy = true;
              Mat img = Myutil.ArImg2CVImg(image);
              image.close();
              glView.queueEvent(() -> {
                background.updateCVImage(img);
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
      case FOUND_SURFACE:
        forDebugging.draw(findPlaneTask.plane.planeVertex, GLES20.GL_TRIANGLES, 3, 0.5f, 0.5f, 0f, viewMX, projMX);
//        for (Cube cube : cubes) {
//          cube.update(dt, findPlane.plane);
//          cube.draw(viewMX, projMX);
//        }
        for (Circle circle : circles) {
          circle.draw(viewMX, projMX);
        }
        break;
      case POINT_COLLECTING:
        // 일이 분리가 안된것 같긴한데 frame을 얻고 해야하므로 어쩔 수 없음.
        pointCollector.push(frame.acquirePointCloud());
        pointCloudRenderer.update(frame.acquirePointCloud());
        pointCloudRenderer.draw(viewMX, projMX);
        break;
      case FINDING_SURFACE:
        // 선택한 점 그리기.
        pointCloudRenderer.draw(viewMX, projMX);
        forDebugging.draw(findPlaneTask.seedPointArr, GLES20.GL_POINTS, 4, 1f, 0f, 0f, viewMX, projMX);
        break;
      case POINT_COLLECTED:
        pointCloudRenderer.draw(viewMX, projMX);
        break;
    }

  }
}