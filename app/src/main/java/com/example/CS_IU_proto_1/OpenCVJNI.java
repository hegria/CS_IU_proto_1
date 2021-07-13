package com.example.CS_IU_proto_1;

import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class OpenCVJNI {

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    public ArrayList<Contour> findTimberContours(Image imgYUV_N12) {
        Image.Plane[] planes = imgYUV_N12.getPlanes();
        ByteBuffer bufferY   = planes[0].getBuffer();
        ByteBuffer bufferUV  = planes[1].getBuffer();
        ByteBuffer bufferYUV = ByteBuffer.allocateDirect( bufferY.remaining() + bufferUV.remaining() );
        bufferYUV.put( bufferY ).put( bufferUV );

        return _findTimberContours(bufferYUV, imgYUV_N12.getWidth(), imgYUV_N12.getHeight());
    }

    public ArrayList<Contour> findTimberContours(ByteBuffer bufferYUV_N12, int width, int height) {
        return _findTimberContours(bufferYUV_N12, width, height);
    }

    private native ArrayList<Contour> _findTimberContours(ByteBuffer data_yuv_n12, int width, int height);

}
