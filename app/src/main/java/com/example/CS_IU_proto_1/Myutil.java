package com.example.CS_IU_proto_1;


import android.media.Image;
import android.opengl.Matrix;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Myutil {

    //생성된 Ray와 평면의 교차점 Return
    public static float[] pickSurfacePoints(Plane plane, Ray ray){
        // 예외 처리 하기
        float[] output = new float[3];

        float parameter = (plane.dval - plane.normal[0]*ray.origin[0] - plane.normal[1]*ray.origin[1] -plane.normal[2]*ray.origin[2])
                / (plane.normal[0]*ray.dir[0]+plane.normal[1]*ray.dir[1]+plane.normal[2]*ray.dir[2]);

        output[0] = ray.dir[0]*parameter + ray.origin[0];
        output[1] = ray.dir[1]*parameter + ray.origin[1];
        output[2] = ray.dir[2]*parameter + ray.origin[2];

        return output;

    }

    // 선택한 점을 향하는 Ray 생성
    // TODO 최적화 더 할 수 있을듯???????
    public static Ray GenerateRay(float xPx, float yPx, int screenWidth, int screenHeight,float[] projMX,float[] viewMX, float[] camera_trans) {
        // https://antongerdelan.net/opengl/raycasting.html 참고

        // screen space 에서 clip space 로
        float x = 2.0f * xPx / screenWidth - 1.0f;
        float y = 1.0f - 2.0f * yPx / screenHeight;

        float[] inverseProjMX = new float[16];
        Matrix.invertM(inverseProjMX, 0, projMX, 0);

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
        out[0] = camera_trans[0];
        out[1] = camera_trans[1];
        out[2] = camera_trans[2];

        // ray의 normalized 된 방향벡터
        out[3] = ray_wor[0] / ray_wor_length;
        out[4] = ray_wor[1] / ray_wor_length;
        out[5] = ray_wor[2] / ray_wor_length;
        Ray ray = new Ray(out);
        return ray;
    }




    public static Mat ArImg2CVImg(Image image){

        Image.Plane[] planes = image.getPlanes();

        ByteBuffer bufferY = planes[0].getBuffer();
        ByteBuffer bufferUV = planes[1].getBuffer();

        ByteBuffer bufferYUV = ByteBuffer.allocateDirect( (bufferY.remaining() +  bufferUV.remaining()) );

        bufferYUV.put( bufferY ).put( bufferUV );

        // bufferYUV = > Input FIX!!!!!!!!!!!!!!!!!
        // List<Contour>  -> output FIX!!!!!!

        // Contour

        Mat inImg = new Mat( image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
        inImg.put(0,0, bufferYUV.array());


        return inImg;
    }

    public static ArrayList<Contour> findCircle(Image image){
        Image.Plane[] planes = image.getPlanes();

        ByteBuffer bufferY = planes[0].getBuffer();
        ByteBuffer bufferUV = planes[1].getBuffer();

        ByteBuffer bufferYUV = ByteBuffer.allocateDirect( (bufferY.remaining() +  bufferUV.remaining()) );

        bufferYUV.put( bufferY ).put( bufferUV );

        return openCVProcessing(bufferYUV, image.getHeight(), image.getWidth());
    }

    private static ArrayList<Contour> openCVProcessing (ByteBuffer bb, float height, float width){

        ArrayList<Contour> contours = new ArrayList<>();
        Contour tmpContour1 = new Contour(new float[]{0.4f, 0.4f, 0.4f, -0.4f, -0.4f, -0.4f, -0.4f, 0.4f});
        Contour tmpContour2 = new Contour(new float[]{0.6f, 0.6f, 0.6f, -0.6f, -0.6f, -0.6f, -0.6f, 0.6f});
        contours.add(tmpContour1);
        contours.add(tmpContour2);

        return contours;
    }

    public static RectEllipseSize elliToRect (EllipseSize elli){

        float[] temp = new float[2];
        float[] rect_ll = new float[2];
        float[] rect_lr = new float[2];
        float[] rect_ul = new float[2];
        float[] rect_ur = new float[2];

        temp[0] = elli.p1[0] - elli.cp[0];
        temp[1] = elli.p1[1] - elli.cp[1];

        rect_ur[0] = elli.p2[0] + temp[0];
        rect_ur[1] = elli.p2[1] + temp[1];

        rect_ul[0] = elli.p2[0] - temp[0];
        rect_ul[1] = elli.p2[1] - temp[1];

        temp[0] = elli.p2[0] - elli.cp[0];
        temp[1] = elli.p2[1] - elli.cp[1];

        rect_lr[0] = rect_ur[0] - temp[0]*2;
        rect_lr[1] = rect_ur[1] - temp[1]*2;

        rect_ll[0] = rect_ul[0] - temp[0]*2;
        rect_ll[1] = rect_ul[1] - temp[1]*2;

        return new RectEllipseSize(rect_ll, rect_lr, rect_ul, rect_ur);
    }

    public static EllipseSize rectToElli (RectEllipseSize rect){
        float elli_lr, elli_sr;
        float[] elli_cp = new float[2];
        float[] elli_p1 = new float[2];
        float[] elli_p2 = new float[2];


        elli_p1[0] = (rect.ul[0] + rect.ur[0]) / 2f;
        elli_p1[1] = (rect.ul[1] + rect.ur[1]) / 2f;

        elli_p2[0] = (rect.ur[0] + rect.lr[0]) / 2f;
        elli_p2[1] = (rect.ur[1] + rect.lr[1]) / 2f;

        elli_cp[0] = (rect.ll[0] + rect.lr[0] + rect.ul[0] + rect.ur[0]) / 4f;
        elli_cp[1] = (rect.ll[1] + rect.lr[1] + rect.ul[1] + rect.ur[1]) / 4f;

        elli_lr = (float)Math.sqrt(Math.pow(elli_cp[0] - elli_p1[0] ,2) + Math.pow(elli_cp[1] - elli_p1[1] ,2));
        elli_sr = (float)Math.sqrt(Math.pow(elli_cp[0] - elli_p2[0] ,2) + Math.pow(elli_cp[1] - elli_p2[1] ,2));

        return new EllipseSize(elli_lr, elli_sr, elli_cp, elli_p1, elli_p2);
    }
    
//    public static EllipseSize findElipses ( Contour localContour){
//
//    }
//    public static RectEllipseSize findElipses ( Contour localContour){
//
//    }
}
