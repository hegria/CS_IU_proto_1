package com.example.CS_IU_proto_1;


import android.util.Log;

import com.curvsurf.fsweb.FindSurfaceRequester;
import com.curvsurf.fsweb.RequestForm;
import com.curvsurf.fsweb.ResponseForm;

import java.nio.FloatBuffer;

public class FindPlaneTask implements Runnable
{
    public interface FindPlaneTaskListener {
        public void onSuccessTask(Plane plane);
        public void onFailTask();
    }

    //상수들
    private static final String REQUEST_URL = "https://developers.curvsurf.com/FindSurface/plane"; // Plane searching server address
    private static final float circleRad = 0.25f;

    FindSurfaceRequester fsr = new FindSurfaceRequester( REQUEST_URL ,false);
    FindPlaneTaskListener findPlaneTaskListener;

    // 원래는 버려도 되는 값인데 Debug를 위해 남겨둠 -> 왕코로 그려짐
    int seedPointID;
    float[] seedPointArr = new float[]{0.0f, 0.0f, 0.0f, 1.0f}; // ??

    // Reuseable Variables...
    FloatBuffer points = null;

    // 받는 변수들
    private Ray ray;
    private float[] z_axis;

    // For Debugg
    public void resetSeedPoint(){
        seedPointID = -1;
        seedPointArr = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    }

    public void setFindPlaneTaskListener(FindPlaneTaskListener _findPlaneTaskListener){
        findPlaneTaskListener = _findPlaneTaskListener;
    }

    public void initTask( FloatBuffer _points, Ray _ray, float[] _z_axis) {
        points = _points.duplicate();
        ray = _ray;
        z_axis = _z_axis;

    }

    @Override // runnable
    public void run() {
        // Ray Picking
        int pointCount  = points.capacity() / 4;
        pickSeedPoint(points,ray);
        float z_dist = 1.0f;

        RequestForm rf = new RequestForm();
        rf.setPointBufferDescription(pointCount, 16, 0); //pointcount, pointstride, pointoffset
        rf.setPointDataDescription(0.05f, 0.02f); //accuracy, meanDistance
        rf.setTargetROI(seedPointID, Math.max(z_dist * circleRad, 0.05f));//seedIndex,touchRadius
        rf.setAlgorithmParameter(RequestForm.SearchLevel.LV7, RequestForm.SearchLevel.NORMAL);//LatExt, RadExp
        try {
            ResponseForm resp = fsr.request( rf, points );
            if( resp != null &&resp.isSuccess() ) {
                ResponseForm.PlaneParam param = resp.getParamAsPlane();

                Log.d("Plane","request sucess");
                if(param == null){
                    Log.d("Plane", "평면 추출 실패");
                    resetSeedPoint();
                    findPlaneTaskListener.onFailTask();

                }else{

                    findPlaneTaskListener.onSuccessTask(new Plane( param, z_axis ));

                }

                // Success notification...
            } else{
                Log.d("Plane","request fail");
                resetSeedPoint();
                findPlaneTaskListener.onFailTask();

            }
            return;
        }
        catch(Exception e) {
            e.printStackTrace();

            // Error Handling
        }
        return;
    }

    // 선택한 포인트에서 가장 가까운 특징 점 찾기
    private void pickSeedPoint(FloatBuffer filterPoints, final Ray ray) {
        // camera: 카메라의 world space 위치(x,y,z), ray : ray의 방향벡터
        float minDistanceSq = Float.MAX_VALUE;

        filterPoints.rewind();
        float[] point;
        float[] product;
        for (int i = 0; i < filterPoints.remaining(); i += 4) {
            point = new float[]{filterPoints.get(i), filterPoints.get(i + 1), filterPoints.get(i + 2), filterPoints.get(i + 3)};
            product = new float[]{point[0] - ray.origin[0], point[1] - ray.origin[1], point[2] - ray.origin[2], 1.0f};

            // 카메라 -> 특징점 벡터의 크기와, 이 벡터와 카메라 -> ray 벡터를 내적한 값을 이용해 피타고라스정리
            float distanceSq = product[0] * product[0] + product[1] * product[1] + product[2] * product[2];
            float innerProduct = ray.dir[0] * product[0] + ray.dir[1] * product[1] + ray.dir[2] * product[2];
            distanceSq = distanceSq - (innerProduct * innerProduct);

            if (distanceSq < 0.01f && distanceSq < minDistanceSq) {
                //찾은 점의 index.
                seedPointArr[0] = point[0];
                seedPointArr[1] = point[1];
                seedPointArr[2] = point[2];
                seedPointID = i / 4;
                minDistanceSq = distanceSq;
            }
        }
    }
}