package com.example.CS_IU_proto_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.AsyncTask;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

  private static final String TAG = "opencv";

  GLSurfaceView glView; // 띄우기 위한 View

  boolean toggleFlag = true;
  boolean installRequested = false;
  Session session; // ??
  Camera camera; // 그냥 카메라

  boolean isBusy = false;

  Image image;
  ExecutorService worker;

  SimpleDraw forDebugging; // 선택한 점 그리는거
  Background background; // background
  ArrayList<Cube> cubes; // 클릭하면 cubes가 만들어질거임
  ArrayList<Circle> circles; // 클릭하면 cubes가 만들어질거임

  PointCloudRenderer pointCloudRenderer; // PointCloud그림

  PointCollector pointCollector; // 모을거임

  Button recordButton; // 레코딩~

  boolean isCollecting = false;
  boolean isPointPicked = false;

  CameraConfig cameraConfig;
  FindPlane findPlane = null; // 아직 못찾음.

  int seedID = -1; // ??
  float[] seedPointArr = new float[]{0.0f, 0.0f, 0.0f, 1.0f}; // ??

  @SuppressLint("ClickableViewAccessibility")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    worker = Executors.newSingleThreadExecutor();
    // 전체화면

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
      if (isCollecting) {
        // collecting 끝내기 위해 버튼 누름
        glView.queueEvent(() -> {
          pointCloudRenderer.fix(pointCollector.getPointBuffer());
        });

        isCollecting = false;
      } else {
        // collecting 시작하기 위해 버튼 누름
        isCollecting = true;
        isPointPicked = false;
        findPlane = null;
        circles.clear();
        pointCollector = new PointCollector();
      }
    });

    glView.setOnTouchListener((view, event) -> {
      if (findPlane != null && findPlane.plane != null) {
        // 바닥을 찾은 후 화면을 터치하면 카메라의 world space 좌표만큼 translate 되는 큐브 생성
//        glView.queueEvent(() -> {
//          Cube cube = new Cube();
//          cube.xyz = new float[]{camera.getPose().tx(), camera.getPose().ty(), camera.getPose().tz()};
//          cubes.add(cube);
//
//        });
        // ray 찾음

        // 카메라를 lIst로 빼두는건 나중에 생각하는걸루하고..
        float[] rayInfo = rayPicking(event.getX(), event.getY(), glView.getMeasuredWidth(), glView.getMeasuredHeight(), camera);
        float[] ray_origin = new float[]{rayInfo[0], rayInfo[1], rayInfo[2]}; //원점좌표
        float[] ray_dir = new float[]{rayInfo[3], rayInfo[4], rayInfo[5]};
        float[] point = Myutil.pickPoints(findPlane.plane,ray_origin,ray_dir);
        glView.queueEvent(() -> {
//          Cube cube = new Cube();
//          cube.xyz = new float[]{point[0],point[1],point[2]};
//          cubes.add(cube);
          Circle circle = new Circle();
          circle.setCircle(findPlane.plane, point);
          circles.add(circle);

        });

      } else if (pointCollector.isFiltered) {
        // 레코드버튼을 두번째 눌러서 다 점 수집을 끝낸 상태에서 화면을 터치하면 레이를 발사해서 점 선택. 그 점으로 바닥 찾기
        float[] rayInfo = rayPicking(event.getX(), event.getY(), glView.getMeasuredWidth(), glView.getMeasuredHeight(), camera);
        float[] ray_origin = new float[]{rayInfo[0], rayInfo[1], rayInfo[2]}; //원점좌표
        float[] ray_dir = new float[]{rayInfo[3], rayInfo[4], rayInfo[5]};
        pickPoint(pointCollector.getPointBuffer(), ray_origin, ray_dir);

        isPointPicked = true;

        findPlane = new FindPlane(this, pointCollector.getPointBuffer(), seedID, camera);
        if (findPlane.getStatus() == AsyncTask.Status.FINISHED || findPlane.getStatus() == AsyncTask.Status.RUNNING) {
          findPlane.cancel(true);
          findPlane = new FindPlane(this, pointCollector.getPointBuffer(), seedID, camera);
        }
        findPlane.execute();
        return true;
      }
      return false;
    });
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

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
    forDebugging = new SimpleDraw();
    pointCollector = new PointCollector();
    pointCloudRenderer = new PointCloudRenderer();
    background = new Background();
    circles = new ArrayList<>();
    background.SetsplitterPosition(1.0f);
  }

  int width = 1, height = 1;

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
    // 화면 크기와 텍스쳐 크기를 맞춰주기 위한 그런...
    session.setDisplayGeometry(((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation(), width, height);
    if (!toggleFlag) {

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
                  }
          );


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
      float[] projMX = new float[16];
      camera.getProjectionMatrix(projMX, 0, 0.1f, 100.0f);
      float[] viewMX = new float[16];
      camera.getViewMatrix(viewMX, 0);

      // 그리기 전에 버퍼 초기화

      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

      background.draw();


      // ray picking 으로 어느 점이 선택되었는지 보여주기 위함
      // 찾은 바닥이 어떻게 생겼는지 보여주기 위함
      if (findPlane != null && findPlane.plane != null) {
        float[] pointForDrawingPlane = {
                findPlane.plane.ll[0], findPlane.plane.ll[1], findPlane.plane.ll[2],
                findPlane.plane.lr[0], findPlane.plane.lr[1], findPlane.plane.lr[2],
                findPlane.plane.ur[0], findPlane.plane.ur[1], findPlane.plane.ur[2],
                findPlane.plane.ll[0], findPlane.plane.ll[1], findPlane.plane.ll[2],
                findPlane.plane.ur[0], findPlane.plane.ur[1], findPlane.plane.ur[2],
                findPlane.plane.ul[0], findPlane.plane.ul[1], findPlane.plane.ul[2],
        };

//        for (Cube cube : cubes) {
//          cube.update(dt, findPlane.plane);
//          cube.draw(viewMX, projMX);
//        }

        forDebugging.draw(pointForDrawingPlane, GLES20.GL_TRIANGLES, 3, 0.5f, 0.5f, 0f, viewMX, projMX);
        for (Circle circle : circles) {
          circle.draw(viewMX, projMX);
        }

      } else {

        if (isCollecting) {
          // 매 프레임마다 보이는 특징점들을 수집하고 렌더링하기
          pointCollector.push(frame.acquirePointCloud());
          pointCloudRenderer.update(frame.acquirePointCloud());
        }
        pointCloudRenderer.draw(viewMX, projMX);
      }

      if (isPointPicked)
        forDebugging.draw(seedPointArr, GLES20.GL_POINTS, 4, 1f, 0f, 0f, viewMX, projMX);
    }


  float[] rayPicking(float xPx, float yPx, int screenWidth, int screenHeight, Camera camera) {
    // https://antongerdelan.net/opengl/raycasting.html 참고

    // screen space 에서 clip space 로
    float x = 2.0f * xPx / screenWidth - 1.0f;
    float y = 1.0f - 2.0f * yPx / screenHeight;

    float[] projMX = new float[16];
    camera.getProjectionMatrix(projMX, 0, 0.1f, 100.0f);
    float[] inverseProjMX = new float[16];
    Matrix.invertM(inverseProjMX, 0, projMX, 0);

    float[] viewMX = new float[16];
    camera.getViewMatrix(viewMX, 0);
    float[] inverseViewMX = new float[16];
    Matrix.invertM(inverseViewMX, 0, viewMX, 0);

    float[] ray_clip = new float[]{x, y, -1f, 1f};

    // clip space 에서 view space 로
    float[] ray_eye = new float[4];
    Matrix.multiplyMV(ray_eye, 0, inverseProjMX, 0, ray_clip, 0);
    ray_eye = new float[]{ray_eye[0], ray_eye[1], -1.0f, 0.0f};

    // view space 에서 world space 로
    float[] ray_wor = new float[4];
    Matrix.multiplyMV(ray_wor, 0, inverseViewMX, 0, ray_eye, 0);

    // normalize 시켜주기 위해 벡터의 크기 계산
    float ray_wor_length = (float) Math.sqrt(ray_wor[0] * ray_wor[0] + ray_wor[1] * ray_wor[1] + ray_wor[2] * ray_wor[2]);

    float[] out = new float[6];

    // 카메라의 world space 좌표
    out[0] = camera.getPose().tx();
    out[1] = camera.getPose().ty();
    out[2] = camera.getPose().tz();

    // ray의 normalized 된 방향벡터
    out[3] = ray_wor[0] / ray_wor_length;
    out[4] = ray_wor[1] / ray_wor_length;
    out[5] = ray_wor[2] / ray_wor_length;

    return out;
  }

  public void pickPoint(FloatBuffer filterPoints, float[] camera, float[] ray) {
    // camera: 카메라의 world space 위치(x,y,z), ray : ray의 방향벡터
    float minDistanceSq = Float.MAX_VALUE;

    filterPoints.rewind();

    for (int i = 0; i < filterPoints.remaining(); i += 4) {
      float[] point = new float[]{filterPoints.get(i), filterPoints.get(i + 1), filterPoints.get(i + 2), filterPoints.get(i + 3)};
      float[] product = new float[]{point[0] - camera[0], point[1] - camera[1], point[2] - camera[2], 1.0f};

      // 카메라 -> 특징점 벡터의 크기와, 이 벡터와 카메라 -> ray 벡터를 내적한 값을 이용해 피타고라스정리
      float distanceSq = product[0] * product[0] + product[1] * product[1] + product[2] * product[2];
      float innerProduct = ray[0] * product[0] + ray[1] * product[1] + ray[2] * product[2];
      distanceSq = distanceSq - (innerProduct * innerProduct);

      if (distanceSq < 0.01f && distanceSq < minDistanceSq) {
        //찾은 점의 index.
        seedPointArr[0] = point[0];
        seedPointArr[1] = point[1];
        seedPointArr[2] = point[2];
        seedID = i / 4;
        minDistanceSq = distanceSq;
      }
    }
  }
}