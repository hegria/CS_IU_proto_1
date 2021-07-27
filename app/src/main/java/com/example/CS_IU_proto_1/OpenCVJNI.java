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
import java.util.Locale;

public class OpenCVJNI {

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    public boolean isCapture = false;

    public ArrayList<Contour> findTimberContours(Image imgYUV_N12) {
        Image.Plane[] planes = imgYUV_N12.getPlanes();
        ByteBuffer bufferY   = planes[0].getBuffer();
        ByteBuffer bufferUV  = planes[1].getBuffer();
        ByteBuffer bufferYUV = ByteBuffer.allocateDirect( bufferY.remaining() + bufferUV.remaining() );
        bufferYUV.put( bufferY ).put( bufferUV );
        bufferYUV.rewind();

        if ( isCapture ) {
            isCapture = false;
            byte[] bytesYUV = new byte[ bufferYUV.remaining() ];
            bufferYUV.get(bytesYUV);
            bufferYUV.rewind();

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            String currentDateAndTime = sdf.format(new Date());

            File dst  = new File(path, "image_yuv420 " + currentDateAndTime + ".raw");
            Log.i("CaptureImage", dst.getAbsolutePath() );

            try {
                OutputStream os = new FileOutputStream(dst);
                os.write(bytesYUV);
                os.close();
            } catch (IOException e) {
                // Unable to create file, likely because external storage is
                // not currently mounted.
                Log.w("ExternalStorage", "Error writing " + dst, e);
            }
        }

        return _findTimberContours(bufferYUV, imgYUV_N12.getWidth(), imgYUV_N12.getHeight());
    }

    public ArrayList<Contour> findTimberContours(ByteBuffer bufferYUV_N12, int width, int height) {
        return _findTimberContours(bufferYUV_N12, width, height);
    }

    private native ArrayList<Contour> _findTimberContours(ByteBuffer data_yuv_n12, int width, int height);

}
