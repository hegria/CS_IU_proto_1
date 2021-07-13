//
// Created by Paul on 2021-07-12.
//

#include "native-lib.h"
#include <vector>
#include <algorithm>
#include <opencv2/opencv.hpp>

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

// inline function version
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
int calcMedian(const cv::Mat& src);
double calcStdDev(const cv::Mat& src);
void localMax(const cv::Mat& src, std::vector<cv::Point>& dst, uint8_t threshold, int min_distance);

void removeShadow(cv::Mat& img_hsv);
int segmentAreas(const cv::Mat& img_bgr, cv::Mat& dst);
void findTimberContours(const cv::Mat& img_bgr, std::vector<std::vector<cv::Point>>& contours);
cv::Mat getBackground(const cv::Mat& src);



jobject J_FIND_TIMBER_CONTOURS(jobject data_juv420_n12, jint width, jint height) {
    cv::Mat img_bgr = getMatrixFromYUV420N12(
            reinterpret_cast<byte*>(env->GetDirectBufferAddress(data_juv420_n12)),
            width, height
    );

    resizeImage(img_bgr, 600);
    int h = img_bgr.rows;
    int w = img_bgr.cols;
    float x_factor = 2.0f / static_cast<float>(w);
    float y_factor = 2.0f / static_cast<float>(h);

    std::vector<std::vector<cv::Point>> contours;
    findTimberContours(img_bgr, contours);

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
    const size_t buffer_size = width * height * 3/2 * sizeof(byte);

    cv::Mat img_in = cv::Mat(height * 3 / 2, width, CV_8UC1);
    std::copy(buffer, buffer + buffer_size, img_in.data);

    cv::Mat img_out = cv::Mat(height, width, CV_8UC3);

    cv::cvtColor(img_in, img_out, cv::COLOR_YUV2BGR_NV12);
    cv::transpose(img_out, img_out);
    cv::flip(img_out, img_out, -1);
    return img_out;
}

/**
 * Resizes image so that the longest side of image becomes "a" pixels wide
 * @param src : source image (gets modified)
 * @param a : resize factor
 */
void resizeImage(cv::Mat& src, int a) {
    double h = src.rows, w = src.cols;
    double ratio = w / h;

    if (ratio < 1.0) {
        w = a;
        h = a * ratio;
    }
    else {
        w = a * ratio;
        h = a;
    }

    int W = static_cast<int>(w);
    int H = static_cast<int>(h);

    cv::resize(src, src, cv::Size(W, H), 0.0, 0.0, cv::INTER_AREA);
}

int calcMedian(const cv::Mat& src) {
    const int DEPTH = 256;

    // COMPUTE HISTOGRAM OF SINGLE CHANNEL MATRIX
    cv::Mat hist;
    std::vector<cv::Mat> inputs = { src };
    cv::calcHist(inputs, { 0 }, cv::noArray(), hist, { DEPTH }, { 0, DEPTH }, false);

    // COMPUTE CUMULATIVE DISTRIBUTION FUNCTION (CDF)
    cv::Mat cdf;
    hist.copyTo(cdf);
    for (int i = 1; i < DEPTH; i++) {
        cdf.at<float>(i) += cdf.at<float>(i - 1);
    }
    cdf /= src.total();

    // COMPUTE MEDIAN
    int m = 0;
    while (m < DEPTH && cdf.at<float>(m) < 0.5f) m++;
    return m;
}

double calcStdDev(const cv::Mat& src) {
    cv::Mat stddev;
    cv::meanStdDev(src, cv::noArray(), stddev);
    return stddev.at<double>(0, 0);
}

void localMax(const cv::Mat& src, std::vector<cv::Point>& dst, uint8_t threshold, int min_distance)
{
    uint8_t ksize = 2 * min_distance + 1;
    cv::Mat kernel = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(ksize, ksize));

    // applying maximum filter
    cv::Mat dilated;
    cv::morphologyEx(src, dilated, cv::MORPH_DILATE, kernel);

    // extracting peak points
    for (int r = 0; r < src.rows; r++) {
        for (int c = 0; c < src.cols; c++) {
            uint8_t x = src.at<uint8_t>(r, c);
            uint8_t y = dilated.at<uint8_t>(r, c);

            if (x == y && x >= threshold) {
                dst.emplace_back(c, r);
            }
        }
    }
}

void removeShadow(cv::Mat& img_hsv) {

    const cv::Mat kernel3 = cv::Mat::ones(cv::Size(3, 3), CV_8UC1);
    const cv::Point nopoint = cv::Point(-1, -1);

    // get HSV channel
    cv::Mat hsv[3];
    cv::split(img_hsv, hsv);

    // remove shadows with V channel
    cv::normalize(hsv[2], hsv[2], 0.0, 5.0, cv::NORM_MINMAX, CV_8UC1);
    cv::threshold(hsv[2], hsv[2], 1.0, 255.0, cv::THRESH_BINARY);
    cv::morphologyEx(hsv[2], hsv[2], cv::MORPH_CLOSE, kernel3, nopoint, 1);
    cv::morphologyEx(hsv[2], hsv[2], cv::MORPH_OPEN, kernel3, nopoint, 2);

    // remove sky areas with H channel
    cv::Mat background = getBackground(hsv[0]);
    cv::bitwise_not(background, background);
    cv::bitwise_and(background, hsv[2], hsv[2]);

    cv::merge(hsv, 3, img_hsv);
}

int segmentAreas(const cv::Mat& img_bgr, cv::Mat& dst) {

    cv::Mat img_hsv;
    cv::cvtColor(img_bgr, img_hsv, cv::COLOR_BGR2HSV);
    removeShadow(img_hsv);

    cv::Mat hsv[3];
    cv::split(img_hsv, hsv);

    // get the "center" points
    std::vector<cv::Point> centers;
    cv::Mat dt;
    cv::distanceTransform(hsv[2], dt, cv::DIST_L2, 3);

    double min, max;
    cv::minMaxIdx(dt, &min, &max, nullptr, nullptr);
    double scale_factor = (max - min) / 256.0;

    cv::normalize(dt, dt, 0.0, 255.0, cv::NORM_MINMAX, CV_8UC1);
    localMax(dt, centers, 25, 20);

    // set the markers from center points
    cv::Mat markers = cv::Mat::zeros(cv::Size(img_bgr.cols, img_bgr.rows), CV_8UC3);
    for (const auto& c : centers) {
        double radius = dt.at<uint8_t>(c) * scale_factor + min;
        radius *= 0.7;
        cv::circle(markers, c, radius, cv::Scalar(255), -1);
    }
    cv::cvtColor(markers, markers, cv::COLOR_BGR2GRAY);

    int num = cv::connectedComponents(markers, markers, 8, CV_32S);

    markers += 1;

    for (int r = 0; r < img_bgr.rows; r++) {
        for (int c = 0; c < img_bgr.cols; c++) {
            auto& m = markers.at<int32_t>(r, c);
            if (m == 1 && hsv[2].at<uint8_t>(r, c) != 0) {
                m = 0;
            }
        }
    }

    // get segmented labels from watershed
    cv::GaussianBlur(img_hsv, img_hsv, cv::Size(3, 3), 3);
    cv::watershed(img_hsv, markers);
    dst = markers;
    return num;
}

void findTimberContours(const cv::Mat& img_bgr, std::vector<std::vector<cv::Point>>& contours) {

    cv::Mat segmented;
    int num = segmentAreas(img_bgr, segmented);

    cv::Mat seg;
    cv::inRange(segmented, cv::Scalar(2), cv::Scalar(num), seg);
    cv::erode(seg, seg, cv::Mat::ones(3, 3, CV_8UC1));
    cv::findContours(seg, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);
}

cv::Mat getBackground(const cv::Mat& src) {
    double median = calcMedian(src);
    double stddev = calcStdDev(src);

    cv::Mat kernel7 = cv::Mat::ones(cv::Size(7, 7), CV_8UC1);

    cv::Mat bg;
    cv::threshold(src, bg, std::min(255.0, median + stddev), 255, cv::THRESH_BINARY);
    cv::morphologyEx(bg, bg, cv::MORPH_OPEN, kernel7, cv::Point(-1, -1), 2);

    return bg;
}
