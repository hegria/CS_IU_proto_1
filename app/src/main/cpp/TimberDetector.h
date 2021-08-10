//
// Created by RFX on 2021-07-22.
//

#ifndef CS_IU_PROTO_1_TIMBERDETECTOR_H
#define CS_IU_PROTO_1_TIMBERDETECTOR_H

#include <opencv2/opencv.hpp>
#include <vector>

struct DetectorParam {
    int norm_lvl = 9;				// 0 ~ 255 (lower value -> more aggressive threshold)

    int morph_close = 0;			// not bounded (crack removal)
    int morph_open = 5;				// not bounded (separate timbers)

    // {param-bg} color in this range will be considered a background
    bool bg_enable_filtering = false;
    int bg_beg = 20;				// 0 ~ 255 (Hue)
    int bg_end = 210;				// 0 ~ 255 (Hue)

    // {param-marker} parameters for setting markers
    int marker_th = 25;				// 0 ~ 255		(lower: oversegmentation / higher: undersegmentation)
    int marker_p1 = 9;				// not bounded	(same as above)

    // {param-cnt} parameters for filtering contours
    double cnt_filter_th = 3.0;	// not bounded
};

using Contour = std::vector<cv::Point>;

class TimberDetector
{
public:
    TimberDetector(const DetectorParam& param);
    ~TimberDetector();

    std::vector<Contour> grabContours(const cv::Mat& img_bgr) const;

#ifdef DEBUG_TIMBER_DETECTOR
    // for observing / debugging purposes
    // modifying the matrices will have no effect on image processing
    // all matrices are in gray scale (single channel)
    mutable struct {
        cv::Mat candidate_V;
        cv::Mat candidate_H;
        cv::Mat candidate_merged;
        cv::Mat distance_transform;
        cv::Mat markers;
    } img_proc;
#endif

private:
    const DetectorParam& param;

    void filterTimberCandidate(cv::Mat& dst_bin, const cv::Mat& src_hsv) const;
    int segmentAreas(cv::Mat& dst_32SC1, const cv::Mat& src_hsv) const;
    void filterContours(std::vector<Contour>& dst, const std::vector<Contour>& src_bin, int w, int h, double th) const;

};

#endif //CS_IU_PROTO_1_TIMBERDETECTOR_H
