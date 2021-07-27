package com.example.CS_IU_proto_1;

import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;


//원래 코드
//----------------------------------------------------------
public class OpenCVJNI {

    ByteBuffer bufferYUV;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    public ArrayList<Contour> findTimberContours(Image imgYUV_N12) {
        Image.Plane[] planes = imgYUV_N12.getPlanes();
        ByteBuffer bufferY   = planes[0].getBuffer();
        ByteBuffer bufferUV  = planes[1].getBuffer();
        bufferYUV.clear();
        bufferYUV.put( bufferY ).put( bufferUV );
        bufferYUV = ByteBuffer.allocateDirect(bufferY.limit() + bufferUV.limit());


        return _findTimberContours(bufferYUV, imgYUV_N12.getWidth(), imgYUV_N12.getHeight());
    }

    public ArrayList<Contour> findTimberContours(ByteBuffer bufferYUV_N12, int width, int height) {
        return _findTimberContours(bufferYUV_N12, width, height);
    }

    private native ArrayList<Contour> _findTimberContours(ByteBuffer data_yuv_n12, int width, int height);

}
//----------------------------------------------------------

//변경 코드
//allocateDirect를 딱 한번만 해줌
//----------------------------------------------------------
//public class OpenCVJNI {
//
//    ByteBuffer bufferYUV;
//
//    static {
//        System.loadLibrary("opencv_java4");
//        System.loadLibrary("native-lib");
//    }
//
//    //Y버퍼 + UV버퍼 limit 값 출력했을 때는 3110399 이였음
//    public OpenCVJNI() {
//        bufferYUV = ByteBuffer.allocateDirect(3120000);
//    }
//
//    public ArrayList<Contour> findTimberContours(Image imgYUV_N12) {
//        Image.Plane[] planes = imgYUV_N12.getPlanes();
//        ByteBuffer bufferY   = planes[0].getBuffer();
//        ByteBuffer bufferUV  = planes[1].getBuffer();
//        bufferYUV.clear();
//        bufferYUV.put( bufferY ).put( bufferUV );
//
//        return _findTimberContours(bufferYUV, imgYUV_N12.getWidth(), imgYUV_N12.getHeight());
//    }
//
//    public ArrayList<Contour> findTimberContours(ByteBuffer bufferYUV_N12, int width, int height) {
//        return _findTimberContours(bufferYUV_N12, width, height);
//    }
//
//    private native ArrayList<Contour> _findTimberContours(ByteBuffer data_yuv_n12, int width, int height);
//
//}
//----------------------------------------------------------