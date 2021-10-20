//
// Created by Paul on 2021-07-12.
//

#ifndef CS_IU_PROTO_1_NATIVE_LIB_H
#define CS_IU_PROTO_1_NATIVE_LIB_H

#include <jni.h>

#define J_DEF2(PKG, CLASS, FUNC) PKG ## _ ## CLASS ## _ ## FUNC
#define J_DEF1(PKG, CLASS, FUNC) J_DEF2(PKG, CLASS, FUNC)

#define J_PKG_CS_IU_proto_1 Java_com_example_CS_1IU_1proto_11
#define J_CLASS_OpenCVJNI OpenCVJNI

#define J_OPENCV_FUNC(FUNC, ...) J_DEF1(J_PKG_CS_IU_proto_1, J_CLASS_OpenCVJNI, FUNC) \
    (JNIEnv* env, jobject thiz, ##__VA_ARGS__)

#define J_FIND_TIMBER_CONTOURS _1findTimberContours
#define J_FIND_TIMBER_CONTOUR2 _1findTimberContours2

extern "C" {

    JNIEXPORT jobject JNICALL
    J_OPENCV_FUNC(J_FIND_TIMBER_CONTOURS, jobject data_yuv_n12, jint width, jint height);
    JNIEXPORT jobject JNICALL
    J_OPENCV_FUNC(J_FIND_TIMBER_CONTOUR2, jobject data_yuv_n12, jint width, jint height, jdouble th);

    JNIEXPORT void JNICALL J_OPENCV_FUNC(loadTrainedModel, jstring model);

    JNIEXPORT void JNICALL J_OPENCV_FUNC(setTH, jint lvl);
    JNIEXPORT void JNICALL J_OPENCV_FUNC(setMORPHO, jint lvl);
    JNIEXPORT void JNICALL J_OPENCV_FUNC(setMORPHC, jint lvl);
    JNIEXPORT void JNICALL J_OPENCV_FUNC(enableBG, jboolean b);
    JNIEXPORT void JNICALL J_OPENCV_FUNC(setMARKTH, jint th);
    JNIEXPORT void JNICALL J_OPENCV_FUNC(setMARKP1, jint p1);
    JNIEXPORT void JNICALL J_OPENCV_FUNC(setCNTRTH, jdouble th);

}


#endif //CS_IU_PROTO_1_NATIVE_LIB_H