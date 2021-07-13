package com.example.CS_IU_proto_1;

//Local
public class EllipseSize {
    float longrad;
    float shortrad;
    float[] cp;
    float[] longvertex;
    float[] shortvertex;
    // 그릴 때 Contour를 쓸지 안쓸지
    // 쓴다면
    // 1. LocalContour List의 Index를 사용할지
    // 2. LocalContour의 정보를 Rectangle에 집어 넣을건지.
    // 3. Center와 contour points들을 한번에 넣어서 삼각형을 겹쳐서 draw
    // 안쓴다면
    // 1. 일단 중심으로 xy에 걸맞는 points 36개 만듬
    // 2. longvertex과 x축 각도를 계산해서 points를 각에 맞춰서 회전
    // 3. draw
    float size;


    public EllipseSize(float _r1, float _r2, float[] _cp, float[] _p1, float[] _p2) {
        longrad = _r1;
        shortrad = _r2;
        cp = _cp;
        longvertex = _p1;
        shortvertex = _p2;

        size = (float) (longrad * shortrad * Math.PI);
    }
}
