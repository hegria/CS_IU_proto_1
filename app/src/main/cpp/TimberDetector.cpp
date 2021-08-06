//
// Created by RFX on 2021-07-22.
//

#include "TimberDetector.h"
#include "utils.h"

#define PI 3.14159

TimberDetector::TimberDetector()
{

}

TimberDetector::~TimberDetector()
{

}

std::vector<Contour> TimberDetector::grabContours(const cv::Mat& img_bgr) const
{
    cv::Mat img_hsv; cv::cvtColor(img_bgr, img_hsv, cv::COLOR_BGR2HSV);

    std::vector<Contour> contours;
    std::vector<Contour> result;

    cv::Mat segmented;
    int num = segmentAreas(segmented, img_hsv);
    cv::inRange(segmented, cv::Scalar(2), cv::Scalar(num), segmented);
    cv::erode(segmented, segmented, cv::Mat::ones(3, 3, CV_8UC1));

    cv::findContours(segmented, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);
    filterContours(result, contours, param.cnt_filter_th1, param.cnt_filter_th2);

    return result;
}

void TimberDetector::filterTimberCandidate(cv::Mat& dst_bin, const cv::Mat& src_hsv) const
{
    const cv::Mat kernel3 = cv::Mat::ones(3, 3, CV_8UC1);
    const cv::Mat kernel7 = cv::Mat::ones(7, 7, CV_8UC1);
    const cv::Point nopoint(-1, -1);

    // get HSV channel
    cv::Mat hsv[3];
    cv::split(src_hsv, hsv);

    // remove shadows with V channel
    cv::normalize(hsv[2], hsv[2], 0.0, param.norm_lvl, cv::NORM_MINMAX, CV_8UC1);
    cv::threshold(hsv[2], hsv[2], 1.0, 255.0, cv::THRESH_BINARY);
#ifdef DEBUG_TIMBER_DETECTOR
    img_proc.candidate_V = hsv[2].clone();
#endif
    cv::morphologyEx(hsv[2], hsv[2], cv::MORPH_CLOSE, kernel3, nopoint, param.morph_close);
    cv::morphologyEx(hsv[2], hsv[2], cv::MORPH_OPEN,  kernel3, nopoint, param.morph_open);

    // remove sky areas with H channel
    cv::Mat background;
    if (param.bg_enable_filtering) {
        if (param.bg_beg <= param.bg_end) {
            cv::inRange(hsv[0], cv::Scalar(param.bg_beg), cv::Scalar(param.bg_end), background);
            background = ~background;
        }
        else {
            cv::inRange(hsv[0], cv::Scalar(param.bg_end), cv::Scalar(param.bg_beg), background);
        }
    }
    else {
        background = cv::Mat::ones(src_hsv.rows, src_hsv.cols, CV_8UC1);
    }
#ifdef DEBUG_TIMBER_DETECTOR
    img_proc.candidate_H = background.clone();
#endif
    cv::morphologyEx(background, background, cv::MORPH_OPEN, kernel7, nopoint, 4);
    hsv[2] &= background;

    dst_bin = hsv[2];
}

int TimberDetector::segmentAreas(cv::Mat& dst_32SC1, const cv::Mat& src_hsv) const
{
    const int HEIGHT = src_hsv.rows;
    const int WIDTH  = src_hsv.cols;

    cv::Mat timber_candidate;
    filterTimberCandidate(timber_candidate, src_hsv);
#ifdef DEBUG_TIMBER_DETECTOR
    img_proc.candidate_merged = timber_candidate.clone();
#endif

    cv::Mat dt;
    cv::distanceTransform(timber_candidate, dt, cv::DIST_L2, 3);
    double min, max;
    cv::minMaxIdx(dt, &min, &max, nullptr, nullptr);
    double scale_factor = (max - min) / 256.0;
    cv::normalize(dt, dt, 0.0, 255.0, cv::NORM_MINMAX, CV_8UC1);
#ifdef DEBUG_TIMBER_DETECTOR
    img_proc.distance_transform = dt.clone();
#endif

    // get the "center" points
    std::vector<cv::Point> centers;
    utils::localMax(centers, dt, param.marker_th, param.marker_p1);

    // set the markers from center points
    cv::Mat markers = cv::Mat::zeros(HEIGHT, WIDTH, CV_8UC3);
    for (const auto& c : centers) {
        double radius = dt.at<uint8_t>(c) * scale_factor + min;
        radius *= 0.7;
        cv::circle(markers, c, radius, cv::Scalar(255), -1);
    }
    cv::cvtColor(markers, markers, cv::COLOR_BGR2GRAY);

    int num = cv::connectedComponents(markers, markers, 8, CV_32S);

    markers += 1;

    for (int r = 0; r < HEIGHT; r++) {
        for (int c = 0; c < WIDTH; c++) {
            auto& m = markers.at<int32_t>(r, c);
            if (m == 1 && timber_candidate.at<uint8_t>(r, c) != 0) {
                m = 0;
            }
        }
    }
#ifdef DEBUG_TIMBER_DETECTOR
    markers.convertTo(img_proc.markers, CV_8UC1);
#endif

    // get segmented labels from watershed
    cv::Mat hsv[3]; cv::Mat processed;
    cv::split(src_hsv, hsv);
    hsv[2] = timber_candidate;
    cv::merge(hsv, 3, processed);

    cv::GaussianBlur(processed, processed, cv::Size(3, 3), 3);
    cv::watershed(processed, markers);

    dst_32SC1 = markers;

    return num;
}

void TimberDetector::filterContours(std::vector<Contour>& dst, const std::vector<Contour>& src, double low, double high) const
{
    utils::filter(dst, src, [low, high](const Contour& contour) {
        double arclen = cv::arcLength(contour, true);
        double area = cv::contourArea(contour, false);

        if (arclen == 0 || area == 0) return false;

        double value = arclen * arclen / (area * 4 * PI);
        return low < value && value < high;
    });
}

void TimberDetector::setCandidateThresh(int x) {
    param.norm_lvl = x;
}

void TimberDetector::setMorphologyParam(int x, int y) {
    // nothing here yet
    param.morph_close = x;
    param.morph_open = y;
}

void TimberDetector::enableBackgroundFiltering(bool x) {
    param.bg_enable_filtering = x;
}

void TimberDetector::setBackgroundRange(int beg, int end) {
    param.bg_beg = (beg % 256 + 256) % 256;
    param.bg_end = (end % 256 + 256) % 256;
}

void TimberDetector::setSegmentationSensitivity(double x) {
    param.marker_th = utils::ensureBounds(static_cast<int>(255 * (1.0 - x)), 1, 255);
}

void TimberDetector::setFilterThresh(double x)
{
    param.cnt_filter_th2 = x;
}

