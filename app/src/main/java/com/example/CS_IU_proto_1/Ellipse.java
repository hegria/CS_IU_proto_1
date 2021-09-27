package com.example.CS_IU_proto_1;

import android.opengl.Matrix;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

//Local
public class Ellipse implements Parcelable {
    boolean istoggled = true;
    boolean isCircle = false;
    boolean isResult = false;
    float yrad;
    float xrad;
    float[] pivot;
    float[] xaxis;
    float[] yaxis;
    float[][] modelmat;
    float[] modelmat0;
    float[] modelmat1;
    float[] modelmat2;
    float[] modelmat3;
    public float[] worldpivot;
    public int size;
    public float size2;

    // local pivot
    public float[] resultpivot;

    public Ellipse(float _r1, float _r2, float[] _pivot, float[] _xaxis, float[] _yaxis) {
        xrad = _r1;
        yrad = _r2;
        pivot = _pivot;
        xaxis = _xaxis;
        yaxis = _yaxis;

        size = (int)( (yrad + xrad) *100);
        size2 = (yrad + xrad) *100f;
    }

    // 그냥 하나.

    public Ellipse(Ray ray, Plane plane, float[] projMX, float[] viewMX){
        isCircle = true;
        xrad = 0.01f;
        yrad = 0.01f;

        size = (int)( (yrad + xrad) *100);
        size2 = (yrad + xrad) *100f;
        movepivot(ray,plane,projMX,viewMX);
    }

    protected Ellipse(Parcel in) {
        istoggled = in.readByte() != 0;
        worldpivot = in.createFloatArray();
        xaxis = in.createFloatArray();
        yaxis = in.createFloatArray();
        modelmat0 = in.createFloatArray();
        modelmat1 = in.createFloatArray();
        modelmat2 = in.createFloatArray();
        modelmat3 = in.createFloatArray();
        xrad = in.readFloat();
        yrad = in.readFloat();
        size = in.readInt();
        size2 = in.readFloat();
        resultpivot = in.createFloatArray();
        modelmat = new float[][] {
                modelmat0,modelmat1,modelmat2,modelmat3
        };
        isResult = true;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (istoggled ? 1 : 0));
        dest.writeFloatArray(worldpivot);
        dest.writeFloatArray(xaxis);
        dest.writeFloatArray(yaxis);
        dest.writeFloatArray(modelmat0);
        dest.writeFloatArray(modelmat1);
        dest.writeFloatArray(modelmat2);
        dest.writeFloatArray(modelmat3);
        dest.writeFloat(xrad);
        dest.writeFloat(yrad);
        dest.writeInt(size);
        dest.writeFloat(size2);
        dest.writeFloatArray(resultpivot);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Ellipse> CREATOR = new Creator<Ellipse>() {
        @Override
        public Ellipse createFromParcel(Parcel in) {
            return new Ellipse(in);
        }

        @Override
        public Ellipse[] newArray(int size) {
            return new Ellipse[size];
        }
    };

    public void movepivot(Ray ray, Plane plane, float[] projMX, float[] viewMX){
        //TODO 변경해야함
        worldpivot = Myutil.pickSurfacePoints(plane,ray);
        pivot_to_local(projMX,viewMX);
        setCircleRotatation(plane);
    }

    public void changerad(float rad,Plane plane){
        //TODO 변경해야함
        if(isCircle){
            xrad = rad/200;
            yrad = rad/200;

            size = (int)( (yrad + xrad) *100);
            size2 = (yrad + xrad) *100f;
            setCircleRotatation(plane);
        }else{
            float ratio = rad/size2;
            xrad *= ratio;
            yrad *= ratio;

            size = (int)(size * ratio);
            size2 *= ratio;
            setRottation(plane);
        }
    }

    public void setRottation(Plane plane){
        if(!isResult) {
            worldpivot = plane.transintoworld(pivot);
        }
        float[] newxvec = new float[]{plane.xvec[0]*xaxis[0]+plane.yvec[0]*xaxis[1],plane.xvec[1]*xaxis[0]+plane.yvec[1]*xaxis[1],plane.xvec[2]*xaxis[0]+plane.yvec[2]*xaxis[1]};
        float[] newyvec = new float[]{plane.xvec[0]*yaxis[0]+plane.yvec[0]*yaxis[1],plane.xvec[1]*yaxis[0]+plane.yvec[1]*yaxis[1],plane.xvec[2]*yaxis[0]+plane.yvec[2]*yaxis[1]};
        modelmat = new float[][]{
                {xrad*newxvec[0],yrad*newyvec[0],plane.normal[0],worldpivot[0]},
                {xrad*newxvec[1],yrad*newyvec[1],plane.normal[1],worldpivot[1]},
                {xrad*newxvec[2],yrad*newyvec[2],plane.normal[2],worldpivot[2]},
                {0,0,0,1}
        };
        modelmat0 = modelmat[0];
        modelmat1 = modelmat[1];
        modelmat2 = modelmat[2];
        modelmat3 = modelmat[3];
        Log.i("pivot", Float.toString(worldpivot[0])+Float.toString(worldpivot[1])+Float.toString(worldpivot[2]));
    }

    //TODO 이거갈격야함

    public void setCircleRotatation(Plane plane){
        modelmat = new float[][]{
                {xrad*plane.xvec[0],yrad*plane.yvec[0],plane.normal[0],worldpivot[0]},
                {xrad*plane.xvec[1],yrad*plane.yvec[1],plane.normal[1],worldpivot[1]},
                {xrad*plane.xvec[2],yrad*plane.yvec[2],plane.normal[2],worldpivot[2]},
                {0,0,0,1}
        };
        Log.i("circlepivot", Float.toString(worldpivot[0])+Float.toString(worldpivot[1])+Float.toString(worldpivot[2]));

    }

    //Clipt to world

    //World Pivot to Clip Pivot
    public void pivot_to_local(float[] projMX, float[] viewMX) {

        float[] MVPmx = new float[16];
        Matrix.multiplyMM(MVPmx,0,projMX,0,viewMX,0);
        float[] thisworldpivot = new float[]{worldpivot[0],worldpivot[1],worldpivot[2],1.0f};
        resultpivot = new float[4];
        Matrix.multiplyMV(resultpivot,0,MVPmx,0,thisworldpivot,0);
        for(int i =0;i<3;i++){
            resultpivot[i]/= resultpivot[3];
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
