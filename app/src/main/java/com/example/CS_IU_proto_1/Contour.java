package com.example.CS_IU_proto_1;

import android.opengl.Matrix;
import android.util.Log;

// OpenCV팀에서 아웃풋으로 뱉을 거
public class Contour {


    public float[] points;

    public Contour(float[] _points){
        points = _points;
    }

    // Contour의 points 좌표를 Local 좌표로 바꾸는.....일을 하였음.
    // TODO 최적화 더 할 수 있음.
    public Contour cliptolocal(float[] projMX, float[] viewMX, float[] camera, Plane plane){
        int len = points.length/2;

        float[] localpoints = new float[points.length];

        float[] ray_clip;
        float[] ray_eye;
        float[] ray_wor;
        float[] out;
        float[] inverseProjMX = new float[16];
        Matrix.invertM(inverseProjMX, 0, projMX, 0);

        float[] inverseViewMX = new float[16];
        Matrix.invertM(inverseViewMX, 0, viewMX, 0);
        Ray ray;

        float[] surpoints;
        float[] localpoint;
        for( int i = 0; i<len;i++){
            ray_clip = new float[]{-points[2*i+1], points[2*i], -1f, 1f};
            ray_eye = new float[4];
            Matrix.multiplyMV(ray_eye, 0, inverseProjMX, 0, ray_clip, 0);
            ray_eye = new float[]{ray_eye[0], ray_eye[1], -1.0f, 0.0f};

            ray_wor = new float[4];
            Matrix.multiplyMV(ray_wor, 0, inverseViewMX, 0, ray_eye, 0);
            float ray_wor_length = (float) Math.sqrt(ray_wor[0] * ray_wor[0] + ray_wor[1] * ray_wor[1] + ray_wor[2] * ray_wor[2]);
            out = new float[]{ray_wor[0]/ray_wor_length, ray_wor[1]/ray_wor_length ,ray_wor[2]/ray_wor_length};
            ray = new Ray(camera,out);
            surpoints = Myutil.pickSurfacePoints(plane,ray);
            localpoint = plane.transintolocal(surpoints);
            localpoints[2*i] = localpoint[0];
            localpoints[2*i+1] = localpoint[1];
        }
        Contour localContor = new Contour(localpoints);

        return localContor;
    }


}
