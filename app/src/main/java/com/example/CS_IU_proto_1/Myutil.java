package com.example.CS_IU_proto_1;


import android.media.Image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

public class Myutil {

    public static float[] pickPoints(Plane plane, float[] rayorigin, float[] raydir){
        // 예외 처리 하기
        float[] output = new float[3];

        float parameter = (plane.dval - plane.normal[0]*rayorigin[0] - plane.normal[1]*rayorigin[1] -plane.normal[2]*rayorigin[2])
                / (plane.normal[0]*raydir[0]+plane.normal[1]*raydir[1]+plane.normal[2]*raydir[2]);

        output[0] = raydir[0]*parameter + rayorigin[0];
        output[1] = raydir[1]*parameter + rayorigin[1];
        output[2] = raydir[2]*parameter + rayorigin[2];

        return output;

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
