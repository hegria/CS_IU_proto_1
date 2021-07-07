package com.example.CS_IU_proto_1;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Circle {
    int program;
    int vBuffer;

    int numpoints;

    private final String vscode = "" +
            "attribute vec3 vPosition;" +

            "uniform mat4 viewMX;" +
            "uniform mat4 projMX;" +

            "void main() {" +
            "  gl_Position = projMX * viewMX * vec4(vPosition, 1.0);" +
            "  gl_PointSize = 10.0;" +
            "}";

    private final String fscode = "" +
            "precision mediump float;" +

            "void main() {" +
            "  gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);" +
            "}";

    public Circle() {
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
    public void setCircle(Plane plane, float[] point){
        float[] newpoint = plane.transintolocal(point);

        FloatBuffer pointsBuffer = ByteBuffer.allocateDirect(4*3*101).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for(int i =0; i<101;i++){
            float[] temppoint = {(float) (newpoint[0]+0.01f*Math.cos(i*Math.PI/50)), (float) (newpoint[1]+0.01f*Math.sin(i*Math.PI/50)),newpoint[2]};
            float[] newtemp = plane.transintoworld(temppoint);
            pointsBuffer.put(newtemp);
        }
        pointsBuffer.position(0);
        numpoints = 101;

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
        GLES20.glVertexAttribPointer(vPos, 3, GLES20.GL_FLOAT, false, 3 * 4, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        int pos = GLES20.glGetUniformLocation(program, "viewMX");
        GLES20.glUniformMatrix4fv(pos, 1, false, viewMX, 0);

        pos = GLES20.glGetUniformLocation(program, "projMX");
        GLES20.glUniformMatrix4fv(pos, 1, false, projMX, 0);
        GLES20.glLineWidth(5.0f);
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, numpoints);
        GLES20.glDisableVertexAttribArray(vPos);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}

