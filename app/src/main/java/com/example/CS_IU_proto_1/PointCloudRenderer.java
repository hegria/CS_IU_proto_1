package com.example.CS_IU_proto_1;

import android.opengl.GLES20;

import com.google.ar.core.PointCloud;

import java.nio.FloatBuffer;

public class PointCloudRenderer {
  int program;
  int vBuffer;

  private final String vscode = "" +
          "attribute vec4 vPosition;" +

          "uniform mat4 viewMX;" +
          "uniform mat4 projMX;" +

          "void main() {" +
          "  gl_Position = projMX * viewMX * vec4(vec3(vPosition), 1.0);" +
          "  gl_PointSize = 10.0;" +
          "}";

  private final String fscode = "" +
          "precision mediump float;" +

          "void main() {" +
          "  gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);" +
          "}";

  public PointCloudRenderer() {
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

    int[] buffers = new int[1];
    GLES20.glGenBuffers(1, buffers, 0);
    vBuffer = buffers[0];
  }

  long lastFrame = 0;
  int numPoints = 0;

  public void update(PointCloud pointCloud) {
    if (lastFrame == pointCloud.getTimestamp()) return;
    lastFrame = pointCloud.getTimestamp();

    numPoints = pointCloud.getPoints().remaining() / 4;

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vBuffer);
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, pointCloud.getPoints().remaining() * 4, pointCloud.getPoints(), GLES20.GL_DYNAMIC_DRAW);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
  }

  public void fix(FloatBuffer pointsBuffer) {
    pointsBuffer.position(0);

    numPoints = pointsBuffer.remaining() / 4;

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vBuffer);
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, pointsBuffer.remaining() * 4, pointsBuffer, GLES20.GL_DYNAMIC_DRAW);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
  }

  public void draw(float[] viewMX, float[] projMX) {
    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    GLES20.glUseProgram(program);

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vBuffer);
    int vPos = GLES20.glGetAttribLocation(program, "vPosition");
    GLES20.glEnableVertexAttribArray(vPos);
    GLES20.glVertexAttribPointer(vPos, 4, GLES20.GL_FLOAT, false, 4 * 4, 0);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    int pos = GLES20.glGetUniformLocation(program, "viewMX");
    GLES20.glUniformMatrix4fv(pos, 1, false, viewMX, 0);

    pos = GLES20.glGetUniformLocation(program, "projMX");
    GLES20.glUniformMatrix4fv(pos, 1, false, projMX, 0);

    GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints);
    GLES20.glDisableVertexAttribArray(vPos);
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
  }
}
