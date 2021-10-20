package com.example.CS_IU_proto_1;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.Image;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class OpenCVJNI {

    private static final String ModelDir = "trained_model/";
    private static final String TrainedModel = "svmModel.xml";
    private boolean xmlUnavailable;
    private boolean modHOG = false;
    private double threshold = 0.5f;

    private final ByteBuffer bufferYUV;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    public void useHOG(boolean val) {
        if (xmlUnavailable) return;
        modHOG = val;
    }

    public void setThreshold(double val) {
        threshold = val;
    }

    //Y버퍼 + UV버퍼 limit 값 출력했을 때는 3110399 이였음
    public OpenCVJNI(Context ctx) {
        bufferYUV = ByteBuffer.allocateDirect(3120000);
        xmlUnavailable = (ctx == null);
        if (!xmlUnavailable) xmlUnavailable = !initHOG(ctx);
    }

    public OpenCVJNI() {
        this(null);
    }

    public ArrayList<Contour> findTimberContours(Image imgYUV_N12) {
        Image.Plane[] planes = imgYUV_N12.getPlanes();
        ByteBuffer bufferY   = planes[0].getBuffer();
        ByteBuffer bufferUV  = planes[1].getBuffer();
        bufferYUV.clear();
        bufferYUV.put( bufferY ).put( bufferUV );

        if (modHOG)
            return _findTimberContours2(bufferYUV, imgYUV_N12.getWidth(), imgYUV_N12.getHeight(), threshold);
        else
            return _findTimberContours(bufferYUV, imgYUV_N12.getWidth(), imgYUV_N12.getHeight());
    }

    private boolean initHOG(Context ctx) {
        File f = new File(ctx.getFilesDir(), TrainedModel);
        if ( f.exists() && f.isFile() || copyToInternalStorage(f, ctx.getAssets()) ) {
            loadTrainedModel(f.getAbsolutePath());
            return true;
        }
        return false;
    }

    private boolean copyToInternalStorage(File internalStorage, AssetManager am) {
        try (
                InputStream in = am.open(ModelDir + TrainedModel);
                OutputStream out = new FileOutputStream(internalStorage);
        ) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
            return true;
        } catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

    public native void setTH(int lvl);
    public native void setMORPHO(int lvl);
    public native void setMORPHC(int lvl);
    public native void enableBG(boolean b);
    public native void setMARKTH(int th);
    public native void setMARKP1(int p1);
    public native void setCNTRTH(double th);

    private native ArrayList<Contour> _findTimberContours(ByteBuffer data_yuv_n12, int width, int height);
    private native ArrayList<Contour> _findTimberContours2(ByteBuffer data_yuv_n12, int width, int height, double th);
    private native void loadTrainedModel(String model);

}
