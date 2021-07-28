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

#define J_FIND_TIMBER_CONTOURS(...) \
    J_DEF1(J_PKG_CS_IU_proto_1, J_CLASS_OpenCVJNI, _1findTimberContours) \
    (JNIEnv* env, jobject thiz, ##__VA_ARGS__)

extern "C" {

    JNIEXPORT jobject JNICALL
    J_FIND_TIMBER_CONTOURS(jobject data_yuv_n12, jint width, jint height, jint resizelvl, jdouble normlvl, jint closelvl, jint openlvl, jdouble markerlvl, jboolean bg_enable_filtering);

}


#endif //CS_IU_PROTO_1_NATIVE_LIB_H
