package com.example.CS_IU_proto_1;

import android.graphics.Bitmap;
import android.media.Image;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BackgroundImage {
  int buffer;
  int program;
  int texID;

  int vpos;
  int tpos;
  int postex;


  private final String vscode = "" +
          "attribute vec3 vPosition;" +
          "attribute vec2 vTexcoord;\n" + // 텍스트 벡터

          "varying vec2 tc;\n" + // 지들끼리 옮기는거

          "void main() {\n" +
          "  gl_Position = vec4(vPosition, 1.0);" +
          "  tc = vTexcoord;\n" +
          "}\n";

  private final String fscode = "" +
          "" +
          "precision mediump float;\n" + // 정밀도라네요~
          "uniform sampler2D tex;" +
          "varying vec2 tc;\n" + // 지들끼리 옮기는거
          //        "varying vec2 ti;\n" + // 지들끼리 옮기는거

          "void main() {\n" +
          " gl_FragColor = texture2D(tex,tc);\n"+
          "}\n";

  public BackgroundImage() {
    float[] vertex = {
            1.0f, 1.0f, 0.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f, 0.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,

            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 0.0f
    };
    ByteBuffer bb = ByteBuffer.allocateDirect(vertex.length * 4);
    bb.order(ByteOrder.nativeOrder());
    FloatBuffer vb = bb.asFloatBuffer();
    vb.put(vertex);
    vb.position(0);

    int[] buffers = new int[1];
    GLES20.glGenBuffers(1, buffers, 0);
    buffer = buffers[0];

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer);
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertex.length * 4, vb, GLES20.GL_DYNAMIC_DRAW);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);



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

    int[] textures = new int[1];
    GLES20.glGenTextures(1, textures, 0);
    texID = textures[0];
    // attribute 값 집어넣으려고 함.


    // Texutre space 만들기
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

    GLES20.glUseProgram(program);

    postex = GLES20.glGetUniformLocation(program, "tex");
    vpos = GLES20.glGetAttribLocation(program, "vPosition");
    tpos = GLES20.glGetAttribLocation(program, "vTexcoord");

  }


  public void draw() {
    GLES20.glDisable(GLES20.GL_DEPTH_TEST);


    GLES20.glUseProgram(program);

    // attribute 값 assgin
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer);
    GLES20.glEnableVertexAttribArray(vpos);
    GLES20.glVertexAttribPointer(vpos, 3, GLES20.GL_FLOAT, false, 5 * 4, 0);


    GLES20.glEnableVertexAttribArray(tpos);
    GLES20.glVertexAttribPointer(tpos, 2, GLES20.GL_FLOAT, false, 5*4, 3*4);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID);
    GLES20.glUniform1i(postex, 0);
        /*  또한 glUniform1i 함수를 사용하여 각 sampler를 설정함으로써 OpenGL에게
        각 shader sampler가 속하는 텍스처 유닛이 어떤 것인지를 알려주어야 합니다
         오직 한번만 설정하면 되므로 렌더링 루프에 들어가기 전에 할 수 있습니다.
         */
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

    GLES20.glDisableVertexAttribArray(vpos);
    GLES20.glDisableVertexAttribArray(tpos);
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
  }

  public void updatImage(Bitmap bmp) {

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texID);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bmp,0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

  }
}