package com.example.CS_IU_proto_1;

public class RectEllipseSize {


    float[] ll;
    float[] lr;
    float[] ul;
    float[] ur;
    float size;

    public RectEllipseSize(float[] _ll, float[] _lr, float[] _ul, float[] _ur) {
        ll = _ll;
        lr = _lr;
        ul = _ul;
        ur = _ur;
        size = (float) (Math.sqrt((ll[0]-lr[0])*(ll[0]-lr[0])+(ll[1]-lr[1])*(ll[1]-lr[1]))/2 *
                        Math.sqrt((ll[0]-ul[0])*(ll[0]-ul[0])+(ll[1]-ul[1])*(ll[1]-ul[1]))/2 * Math.PI);
    }
}
