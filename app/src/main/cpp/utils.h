//
// Created by RFX on 2021-07-22.
//

#ifndef CS_IU_PROTO_1_UTILS_H
#define CS_IU_PROTO_1_UTILS_H

#pragma once

#include <vector>
#include <algorithm>
#include <iterator>

#include <opencv2/opencv.hpp>

namespace utils {
    template <typename Ty, class FuncType>
    std::vector<Ty> filter(const std::vector<Ty>& src, FuncType callback) {
        std::vector<Ty> res;
        std::copy_if(src.begin(), src.end(), std::back_inserter(res), callback);
        return res;
    }

    template <typename Ty, class FuncType>
    void filter(std::vector<Ty>& dst, const std::vector<Ty>& src, FuncType callback) {
        std::copy_if(src.begin(), src.end(), std::back_inserter(dst), callback);
    }

    int calcMedian(const cv::Mat& src);

    double calcStdDev(const cv::Mat& src);

    void localMax(std::vector<cv::Point>& dst, const cv::Mat& src, uint8_t threshold, int param_1);

}

#endif //CS_IU_PROTO_1_UTILS_H
