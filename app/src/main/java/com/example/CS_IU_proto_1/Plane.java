package com.example.CS_IU_proto_1;

import android.annotation.SuppressLint;

public class Plane {
  public float[] ll, lr, ul, ur;
  public float[] normal;

  public float[] center;

  public float dval;

  public float[] planeVertex;
  float[][] transformworldtolocal;
  float[][] transformlocaltoworld;
  float[] xvec;
  float[] yvec;
  float[] neworigin;

  public Plane(float[] ll, float[] lr, float[] ur, float[] ul, float[] center, float[] z_dir) {
    this.ll = ll;
    this.lr = lr;
    this.ul = ul;
    this.ur = ur;
    this.center = center;

    planeVertex = new float[]{
            ul[0], ul[1], ul[2],
            ll[0], ll[1], ll[2],
            lr[0], lr[1], lr[2],
            ur[0], ur[1], ur[2],
    };

    normal = new float[3];
    this.calNormal();
    this.checkNormal(z_dir);
    xvec = new float[3];
    yvec = new float[3];
    setDval();

    float[] standard = {0f,1f,0f};
    xvec = crossprod(normal,standard);
    yvec = crossprod(xvec,normal);
    normalize(xvec);
    normalize(yvec);
    transformworldtolocal = new float[][]{
            {xvec[0], xvec[1], xvec[2]},
            {yvec[0], yvec[1], yvec[2]},
            {normal[0], normal[1], normal[2]}
    };

    transformlocaltoworld = new float[][]{
            {xvec[0],yvec[0],normal[0]},
            {xvec[1],yvec[1],normal[1]},
            {xvec[2],yvec[2],normal[2]}
    };
    neworigin = new float[3];

    for(int i =0;i<3;i++){
        for(int j = 0; j<3;j++){
        neworigin[i] += transformworldtolocal[i][j] * center[j];
      }
    }
  }

  protected  void setDval(){
    dval = normal[0]*ll[0] + normal[1]*ll[1] + normal[2]*ll[2];
  }

  protected void calNormal() {
    float[] vec1 = {lr[0] - ll[0], lr[1] - ll[1], lr[2] - ll[2], 1f};
    float[] vec2 = {ul[0] - ll[0], ul[1] - ll[1], ul[2] - ll[2], 1f};

    this.normal = new float[]{
            vec1[1] * vec2[2] - vec1[2] * vec2[1],
            vec1[2] * vec2[0] - vec1[0] * vec2[2],
            vec1[0] * vec2[1] - vec1[1] * vec2[0]
    };

    float length = (float) Math.sqrt(
            this.normal[0] * this.normal[0]
                    + this.normal[1] * this.normal[1]
                    + this.normal[2] * this.normal[2]
    );

    this.normal = new float[]{
            this.normal[0] / length,
            this.normal[1] / length,
            this.normal[2] / length
    };
  }

  public void checkNormal(float[] z_dir) {
    if (z_dir[0] * normal[0] + z_dir[1] * normal[1] + z_dir[2] * normal[2] >= 0) return;
    normal[0] = -normal[0];
    normal[1] = -normal[1];
    normal[2] = -normal[2];
  }

  @Override
  public String toString() {
    @SuppressLint("DefaultLocale")
    String ret = String.format("ll: (%f, %f, %f),\n" +
                    "lr: (%f, %f, %f),\n" +
                    "ul: (%f, %f, %f),\n" +
                    "ur: (%f, %f, %f)\n",
            ll[0], ll[1], ll[2],
            lr[0], lr[1], lr[2],
            ul[0], ul[1], ul[2],
            ur[0], ur[1], ur[2]
    );
    return ret;
  }

  float[] crossprod(float[] a, float[] b){
    float[] temp = new float[3];

    temp[0] = a[1]*b[2]-a[2]*b[1];
    temp[1] = a[2]*b[0]-a[0]*b[2];
    temp[2] = a[0]*b[1]-a[1]*b[0];
    return temp;
  }
  float distance(float[] a){
    return (float) Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);
  }
  void normalize(float[] a){
    float len = distance(a);
    for (int i =0;i<3;i++){
      a[i]/=len;
    }
  }
  float dotproduct(float[] a,float[] b){
    float now = 0;
    for(int i =0;i<3;i++)
    {
      now+= a[i]*b[i];
    }
    return now;
  }

  float[] substractvec(float[] a, float[] b){
    float[] temp = new float[3];
    for(int i=0;i<3;i++){
      temp[i] = a[i]-b[i];
    }
    return temp;
  }
  float[] addvec(float[] a, float[] b){
    float[] temp = new float[3];
    for(int i=0;i<3;i++){
      temp[i] = a[i]+b[i];
    }
    return temp;
  }


  public float[] transintolocal(float[] point){

    float[] newpoint = new float[3];
    for(int i =0;i<3;i++){
        for(int j=0;j<3;j++){
        newpoint[i]+=transformworldtolocal[i][j]*point[j];
          }
      }
    return substractvec(newpoint,neworigin);
  }

  public float[] transintoworld(float[] point){
    float[] newpoint = addvec(point,neworigin);
    float[] newpoint2 = new float[3];

    for(int i =0;i<3;i++){
      for(int j=0;j<3;j++){
        newpoint2[i]+=transformlocaltoworld[i][j]*newpoint[j];
      }
    }
    return newpoint2;
  }

}
