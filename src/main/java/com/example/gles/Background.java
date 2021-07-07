package com.example.CS_IU_proto_1;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Background {
  int program;
  int texID;

  int[] textures = new int[1];
  FloatBuffer vb;
  FloatBuffer tb;

  private final String vscode = "" +
          "attribute vec2 vPosition;\n" +
          "attribute vec2 vTexcoord;\n" +
          "varying vec2 tc;\n" +

          "void main() {\n" +
          "  gl_Position = vec4(vPosition, 1.0, 1.0);\n" +
          "  tc = vTexcoord;" +
          "}\n";

  private final String fscode = "" +
          "#extension GL_OES_EGL_image_external : require\n" +
          "" +
          "precision mediump float;" +
          "uniform samplerExternalOES tex;\n" +
          "varying vec2 tc;\n" +

          "void main() {\n" +
          "  gl_FragColor = texture2D(tex, tc);\n" +
          "}\n";

  public Background() {
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

    float[] vertices = new float[]{-1.0f, -1.0f, +1.0f, -1.0f, -1.0f, +1.0f, +1.0f, +1.0f,};
    vb = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    vb.put(vertices);
    vb.position(0);

    tb = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

    GLES20.glGenTextures(1, textures, 0);
    texID = textures[0];
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texID);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
  }


  public void draw(Frame frame) {
    if (frame.hasDisplayGeometryChanged())
      frame.transformCoordinates2d(
              Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, vb,
              Coordinates2d.TEXTURE_NORMALIZED, tb);
    tb.position(0);

    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    GLES20.glUseProgram(program);

    int pos = GLES20.glGetAttribLocation(program, "vPosition");
    GLES20.glEnableVertexAttribArray(pos);
    GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 0, vb);

    pos = GLES20.glGetAttribLocation(program, "vTexcoord");
    GLES20.glEnableVertexAttribArray(pos);
    GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 0, tb);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texID);
    pos = GLES20.glGetUniformLocation(program, "tex");
    GLES20.glUniform1i(pos, GLES20.GL_TEXTURE0);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    GLES20.glDisableVertexAttribArray(pos);
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
  }
}
