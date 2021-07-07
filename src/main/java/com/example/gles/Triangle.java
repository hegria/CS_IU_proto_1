package com.example.CS_IU_proto_1;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Triangle {
  int buffer;
  int program;

  int width, height;

  private final String vscode = "" +
          "attribute vec3 vPosition;" +
          "attribute vec3 vColor;" +
          "" +
          "uniform mat4 aspectMX;" +
          "" +
          "varying vec3 color;" +
          "" +
          "void main() {" +
          "  gl_Position = aspectMX * vec4(vPosition, 1.0);" +
          "  color = vColor;" +
          "}";

  private final String fscode = "" +
          "precision mediump float;" +
          "" +
          "uniform float a;" +
          "" +
          "varying vec3 color;" +
          "" +
          "void main() {" +
          "  gl_FragColor = vec4(color * a, 1.0);" +
          "}";

  public Triangle() {
    float[] vertex = {
            0.0f, 1.0f, 0.0f, /*x, y, z*/ 1.0f, 0.0f, 0.0f, /*r, g, b*/
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f
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

    int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
    GLES20.glShaderSource(vs, vscode);
    GLES20.glCompileShader(vs);
    int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
    GLES20.glShaderSource(fs, fscode);
    GLES20.glCompileShader(fs);

    program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vs);
    GLES20.glAttachShader(program, fs);
    GLES20.glLinkProgram(program);
  }

  float time = 0.0f;

  public void draw(float dt) {
    time += dt;

    GLES20.glUseProgram(program);

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer);
    int vpos = GLES20.glGetAttribLocation(program, "vPosition");
    GLES20.glEnableVertexAttribArray(vpos);
    GLES20.glVertexAttribPointer(vpos, 3, GLES20.GL_FLOAT, false, 6 * 4, 0);

    int cpos = GLES20.glGetAttribLocation(program, "vColor");
    GLES20.glEnableVertexAttribArray(cpos);
    GLES20.glVertexAttribPointer(cpos, 3, GLES20.GL_FLOAT, false, 6 * 4, 3 * 4);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    float ratio = (float) width / (float) height;
    float[] aspectMX = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, ratio, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    int pos = GLES20.glGetUniformLocation(program, "aspectMX");
    GLES20.glUniformMatrix4fv(pos, 1, false, aspectMX, 0);

    pos = GLES20.glGetUniformLocation(program, "a");
    GLES20.glUniform1f(pos, (float) ((float) Math.cos(time) + 1.0f) / 2.0f);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

    GLES20.glDisableVertexAttribArray(vpos);
    GLES20.glDisableVertexAttribArray(cpos);
  }

  public void changeSize(int width, int height) {
    this.width = width;
    this.height = height;
  }
}
