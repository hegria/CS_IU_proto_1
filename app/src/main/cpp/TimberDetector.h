//
// Created by RFX on 2021-07-22.
//

#ifndef CS_IU_PROTO_1_TIMBERDETECTOR_H
#define CS_IU_PROTO_1_TIMBERDETECTOR_H

#include <opencv2/opencv.hpp>
#include <vector>

using Contour = std::vector<cv::Point>;

class TimberDetector
{
public:
    TimberDetector();
    ~TimberDetector();

    std::vector<Contour> grabContours(const cv::Mat& img_bgr) const;


    void setCandidateThresh(double x); // x -> 0.0 ~ 1.0
    void setMorphologyParam(int x, int y); // does nothing at the moment
    void setBackgroundRange(int beg, int end); // hue value
    void setSegmentationSensitivity(double x); // x -> 0.0 ~ 1.0

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
    struct {
        int norm_lvl = 5;				// 0 ~ 255 (lower value -> more aggressive threshold)

        int morph_close = 1;			// not bounded (crack removal)
        int morph_open = 2;				// not bounded (separate timbers)

        // {param-bg} color in this range will be considered a background
        int bg_beg = 25;				// 0 ~ 255 (Hue)
        int bg_end = 210;				// 0 ~ 255 (Hue)

        // {param-marker} parameters for setting markers
        int marker_th = 25;				// 0 ~ 255		(lower: oversegmentation / higher: undersegmentation)
        int marker_p1 = 20;				// not bounded	(same as above)

        // {param-cnt} parameters for filtering contours
        double cnt_filter_th1 = 1.0;	// not bounded
        double cnt_filter_th2 = 3.0;	// not bounded
    } param;

    void filterTimberCandidate(cv::Mat& dst_bin, const cv::Mat& src_hsv) const;
    int segmentAreas(cv::Mat& dst_32SC1, const cv::Mat& src_hsv) const;
    void filterContours(std::vector<Contour>& dst, const std::vector<Contour>& src_bin, double low, double high) const;

};

#endif //CS_IU_PROTO_1_TIMBERDETECTOR_H
