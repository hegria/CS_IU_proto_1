package com.example.CS_IU_proto_1;

//Local
public class EllipseSize {
    float r1;
    float r2;
    float[] cp;
    float[] p1;
    float[] p2;

    float size;


    public EllipseSize(float _r1, float _r2, float[] _cp, float[] _p1, float[] _p2) {
        r1 = _r1;
        r2 = _r2;
        cp = _cp;
        p1 = _p1;
        p2 = _p2;

        size = (float) (r1 * r2 * Math.PI);
    }
}
