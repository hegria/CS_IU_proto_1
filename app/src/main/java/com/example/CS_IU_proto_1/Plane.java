package com.example.CS_IU_proto_1;


import android.util.Log;

import com.curvsurf.fsweb.ResponseForm;

public class Plane {
  public float[] ll, lr, ul, ur;
  public float[] normal;

  public float[] center;

  public float dval;

  public float[] planeVertex;
  private float[][] transformworldtolocal;
  private float[][] transformlocaltoworld;
  private float[] xvec;
  private float[] yvec;
  private float[] neworigin;

  public Plane(ResponseForm.PlaneParam param, float[] z_dir){

    ll = param.ll;
    lr = param.lr;
    ul = param.ul;
    ur = param.ur;
    center = param.c;
    normal = param.n;

    this.checkNormal(z_dir);

    planeVertex = new float[]{
            ll[0], ll[1], ll[2],
            lr[0], lr[1], lr[2],
            ur[0], ur[1], ur[2],
            ll[0], ll[1], ll[2],
            ur[0], ur[1], ur[2],
            ul[0], ul[1], ul[2],
    };

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
  //
  // 초기화 함수들
  //

  protected  void setDval(){
    dval = normal[0]*ll[0] + normal[1]*ll[1] + normal[2]*ll[2];
  }

  //법선벡터 방향 정하기

  private void checkNormal(float[] z_dir) {
    if (z_dir[0] * normal[0] + z_dir[1] * normal[1] + z_dir[2] * normal[2] >= 0) return;
    normal[0] = -normal[0];
    normal[1] = -normal[1];
    normal[2] = -normal[2];
  }

  //
  // 백터연산
  //
  private float distance(float[] a){
    return (float) Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);
  }
  void normalize(float[] a){
    float len = distance(a);
    for (int i =0;i<3;i++){
      a[i]/=len;
    }
  }

  private float[] crossprod(float[] a, float[] b){
    float[] temp = new float[3];

    temp[0] = a[1]*b[2]-a[2]*b[1];
    temp[1] = a[2]*b[0]-a[0]*b[2];
    temp[2] = a[0]*b[1]-a[1]*b[0];
    return temp;
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

  // 평면위의 점의 월드좌표에서 평면의 로컬 좌표로 바꾸기 -> 평면이라면 z좌표는 0이라서 그냥 제거
  // TODO 그냥 2차원 점으로 점근해도 될듯????
  public float[] transintolocal(float[] point){

    float[] newpoint = new float[3];
    for(int i =0;i<3;i++){
        for(int j=0;j<3;j++){
            newpoint[i]+=transformworldtolocal[i][j]*point[j];
          }
      }

    Log.i("Point", Float.toString(newpoint[2])+Float.toString(neworigin[2]));
    float[] resultpoint = substractvec(newpoint,neworigin);
    resultpoint[2] = 0;
    return resultpoint;
  }

  // 평면 로컬 좌표에서 월드 좌표로 바꾸기

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
