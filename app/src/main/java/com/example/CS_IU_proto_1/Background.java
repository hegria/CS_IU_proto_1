package com.example.CS_IU_proto_1;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.concurrent.Future;

public class Background {
  int program;
  int texID;
  int teximgID;

  private FloatBuffer quadCoords;
  private FloatBuffer quadTexCoords;

  int[] textures = new int[2];
  int vpos;
  int tpos;
//    int ipos;

  FloatBuffer vb;
  FloatBuffer tb;

  private final String vscode = "" +
          "attribute vec4 vPosition;\n" + // 포지션 벡터 attribute는 입력받는 변수
          "attribute vec2 vTexcoord;\n" + // 텍스트 벡터
          //         "attribute vec2 vImgcoord;\n" + // 텍스트 벡터

          "varying vec2 tc;\n" + // 지들끼리 옮기는거
          //       "varying vec2 ti;\n" + // 지들끼리 옮기는거

          "void main() {\n" +
          "  gl_Position = vPosition;\n" +
          "  tc = vTexcoord;\n" +
          //        "  ti = vImgcoord;\n" +
          "}\n";

  private final String fscode = "" +
          "#extension GL_OES_EGL_image_external : require\n" +
          "" +
          "precision mediump float;\n" + // 정밀도라네요~
          "uniform samplerExternalOES tex;\n" + // uniform1i로 texture를 바인딩함
          "uniform sampler2D tei;\n" + // uniform1i로 texture를 바인딩함
          "varying vec2 tc;\n" + // 지들끼리 옮기는거
          //        "varying vec2 ti;\n" + // 지들끼리 옮기는거

          "void main() {\n" +
          " gl_FragColor = texture2D(tex,tc);\n"+
          "}\n";

  public Background() {


    int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER); // vertex shader code compile
    GLES20.glShaderSource(vs, vscode);
    GLES20.glCompileShader(vs);
    int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
    GLES20.glShaderSource(fs, fscode);
    GLES20.glCompileShader(fs);

    program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vs);
    GLES20.glAttachShader(program, fs);
    GLES20.glLinkProgram(program); // 프로그램에 겹쳐버림

    // attribute 값 집어넣으려고 함.

    float[] vertices = new float[]{-1.0f, -1.0f, +1.0f, -1.0f, -1.0f, +1.0f, +1.0f, +1.0f,}; // 2차원이니까 바로 ㄱㄴ

    int numVertice = vertices.length / 2;
    ByteBuffer bbCoords = ByteBuffer.allocateDirect(vertices.length * 4);
    bbCoords.order(ByteOrder.nativeOrder());
    quadCoords = bbCoords.asFloatBuffer();
    quadCoords.put(vertices);
    quadCoords.position(0);

    ByteBuffer bbTexCoords = ByteBuffer.allocateDirect(numVertice * 2 * 4);
    bbTexCoords.order(ByteOrder.nativeOrder());
    quadTexCoords = bbTexCoords.asFloatBuffer();


    // Texutre space 만들기
    GLES20.glGenTextures(2, textures, 0);
    texID = textures[0];
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texID);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

    teximgID = textures[1];
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, teximgID);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

    GLES20.glUseProgram(program);

    int postex = GLES20.glGetUniformLocation(program, "tex");
    GLES20.glUniform1i(postex, 0); // Texture를 uniform sampler 값에 집어쳐넣기

    int postexi = GLES20.glGetUniformLocation(program, "tei");
    GLES20.glUniform1i(postexi, 1); // Texture를 uniform sampler 값에 집어쳐넣기

    vpos = GLES20.glGetAttribLocation(program, "vPosition");
    tpos = GLES20.glGetAttribLocation(program, "vTexcoord");

  }

  public float[] getTexCoord(){

    float[] arr = new float[quadTexCoords.remaining()];
    quadTexCoords.get(arr);
    quadTexCoords.rewind();
    return arr;
  }

  public void transformCoordinate(Frame frame){

    if (true) {
      quadTexCoords.put(new float[] { 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f });
      quadTexCoords.position(0);
    } else { // 이것 때문에 contour 맵핑이 이상하게 됨 (화면 디스플레이는 quadTexCoords 변환이 이루어지는데, findTimberContours가 받는 이미지는 그 변환이 이루어지지 않은 이미지를 받음)
      frame.transformCoordinates2d(
              Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, quadCoords,
              Coordinates2d.TEXTURE_NORMALIZED, quadTexCoords);
      quadTexCoords.position(0);
    }


  }

  public void draw() {
    GLES20.glDisable(GLES20.GL_DEPTH_TEST);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texID);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,teximgID);

    GLES20.glUseProgram(program);

    // attribute 값 assgin
    GLES20.glEnableVertexAttribArray(vpos);
    GLES20.glVertexAttribPointer(vpos, 2, GLES20.GL_FLOAT, false, 0, quadCoords);
    //????????????????????????????????? 버퍼 없이 바로?

    GLES20.glEnableVertexAttribArray(tpos);
    GLES20.glVertexAttribPointer(tpos, 2, GLES20.GL_FLOAT, false, 0, quadTexCoords);

    //     GLES20.glEnableVertexAttribArray(ipos);
    //    GLES20.glVertexAttribPointer(ipos, 2, GLES20.GL_FLOAT, false, 0, quadTexCoords);

    // Texture 집어넣기


        /*  또한 glUniform1i 함수를 사용하여 각 sampler를 설정함으로써 OpenGL에게
        각 shader sampler가 속하는 텍스처 유닛이 어떤 것인지를 알려주어야 합니다
         오직 한번만 설정하면 되므로 렌더링 루프에 들어가기 전에 할 수 있습니다.
         */
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

    GLES20.glDisableVertexAttribArray(vpos);
    GLES20.glDisableVertexAttribArray(tpos);
    //     GLES20.glDisableVertexAttribArray(ipos);
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
  }

  public void updateCVImage(Mat img) {

    Bitmap bmp = null;

    bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
    Utils.matToBitmap(img, bmp);
    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,teximgID);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bmp,0);

  }
}
