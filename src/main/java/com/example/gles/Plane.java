package com.example.CS_IU_proto_1;

import android.annotation.SuppressLint;

public class Plane {
  public float[] ll, lr, ul, ur;
  public float[] normal;

  public float[] planeVertex;

  public Plane(float[] ll, float[] lr, float[] ur, float[] ul, float[] z_dir) {
    this.ll = ll;
    this.lr = lr;
    this.ul = ul;
    this.ur = ur;

    planeVertex = new float[]{
            ul[0], ul[1], ul[2],
            ll[0], ll[1], ll[2],
            lr[0], lr[1], lr[2],
            ur[0], ur[1], ur[2],
    };

    normal = new float[3];
    this.calNormal();
    this.checkNormal(z_dir);
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
}
