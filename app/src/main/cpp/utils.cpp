//
// Created by RFX on 2021-07-22.
//

#include "utils.h"

int utils::calcMedian(const cv::Mat& src_single_channel)
{
    const int DEPTH = 256;

    // COMPUTE HISTOGRAM OF SINGLE CHANNEL MATRIX
    cv::Mat hist;
    std::vector<cv::Mat> inputs = { src_single_channel };
    cv::calcHist(inputs, { 0 }, cv::noArray(), hist, { DEPTH }, { 0, DEPTH }, false);

    // COMPUTE CUMULATIVE DISTRIBUTION FUNCTION (CDF)
    cv::Mat cdf;
    hist.copyTo(cdf);

    for (int i = 1; i < DEPTH; i++) {
        cdf.at<float>(i) += cdf.at<float>(i - 1);
    }
    cdf /= src_single_channel.total();

    // COMPUTE MEDIAN
    int m = 0;
    while (m < DEPTH && cdf.at<float>(m) < 0.5f) m++;
    return m;
}

double utils::calcStdDev(const cv::Mat& src)
{
    cv::Mat stddev;
    cv::meanStdDev(src, cv::noArray(), stddev);
    return stddev.at<double>(0, 0);
}

void utils::localMax(std::vector<cv::Point>& dst, const cv::Mat& src_bin, uint8_t threshold, int param_1)
{
    uint8_t ksize = 2 * param_1 + 1;
    cv::Mat kernel = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(ksize, ksize));

    // applying maximum filter
    cv::Mat dilated;
    cv::dilate(src_bin, dilated, kernel);

    // extracting peak points
    for (int r = 0; r < src_bin.rows; r++) {
        for (int c = 0; c < src_bin.cols; c++) {
            uint8_t x = src_bin.at<uint8_t>(r, c);
            uint8_t y = dilated.at<uint8_t>(r, c);

            if (x == y && x >= threshold) {
                dst.emplace_back(c, r);
            }
        }
    }
}
