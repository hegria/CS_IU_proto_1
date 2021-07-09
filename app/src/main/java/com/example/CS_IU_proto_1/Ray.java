package com.example.CS_IU_proto_1;

public class Ray {
    float[] origin;
    float[] dir;

    public Ray(float[] rayinfo) {
        origin = new float[]{rayinfo[0], rayinfo[1], rayinfo[2]};
        dir = new float[]{rayinfo[3], rayinfo[4], rayinfo[5]};
    }
    public Ray(float[] rayorigin, float[] raydir) {
        origin = new float[]{rayorigin[0], rayorigin[1], rayorigin[2]};
        dir = new float[]{raydir[0], raydir[1], raydir[2]};
    }
}
