package com.example.CS_IU_proto_1;

import android.media.Image;
import android.os.Environment;
import android.util.Log;
import android.content.ContextWrapper;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OpenCVJNI {

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    //private final int[] imgProcessMatIDs;

    public OpenCVJNI() {
        //imgProcessMatIDs = new int[5];
    }

    public ArrayList<Contour> findTimberContours(Image imgYUV_N12, int resizelvl, int normlvl, int closelvl, int openlvl, double markerlvl, boolean bg_enable_filtering, double filterlvl) {
        Image.Plane[] planes = imgYUV_N12.getPlanes();
        ByteBuffer bufferY   = planes[0].getBuffer();
        ByteBuffer bufferUV  = planes[1].getBuffer();
        ByteBuffer bufferYUV = ByteBuffer.allocateDirect( bufferY.remaining() + bufferUV.remaining() );
        bufferYUV.put( bufferY ).put( bufferUV );
        bufferYUV.rewind();

        return _findTimberContours(bufferYUV, imgYUV_N12.getWidth(), imgYUV_N12.getHeight(), resizelvl, normlvl, closelvl, openlvl, markerlvl, bg_enable_filtering, filterlvl);
    }

    public ArrayList<Contour> findTimberContours(ByteBuffer bufferYUV_N12, int width, int height, int resizelvl, int normlvl, int closelvl, int openlvl, double markerlvl, boolean bg_enable_filtering, double filterlvl) {
        return _findTimberContours(bufferYUV_N12, width, height, resizelvl, normlvl, closelvl, openlvl, markerlvl, bg_enable_filtering, filterlvl);
    }

    private native ArrayList<Contour> _findTimberContours(ByteBuffer data_yuv_n12, int width, int height, int resizelvl, int normlvl, int closelvl, int openlvl, double markerlvl, boolean bg_enable_filtering, double filterlvl);

}
