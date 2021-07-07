package com.example.CS_IU_proto_1;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class SimpleDraw {
  private int width, height;
  int vBuffer;

  private int program;

  private final String vscode =
          "attribute vec4 vPosition;\n" +

                  "uniform mat4 projMX;\n" +
                  "uniform mat4 viewMX;\n" +

                  "void main() {\n" +
                  "  gl_Position = projMX * viewMX * vec4(vec3(vPosition), 1.0);\n" +
                  "  gl_PointSize = 30.0;\n" +
                  "}\n";

  private final String fscode =
          "precision mediump float;\n" +
                  "uniform vec3 color;\n" +

                  "void main() {\n" +
                  "  gl_FragColor = vec4(color, 1.0);\n" +
                  "}\n";


  public SimpleDraw() {
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


  public void draw(float[] object, int objectType, int numPerCoord, float r, float g, float b, float[] viewMX, float[] projMX) {
    FloatBuffer vb = ByteBuffer.allocateDirect(object.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    vb.put(object);
    vb.position(0);

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vBuffer);
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, object.length * 4, vb, GLES20.GL_DYNAMIC_DRAW);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


    GLES20.glUseProgram(program);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vBuffer);

    int pos = GLES20.glGetAttribLocation(program, "vPosition");
    GLES20.glEnableVertexAttribArray(pos);
    GLES20.glVertexAttribPointer(pos, numPerCoord, GLES20.GL_FLOAT, false, numPerCoord * 4, 0);
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);


    pos = GLES20.glGetUniformLocation(program, "viewMX");
    GLES20.glUniformMatrix4fv(pos, 1, false, viewMX, 0);


    pos = GLES20.glGetUniformLocation(program, "projMX");
    GLES20.glUniformMatrix4fv(pos, 1, false, projMX, 0);

    pos = GLES20.glGetUniformLocation(program, "color");
    GLES20.glUniform3f(pos, r, g, b);

    GLES20.glDrawArrays(objectType, 0, object.length / numPerCoord);
    GLES20.glDisableVertexAttribArray(pos);
  }
}
