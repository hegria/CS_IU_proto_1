package com.example.CS_IU_proto_1;

import java.util.List;

public class FindContour {
    // TODO 아직 생각중
    // 1. RawContour의 points들을 받음
    // 2. points Local points로 변환함 -> clips to local
    //    clip points들을 Lays 바꿈 (Lay Origin은 동일함, project Matrix와 View Matrix 필요.
    //   -> snapshot이 필요함.
    //    Lays를 만들고 -> 평방에 투영 -> Local로 변환
    // 3. Local Points를 쌓음
    // 4. 쌓은 Point로 Contour를 다시 만듬
    // final. 그 Contour를 Draw
    float[] viewMX;
    float[] projMX;
    float[] cameratrans;

    List<Contour> rawContour;

    boolean isinital;


    // Contour들을 쌓는...
    public void push(float[] viewMX, float[] projMX[], float[] cameratrans, List<Contour> nowcontour){
        // 이미 contour 별로 serialize가 되었는데.. 이걸 다시 deserialize할 것 같다는 생각..
    }

}
