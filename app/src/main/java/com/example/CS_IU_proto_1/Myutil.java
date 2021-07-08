package com.example.CS_IU_proto_1;


import android.media.Image;
import android.opengl.Matrix;

import com.google.ar.core.Camera;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

public class Myutil {
    // Lay를 쏴서 평면위의 교차점 Return
    public static float[] pickSurfacePoints(Plane plane, float[] rayorigin, float[] raydir){
        // 예외 처리 하기
        float[] output = new float[3];

        float parameter = (plane.dval - plane.normal[0]*rayorigin[0] - plane.normal[1]*rayorigin[1] -plane.normal[2]*rayorigin[2])
                / (plane.normal[0]*raydir[0]+plane.normal[1]*raydir[1]+plane.normal[2]*raydir[2]);

        output[0] = raydir[0]*parameter + rayorigin[0];
        output[1] = raydir[1]*parameter + rayorigin[1];
        output[2] = raydir[2]*parameter + rayorigin[2];

        return output;

    }


    public static float[] rayPicking(float xPx, float yPx, int screenWidth, int screenHeight, Camera camera) {
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


    public static Mat ArImg2CVImg(Image image){
        /* Image.Plane Y = image.getPlanes()[0];
         Image.Plane U = image.getPlanes()[1];

         int Yb = Y.getBuffer().remaining();
         int Ub = U.getBuffer().remaining();

         byte[] data = new byte[Yb + Ub];

         Y.getBuffer().get(data, 0, Yb);
         U.getBuffer().get(data, Yb, Ub);

         Mat matInput2 = new Mat(image.getHeight()+image.getHeight()/2,image.getWidth(), CvType.CV_8UC1);
         matInput2.put(0,0,data);

         Mat matoutput = new Mat(image.getHeight(),image.getWidth(),CvType.CV_8UC4);

         Imgproc.cvtColor(matInput2,matoutput,Imgproc.COLOR_YUV2RGB_NV12);*/

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer bufferY = planes[0].getBuffer();
        ByteBuffer bufferUV = planes[1].getBuffer();

        ByteBuffer bufferYUV = ByteBuffer.allocate( (bufferY.remaining() +  bufferUV.remaining()) );
        bufferYUV.put( bufferY ).put( bufferUV );

        Mat inImg = new Mat( image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
        // bufferdata가 안됨...
        inImg.put(0,0, bufferYUV.array());


        Mat outImg = new Mat( image.getHeight(), image.getWidth(), CvType.CV_8UC4 );

        Mat outoutImg = new Mat( image.getHeight(), image.getWidth(), CvType.CV_8UC4 );

        // // YUV to RGB -> cvCvtColor();
        Imgproc.cvtColor( inImg, outImg, Imgproc.COLOR_YUV2RGB_NV12 );
        Imgproc.threshold(outImg,outoutImg,100,255,Imgproc.THRESH_BINARY_INV);

        return outoutImg;
    }
}
