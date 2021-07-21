package com.example.CS_IU_proto_1;

import android.opengl.Matrix;
import android.os.Debug;
import android.util.Log;

import com.google.ar.core.Camera;

import java.util.Arrays;

//Local
public class Ellipse {
    float yrad;
    float xrad;
    float[] pivot;
    float[] xaxis;
    float[] yaxis;
    float[][] modelmat;
    public float[] worldpivot;
    public int size;
    public float[] resultprivot;

    public Ellipse(float _r1, float _r2, float[] _pivot, float[] _xaxis, float[] _yaxis) {
        xrad = _r1;
        yrad = _r2;
        pivot = _pivot;
        xaxis = _xaxis;
        yaxis = _yaxis;

        size = (int)( (yrad + xrad) *100);
    }

    public void setRottation(Plane plane){
        worldpivot = plane.transintoworld(pivot);
        float[] newxvec = new float[]{plane.xvec[0]*xaxis[0]+plane.yvec[0]*xaxis[1],plane.xvec[1]*xaxis[0]+plane.yvec[1]*xaxis[1],plane.xvec[2]*xaxis[0]+plane.yvec[2]*xaxis[1]};
        float[] newyvec = new float[]{plane.xvec[0]*yaxis[0]+plane.yvec[0]*yaxis[1],plane.xvec[1]*yaxis[0]+plane.yvec[1]*yaxis[1],plane.xvec[2]*yaxis[0]+plane.yvec[2]*yaxis[1]};
        modelmat = new float[][]{
                {xrad*newxvec[0],yrad*newyvec[0],plane.normal[0],worldpivot[0]},
                {xrad*newxvec[1],yrad*newyvec[1],plane.normal[1],worldpivot[1]},
                {xrad*newxvec[2],yrad*newyvec[2],plane.normal[2],worldpivot[2]},
                {0,0,0,1}
        };
    }

    public void pivot_to_local(float[] projMX, float[] viewMX) {
        float[] MVPmx = new float[16];
        Matrix.multiplyMM(MVPmx,0,projMX,0,viewMX,0);
        float[] thisworldpivot = new float[]{worldpivot[0],worldpivot[1],worldpivot[2],1.0f};
        resultprivot = new float[4];
        Matrix.multiplyMV(resultprivot,0,MVPmx,0,thisworldpivot,0);
        for(int i =0;i<3;i++){
            resultprivot[i]/=resultprivot[3];
        }
    }

    public float[] transtoworld(float[] point){
        float[] points = new float[]{point[0],point[1],0,1};
        float[] newpoint = new float[4];
        for(int i =0;i<4;i++){
            for(int j=0;j<4;j++){
                newpoint[i]+=modelmat[i][j]*points[j];
            }
        }
        return new float[]{newpoint[0],newpoint[1],newpoint[2]};


    }
}
