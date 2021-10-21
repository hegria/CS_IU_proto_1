//
// Created by Paul on 2021-07-12.
//

#include "native-lib.h"
#include <vector>
#include <algorithm>
#include <opencv2/opencv.hpp>
#include "TimberDetector.h"

using byte = uint8_t;

//
// JNI stuff
//

static jint JNI_VERSION = JNI_VERSION_1_6;

static jclass JC_Contour;
static jmethodID JMID_Contour_Ctor;

static jclass JC_ArrayList;
static jmethodID JMID_ArrayList_Ctor;
static jmethodID JMID_ArrayList_Add;


inline void cacheClass(JNIEnv* const env, const char* class_name, jclass& dst) {
    jclass tmp_class_ref = env->FindClass(class_name);
    dst = (jclass) env->NewGlobalRef(tmp_class_ref);
    env->DeleteLocalRef(tmp_class_ref);
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION) != JNI_OK) {
        return JNI_ERR;
    }

    cacheClass(env, "com/example/CS_IU_proto_1/Contour", JC_Contour);
    JMID_Contour_Ctor = env->GetMethodID(JC_Contour, "<init>", "([F)V");

    cacheClass(env, "java/util/ArrayList", JC_ArrayList);
    JMID_ArrayList_Ctor = env->GetMethodID(JC_ArrayList, "<init>", "()V");
    JMID_ArrayList_Add  = env->GetMethodID(JC_ArrayList, "add", "(Ljava/lang/Object;)Z");

    return JNI_VERSION;
}

void JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION);

    env->DeleteGlobalRef(JC_Contour);
    env->DeleteGlobalRef(JC_ArrayList);
}

//
// JNI stuff
//

cv::Mat getMatrixFromYUV420N12(const byte* buffer, int width, int height);
void resizeImage(cv::Mat& src, int a);

// new method

static cv::HOGDescriptor hog;

void JNICALL J_OPENCV_FUNC(loadTrainedModel, jstring model) {
    static bool shouldInit = true;
    if (shouldInit) {
        const char* native_str = env->GetStringUTFChars(model, nullptr);
        hog.load(native_str);
        shouldInit = false;
        env->ReleaseStringUTFChars(model, native_str);
    }
}

jobject J_OPENCV_FUNC(J_FIND_TIMBER_CONTOUR2, jobject data_yuv420_n12, jint width, jint height, jdouble th) {
    using namespace std;
    using namespace cv;

    Mat img_bgr = getMatrixFromYUV420N12(
            reinterpret_cast<byte*>( env->GetDirectBufferAddress(data_yuv420_n12) ),
            width, height
    );

    int h = img_bgr.rows;
    int w = img_bgr.cols;
    float x_factor = 2.0f / static_cast<float>(w);
    float y_factor = 2.0f / static_cast<float>(h);

    vector<Rect> detections;
    vector<double> weights;
    hog.detectMultiScale(img_bgr, detections, weights, th);

    jobject contour_list = env->NewObject(JC_ArrayList, JMID_ArrayList_Ctor);

    for (const auto& rect : detections) {
        float radius = rect.width * sqrt(0.35f) / 2.0f;
        float x = rect.x + rect.width / 2.0f;
        float y = rect.y + rect.width / 2.0f;
        float arr[] = {
                (x - radius) * x_factor - 1.0f, (y - radius) * y_factor - 1.0f,
                (x + radius) * x_factor - 1.0f, (y - radius) * y_factor - 1.0f,
                (x + radius) * x_factor - 1.0f, (y + radius) * y_factor - 1.0f,
                (x - radius) * x_factor - 1.0f, (y + radius) * y_factor - 1.0f
        };
        auto* ptr = reinterpret_cast<jfloat*>(arr);
        jfloatArray jxy = env->NewFloatArray(8);
        env->SetFloatArrayRegion(jxy, 0, 8, ptr);
        jobject jcontour = env->NewObject(JC_Contour, JMID_Contour_Ctor, jxy);
        env->CallBooleanMethod(contour_list, JMID_ArrayList_Add, jcontour);
    }

    return contour_list;
}

// old method

static DetectorParam global_param;

void J_OPENCV_FUNC(setTH, jint lvl) {
    global_param.norm_lvl = lvl;
}
void J_OPENCV_FUNC(setMORPHO, jint lvl) {
    global_param.morph_open = lvl;
}
void J_OPENCV_FUNC(setMORPHC, jint lvl) {
    global_param.morph_close = lvl;
}
void J_OPENCV_FUNC(enableBG, jboolean b) {
    global_param.bg_enable_filtering = b;
}
void J_OPENCV_FUNC(setMARKTH, jint th) {
    global_param.marker_th = th;
}
void J_OPENCV_FUNC(setMARKP1, jint p1) {
    global_param.marker_p1 = p1;
}
void J_OPENCV_FUNC(setCNTRTH, jdouble th) {
    global_param.cnt_filter_th = th;
}

jobject J_OPENCV_FUNC(J_FIND_TIMBER_CONTOURS, jobject data_juv420_n12, jint width, jint height) {

    const int resizelvl = 900;

    cv::Mat img_bgr = getMatrixFromYUV420N12(
            reinterpret_cast<byte*>( env->GetDirectBufferAddress(data_juv420_n12) ),
            width, height
    );

    resizeImage(img_bgr, resizelvl);
    int h = img_bgr.rows;
    int w = img_bgr.cols;
    float x_factor = 2.0f / static_cast<float>(w);
    float y_factor = 2.0f / static_cast<float>(h);

    TimberDetector detector(global_param);
    auto contours = detector.grabContours(img_bgr);

    jobject contour_list = env->NewObject(JC_ArrayList, JMID_ArrayList_Ctor);

    for (const auto& contour : contours) {
        std::vector<float> arr;
        arr.reserve(contour.size() * 2);
        for (const auto& point : contour) {
            arr.push_back( point.x * x_factor - 1.0f );
            arr.push_back( point.y * y_factor - 1.0f );
        }
        auto* ptr = reinterpret_cast<jfloat*>(&arr[0]);
        jfloatArray jxy = env->NewFloatArray(arr.size());
        env->SetFloatArrayRegion(jxy, 0, arr.size(), ptr);
        jobject jcontour = env->NewObject(JC_Contour, JMID_Contour_Ctor, jxy);
        env->CallBooleanMethod(contour_list, JMID_ArrayList_Add, jcontour);
    }

    return contour_list;
}


cv::Mat getMatrixFromYUV420N12(const byte* buffer, int width, int height) {
    const size_t buffer_size = width * height * 3 / 2 * sizeof(byte);
    constexpr int FLIP_ALL = -1;

    cv::Mat img_in = cv::Mat(height * 3 / 2, width, CV_8UC1);
    std::copy(buffer, buffer + buffer_size, img_in.data);
    cv::Mat img_out = cv::Mat(height, width, CV_8UC3);

    cv::cvtColor(img_in, img_out, cv::COLOR_YUV2BGR_NV12);

    // for some reason, arcore input image is flipped, and transposed, so correct that (?)
    cv::transpose(img_out, img_out);
    cv::flip(img_out, img_out, FLIP_ALL);
    // for some reason, arcore input image is flipped, and transposed, so correct that (?)

    return img_out;
}

void resizeImage(cv::Mat& src, int a) {
    double h = src.rows, w = src.cols;
    double ratio = w / h;

    if (ratio < 1.0) {
        w = a * ratio;
        h = a;
    }
    else {
        w = a;
        h = a / ratio;
    }

    int W = static_cast<int>(w);
    int H = static_cast<int>(h);

    cv::resize(src, src, cv::Size(W, H), 0.0, 0.0, cv::INTER_AREA);
}
