package com.example.CS_IU_proto_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.tabs.TabLayout;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

  boolean mode_contour = false;
  boolean mode_ellipse = true;

  boolean isCapture = false;

  ExecutorService worker;
  ExecutorService findPlaneworker;
  FindPlaneTask findPlaneTask;

  SimpleDraw forDebugging; // 선택한 점 그리는거
  Background background; // background
  DrawText drawText;
  ArrayList<ContourForDraw> contourForDraws;
  ArrayList<DrawEllipse> drawEllipses;
  ArrayList<Contour> contours;
  OpenCVJNI jni;

  ArrayList<Circle> circles; // 클릭하면 cubes가 만들어질거임
  PointCloudRenderer pointCloudRenderer; // PointCloud그림
  PointCollector pointCollector; // 모을거임
  Plane plane;

  GLSurfaceView glView; // 띄우기 위한 View
  Button recordButton, contourButton, ellipseButton, resizeButton, normButton, morphButton, markerButton, bgrangeButton, captureButton, optionButton;; // 레코딩~
  SwitchCompat bgSwitch;
  TabLayout resizeTab;
  TextView txtCount, txtClose, txtOpen, txtNormLvl, txtOpenLvl, txtCloseLvL, txtMarkerLvL;
  SeekBar normBar, morphCloseBar,morphOpenBar, markerTHBar;
  View normLayout, morphLayout, markerLayout, optionLayout;
  int normLvL = 2, markerLvL = 10 , openLvL = 2, closeLvL = 1, resizelvl = 600;
  boolean bg_enable_filtering = false;

  int width = 1, height = 1;
  float[] projMX = {1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f};
  float[] viewMX = {1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f,0,0,0,0,1.0f};
  //앱종료시간체크
  long backKeyPressedTime;    //앱종료 위한 백버튼 누른시간

  //뒤로가기 2번하면 앱종료

  public void setVisibility(View view)
  {
    if(view.getVisibility() == View.VISIBLE)
      view.setVisibility(View.INVISIBLE);
    else
      view.setVisibility(View.VISIBLE);
  }

  public void setBarLvL(SeekBar bar, int progress)
  {
    if(bar.equals(normBar))
      normLvL = progress;
    else if(bar.equals(morphOpenBar))
      openLvL = progress;
    else if(bar.equals(morphCloseBar))
      closeLvL = progress;
    else if(bar.equals(markerTHBar))
      markerLvL = progress;
  }

  public void setSeekBarListener(SeekBar bar, TextView txt){
    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // onProgressChange - Seekbar 값 변경될때마다 호출
        txt.setText(String.valueOf(seekBar.getProgress()));
        //나중에 수정하기 (local variable 사용 못함)
        setBarLvL(bar, progress);
      }
      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        // onStartTeackingTouch - SeekBar 값 변경위해 첫 눌림에 호출
        txt.setText(String.valueOf(seekBar.getProgress()));
        setBarLvL(bar, seekBar.getProgress());
      }
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        // onStopTrackingTouch - SeekBar 값 변경 끝나고 드래그 떼면 호출
        txt.setText(String.valueOf(seekBar.getProgress()));
        setBarLvL(bar, seekBar.getProgress());
    }});
  }
  @Override
  public void onBackPressed() {
    //1번째 백버튼 클릭
    if(System.currentTimeMillis()>backKeyPressedTime+2000){
      backKeyPressedTime = System.currentTimeMillis();
      Toast.makeText(this, "한번 더 눌러 앱 종료", Toast.LENGTH_SHORT).show();
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
        runOnUiThread(() -> {
          Toast.makeText(MainActivity.this,"평면을 찾았습니다.",Toast.LENGTH_SHORT).show();
        });
        state = State.FoundSurface;
        plane = _plane;
      }

      @Override
      public void onFailTask() {
        runOnUiThread(() -> {
          Toast.makeText(MainActivity.this,"평면을 못 찾았습니다. 다시 시도하여 주세요.",Toast.LENGTH_SHORT).show();
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

    txtCount = findViewById(R.id.txtCount);
    txtClose = findViewById(R.id.txtMorphClose);
    txtOpen = findViewById(R.id.txtMorphOpen);
    contourButton = findViewById(R.id.btnContour);
    ellipseButton = findViewById(R.id.btnEllipse);
    recordButton = findViewById(R.id.recordButton);

    resizeButton = findViewById(R.id.btnResize);

    normButton = findViewById(R.id.btnNorm);
    morphButton = findViewById(R.id.btnMorph);
    markerButton = findViewById(R.id.btnMarker);
    bgrangeButton = findViewById(R.id.btnBackgroundRange);
    captureButton = findViewById(R.id.btnCapture);
    bgSwitch = findViewById(R.id.switchBG);

    resizeTab = findViewById(R.id.tabResize);
    resizeTab.selectTab(resizeTab.getTabAt((resizelvl-500)/100));

    normLayout = findViewById(R.id.normLayout);
    normButton = findViewById(R.id.btnNorm);
    normBar = findViewById(R.id.barNorm);
    txtNormLvl = findViewById(R.id.txtNormLvL);
    normBar.setProgress(normLvL);
    txtNormLvl.setText(String.valueOf(normLvL));

    morphLayout = findViewById(R.id.morphLayout);
    morphButton = findViewById(R.id.btnMorph);
    morphCloseBar = findViewById(R.id.barClose);
    morphOpenBar = findViewById(R.id.barOpen);
    txtCloseLvL = findViewById(R.id.txtCloseLvL);
    txtOpenLvl = findViewById(R.id.txtOpenLvL);
    morphCloseBar.setProgress(closeLvL);
    txtCloseLvL.setText(String.valueOf(closeLvL));
    morphOpenBar.setProgress(openLvL);
    txtOpenLvl.setText(String.valueOf(openLvL));

    markerLayout = findViewById(R.id.markerLayout);
    markerButton = findViewById(R.id.btnMarker);
    markerTHBar = findViewById(R.id.barMarkerth);
    txtMarkerLvL = findViewById(R.id.txtMarkerLvL);
    markerTHBar.setProgress(markerLvL);
    txtMarkerLvL.setText(String.valueOf(markerLvL));

    bgrangeButton = findViewById(R.id.btnBackgroundRange);

    bgSwitch = findViewById(R.id.switchBG);

    optionLayout = findViewById(R.id.optionLayout);
    optionButton = findViewById(R.id.btnOption);


    contourButton.setOnClickListener(l -> mode_contour = !mode_contour);
    ellipseButton.setOnClickListener(l -> mode_ellipse = !mode_ellipse);

    resizeButton.setOnClickListener(l-> {
      setVisibility(resizeTab);
    });

    normButton.setOnClickListener(l-> {
      setVisibility(normLayout);
    });

    morphButton.setOnClickListener(l-> {
      setVisibility(morphLayout);
    });

    markerButton.setOnClickListener(l-> {
      setVisibility(markerLayout);
    });

    captureButton.setOnClickListener(l -> {
      this.isCapture = true;
      captureButton.setEnabled(false);
    });

    bgSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked)
          bg_enable_filtering = true;
        else
          bg_enable_filtering = false;
      }
    });

    optionButton.setOnClickListener(l->{
      setVisibility(optionLayout);
      setVisibility(bgSwitch);
      setVisibility(contourButton);
      setVisibility(ellipseButton);
      setVisibility(captureButton);
    });

    setSeekBarListener(normBar, txtNormLvl);
    setSeekBarListener(morphCloseBar, txtCloseLvL);
    setSeekBarListener(morphOpenBar, txtOpenLvl);
    setSeekBarListener(markerTHBar, txtMarkerLvL);

    resizeTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      @Override
      public void onTabSelected(TabLayout.Tab tab) {
        resizelvl = tab.getPosition()*100 + 500;
      }
      @Override
      public void onTabUnselected(TabLayout.Tab tab) {
        // do nothing
      }

      @Override
      public void onTabReselected(TabLayout.Tab tab) {
        // do nothing
      }
    }) ;




    출처: https://bitsoul.tistory.com/29 [Happy Programmer~]

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
        mode_contour = false;
        mode_ellipse = true;
        try {
          worker.awaitTermination(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        plane = null;
        contourForDraws.clear();
        drawEllipses.clear();
        pointCollector = new PointCollector();
      }
    });

    glView.setOnTouchListener((view, event) -> {
      Ray ray = Myutil.GenerateRay(event.getX(), event.getY(), glView.getMeasuredWidth(), glView.getMeasuredHeight(), projMX,viewMX,camera.getPose().getTranslation());

      if (state == State.FoundSurface) {
        float[] point = Myutil.pickSurfacePoints(plane,ray);
        glView.queueEvent(() -> {
          Circle circle = new Circle();
          circle.setCircle(plane, point);
          circles.add(circle);
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
    circles = new ArrayList<>();
    contourForDraws = new ArrayList<>();
    drawEllipses = new ArrayList<>();
    contours = new ArrayList<>();
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
    if (state == State.FoundSurface) {

      if (!isBusy) {
        try {
          image = frame.acquireCameraImage();

          // viewMX, ProjMax 훔침
          // SnapShot 과정..

          worker.execute(() -> {
              if (image == null) {
                return;
              }
              isBusy = true;
              float[] snapprojMX = projMX.clone();
              float[] snapviewMX = viewMX.clone(); // 복사가 되나???
              float[] snapcameratrans = camera.getPose().getTranslation();
              // ADDED BY OPENCV TEAM
              contours.clear();

              Image.Plane[] planes = image.getPlanes();
              ByteBuffer bufferY   = planes[0].getBuffer();
              ByteBuffer bufferUV  = planes[1].getBuffer();
              ByteBuffer bufferYUV = ByteBuffer.allocateDirect( bufferY.remaining() + bufferUV.remaining() );
              bufferYUV.put( bufferY ).put( bufferUV );
              bufferYUV.rewind();

            contours =  jni.findTimberContours(bufferYUV, image.getWidth(), image.getHeight(), resizelvl, (double)(normLvL)/100.0, closeLvL, openLvL, (double)(markerLvL)/100.0, bg_enable_filtering);

              if ( this.isCapture ) {
                this.isCapture = false;
                bufferYUV.rewind();
                byte[] bytesYUV = new byte[ bufferYUV.remaining() ];
                bufferYUV.get(bytesYUV);
                bufferYUV.rewind();

                File path = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
                String currentDateAndTime = sdf.format(new Date());

                File dst  = new File(path, "image_yuv420 " + currentDateAndTime + ".raw");
                Log.i("CaptureImage", dst.getAbsolutePath() );
                try {
                  OutputStream os = new FileOutputStream(dst);
                  os.write( bytesYUV );
                  os.close();

                  runOnUiThread(() -> {
                    Toast.makeText(this, "Save: " + dst.getAbsolutePath(), Toast.LENGTH_SHORT ).show();

                    captureButton.setEnabled(true);
                  });

                } catch (IOException e) {
                  Log.w("ExternalStorage", "Error writing " + dst, e);

                  runOnUiThread(() -> {
                    // Enable Button
                    captureButton.setEnabled(true);
                  });
                }

              }

            ArrayList<Contour> localcontours = new ArrayList<>();
              ArrayList<Ellipse> ellipses = new ArrayList<>();
              // ADDED BY OPENCV TEAM
              for (Contour contour: contours) {
                localcontours.add(contour.cliptolocal(snapprojMX,snapviewMX,snapcameratrans,plane));
              }
              for (Contour contour: localcontours)
              {
                Ellipse tempellipse = Myutil.findBoundingBox(contour);
                tempellipse.setRottation(plane);
                ellipses.add(tempellipse);
              }

              runOnUiThread(() -> {
                txtCount.setText("개수: "+localcontours.size());
              });


              image.close();
              glView.queueEvent(() -> {
                  contourForDraws.clear();
                  drawEllipses.clear();
                  drawText.clearEllipses();

                  for (Ellipse ellipse: ellipses)
                  {
                      ellipse.pivot_to_local(projMX,viewMX);
                      DrawEllipse drawEllipse = new DrawEllipse();
                      drawEllipse.setContour(plane,ellipse);
                      drawText.setEllipses(ellipse);
                      drawEllipses.add(drawEllipse);
                  }


                  for (Contour localContor: localcontours)
                  {
                    ContourForDraw contourForDraw = new ContourForDraw();
                    contourForDraw.setContour(plane, localContor);
                    contourForDraws.add(contourForDraw);
                  }
                  drawText.setTexture(width,height);
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
//        forDebugging.draw(plane.planeVertex, GLES20.GL_TRIANGLES, 3, 0.5f, 0.5f, 0f, viewMX, projMX);
//        for (Cube cube : cubes) {
//          cube.update(dt, findPlane.plane);
//          cube.draw(viewMX, projMX);
//        }
//        for (Circle circle : circles) {
//          circle.draw(viewMX, projMX);
//        }
        if(mode_contour) {
          for (ContourForDraw contourForDraw : contourForDraws) {
            contourForDraw.draw(viewMX, projMX);
          }
        }

        if(mode_ellipse) {
          for (DrawEllipse drawEllipse : drawEllipses) {
            drawEllipse.draw(viewMX, projMX);
          }
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