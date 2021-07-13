package com.example.CS_IU_proto_1;

public class RectEllipseSize {

    // 그릴 때는 Contour를 쓸탠데
    // 1. LocalContour List의 Index를 사용할지
    // 2. LocalContour의 정보를 Rectangle에 집어 넣을건지.

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
