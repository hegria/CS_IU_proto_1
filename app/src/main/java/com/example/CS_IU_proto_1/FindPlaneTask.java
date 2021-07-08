package com.example.CS_IU_proto_1;


import android.util.Log;

import com.curvsurf.fsweb.FindSurfaceRequester;
import com.curvsurf.fsweb.RequestForm;
import com.curvsurf.fsweb.ResponseForm;
import com.google.ar.core.Camera;

import java.nio.FloatBuffer;
import java.util.concurrent.Callable;

public class FindPlaneTask implements Callable<Boolean>
{
    //TODO listener로 구현하기
//    public interface FindPlaneTaskListener {
//        public void onSuccessTask(Plane plane);
//        public void onFailTask();
//    }

    private static final String REQUEST_URL = "https://developers.curvsurf.com/FindSurface/plane"; // Plane searching server address
    private static final float circleRad = 0.25f;

    FindSurfaceRequester fsr = new FindSurfaceRequester( REQUEST_URL ,false);

    // Reuseable Variables...
    FloatBuffer points = null;
    int seedPointID;
    // Result
    Plane       plane = null;
    // 점을 선택하는 과정을 FindPlane에 집어 넣었다.
    float[] layinfo = new float[6];
    float[] z_axis = new float[3];
    float[] seedPointArr = new float[]{0.0f, 0.0f, 0.0f, 1.0f}; // ??

    public void resetSeedPoint(){
        seedPointID = -1;
        seedPointArr = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    }


    // 카메라를 그냥 받아라 ㅅㅂ아
    public void initTask( FloatBuffer _points, float[] _layinfo, float[] _z_axis) {
        points = _points.duplicate();

        layinfo = _layinfo;
        z_axis = _z_axis;

        plane  = null;
    }

    @Override // - Callable<>
    public Boolean call() {
        // Ray Picking
        int pointCount  = points.capacity() / 4;
        pickPoint(points,layinfo);
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
                    return false;
                }else{

                    plane = new Plane( param, z_axis );
                    return  true;
                }

                // Success notification...
            } else{
                Log.d("Plane","request fail");
                resetSeedPoint();
                return  false;
            }
        }
        catch(Exception e) {
            e.printStackTrace();

            // Error Handling
        }
        return null;
    }

    private void pickPoint(FloatBuffer filterPoints, final float[] layinfo) {
        // camera: 카메라의 world space 위치(x,y,z), ray : ray의 방향벡터
        float minDistanceSq = Float.MAX_VALUE;

        filterPoints.rewind();
        float[] point;
        float[] product;
        for (int i = 0; i < filterPoints.remaining(); i += 4) {
            point = new float[]{filterPoints.get(i), filterPoints.get(i + 1), filterPoints.get(i + 2), filterPoints.get(i + 3)};
            product = new float[]{point[0] - layinfo[0], point[1] - layinfo[1], point[2] - layinfo[2], 1.0f};

            // 카메라 -> 특징점 벡터의 크기와, 이 벡터와 카메라 -> ray 벡터를 내적한 값을 이용해 피타고라스정리
            float distanceSq = product[0] * product[0] + product[1] * product[1] + product[2] * product[2];
            float innerProduct = layinfo[3] * product[0] + layinfo[4] * product[1] + layinfo[5] * product[2];
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