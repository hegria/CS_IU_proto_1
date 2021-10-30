//
// Created by RFX on 2021-07-22.
//

#include "TimberDetector.h"
#include "utils.h"

#define PI 3.14159

TimberDetector::TimberDetector(const DetectorParam& param) : param(param)
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
;


    if(param.size_combine_filtering) {
        std::vector<Contour> combined_contours;
        std::vector<Contour> filtered1, filtered2;
        filterContours2(filtered1, contours, img_bgr.cols, img_bgr.rows);
        combined_contours = removeOverflow(filtered2, filtered1, img_bgr.cols, img_bgr.rows);
        divideMergedContour(filtered2, combined_contours, img_bgr.cols, img_bgr.rows);
        filterContours(result, filtered2, img_bgr.cols, img_bgr.rows, param.cnt_filter_th);
    }
    else
        filterContours(result, contours, img_bgr.cols, img_bgr.rows, param.cnt_filter_th);

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

void TimberDetector::filterContours(std::vector<Contour>& dst, const std::vector<Contour>& src, int w, int h, double th) const
{
    int border_w = w / 200;
    int border_h = h / 100;

    int left = border_w;
    int right = w - border_w;
    int top = border_h;
    int bottom = h - border_h;

    utils::filter(dst, src, [=](const Contour& contour) {
        double arclen = cv::arcLength(contour, true);
        double area = cv::contourArea(contour, false);

        if (arclen == 0 || area == 0) return false;

        for (const cv::Point& p : contour) {
            if (p.x < left || p.x > right || p.y < top || p.y > bottom)
                return false;
        }

        double value = arclen * arclen / (area * 4 * PI);
        return value < th;
    });
}

void TimberDetector::filterContours2(std::vector<Contour>& dst, const std::vector<Contour>& src, int w, int h) const
{
    int border_w = w / 200;
    int border_h = h / 100;

    int left = border_w;
    int right = w - border_w;
    int top = border_h;
    int bottom = h - border_h;

    double area_sum = 0;

    utils::filter(dst, src, [&](const Contour& contour) {
        double arclen = cv::arcLength(contour, true);
        double area = cv::contourArea(contour, false);
        if (arclen == 0 || area == 0) return false;
        area_sum += area;

        for (const cv::Point& p : contour) {
            if (p.x < left || p.x > right || p.y < top || p.y > bottom)
                return false;
        }
        return true;
    });

    double mean = area_sum / dst.size();
    std::vector<Contour> dst2;
    utils::filter(dst2, dst, [=](const Contour& contour) {
        double area = cv::contourArea(contour, false);
        if (area / mean >= 5.0f || area / mean <= 0.1f)
            return false;
        return true;
    });

    dst = dst2;
}

void TimberDetector::divideMergedContour(std::vector<Contour>& dst, std::vector<Contour>& src, int w, int h) const
{
    std::vector<cv::Point2f> centers(src.size());
    std::vector<float> radii(src.size());
    const cv::Mat kernel3 = cv::Mat::ones(3, 3, CV_8UC1);

    for (int i = 0; i < src.size(); i++)
    {
        cv::minEnclosingCircle(src[i], centers[i], radii[i]);
        cv::Point offset = cv::Point(centers[i].x - radii[i], centers[i].y - radii[i]);
        cv::Mat error_hull_img = cv::Mat::zeros(radii[i]*2, radii[i]*2, CV_8UC1);// contour이미지에서 hull을 뺀 부분

        std::vector<Contour> hull(1);
        cv::convexHull(src[i], hull[0]);

        cv::drawContours(error_hull_img, std::vector<Contour>(1, hull[0]), -1, cv::Scalar(255), -1, 8, cv::noArray(), 2147483647, -offset);
        cv::drawContours(error_hull_img, std::vector<Contour>(1, src[i]), -1, cv::Scalar(0), -1, 8, cv::noArray(), 2147483647, -offset);


        cv::Vec4f fitline;
        cv::fitLine(src[i], fitline, cv::DIST_L2, 0, 0.01, 0.01);

        int x0 = fitline[2] - offset.x; // 선에 놓은 한 점
        int y0 = fitline[3] - offset.y;
        float vx = fitline[0]; //vector
        float vy = fitline[1];
        float vvx = fitline[1] * -1;//수직 vector
        float vvy = fitline[0];

        //수직선 그리기(수직선 개수 바꿀때 int dot[fitline_num + 1][2];로 바꾸기)
        int line_num = 20;// line_num + 1개의 수직선이 만들어짐
        float line_length = (int)radii[i] * 2;
        float interval = line_length / line_num;
        int dot[21][2];
        int j, max = -1, max_idx;

        for (j = 0; j <= line_num; j++)
        {
            int count; //겹치는 부분 점 개수
            cv::Mat tmp_img = error_hull_img.clone();
            cv::Mat line_img = cv::Mat::zeros(tmp_img.size(), CV_8UC1);
            dot[j][0] = x0 + vx * (interval * j - line_length / 2);
            dot[j][1] = y0 + vy * (interval * j - line_length / 2);
            int x1 = dot[j][0] + vvx * line_length / 2;
            int x2 = dot[j][0] - vvx * line_length / 2;
            int y1 = dot[j][1] + vvy * line_length / 2;
            int y2 = dot[j][1] - vvy * line_length / 2;
            cv::line(line_img, cv::Point(x1, y1), cv::Point(x2, y2), cv::Scalar(255), 2);//수직선그리기

            tmp_img = tmp_img & line_img;
            count = cv::countNonZero(tmp_img);

            if (count > max)
            {
                max = count;
                max_idx = j;
            }
        }
        int max_x1 = dot[max_idx][0] + vvx * line_length / 2;
        int max_x2 = dot[max_idx][0] - vvx * line_length / 2;
        int max_y1 = dot[max_idx][1] + vvy * line_length / 2;
        int max_y2 = dot[max_idx][1] - vvy * line_length / 2;

        cv::Mat single_combine_img = cv::Mat::zeros(error_hull_img.size(), CV_8UC1);
        std::vector<Contour> separated_cnt, filtered_cnt;
        cv::drawContours(single_combine_img, std::vector<Contour>(1, src[i]), -1, cv::Scalar(255), -1, 8, cv::noArray(), 2147483647, -offset);

        cv::line(single_combine_img, cv::Point(max_x1, max_y1), cv::Point(max_x2, max_y2), cv::Scalar(0), 4);

        cv::erode(single_combine_img, single_combine_img, kernel3, cv::Point(-1, -1), 5);
        cv::dilate(single_combine_img, single_combine_img, kernel3, cv::Point(-1, -1), 5);

        cv::findContours(single_combine_img, separated_cnt, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE, offset);
        filterContours(filtered_cnt, separated_cnt, w, h, param.cnt_filter_th);

        if (filtered_cnt.size() !=  0)
        {

            int circle_idx, formfactor = 100, max_idx, max_area = -1;
            for (int k = 0; k < filtered_cnt.size(); k++)
            {
                double arclen = cv::arcLength(filtered_cnt[k], true);
                double area = cv::contourArea(filtered_cnt[k], false);
                if (arclen * arclen / (area * 4 * PI) < formfactor)
                {
                    formfactor = arclen * arclen / (area * 4 * PI);
                    circle_idx = k;
                }
                if (area > max_area)
                {
                    max_area = area;
                    max_idx = k;
                }
            }
            dst.push_back(filtered_cnt[circle_idx]);
            if (circle_idx != max_idx)
            {

                dst.push_back(filtered_cnt[max_idx]);
            }

        }

    }
}


std::vector<Contour> TimberDetector::removeOverflow(std::vector<Contour>& dst, const std::vector<Contour>& src, int w, int h) const
{
    std::vector<Contour> combined_cnts;
    const cv::Mat kernel5 = cv::Mat::ones(5, 5, CV_8UC1);
    const cv::Mat kernel3 = cv::Mat::ones(3, 3, CV_8UC1);
    std::vector<cv::Point2f> centers(src.size());
    std::vector<float> radii(src.size());

    for (int i = 0; i < src.size(); i++)
    {
        cv::minEnclosingCircle(src[i], centers[i], radii[i]);
        if ((double)radii[i] * radii[i] * PI / cv::contourArea(src[i], false) > param.overflow_th)
        {
            cv::Point offset = cv::Point(centers[i].x - radii[i], centers[i].y - radii[i]);
            cv::Mat overflow_img = cv::Mat::zeros(radii[i] * 2, radii[i] * 2, CV_8UC1);
            cv::drawContours(overflow_img, std::vector<Contour>(1, src[i]), -1, cv::Scalar(255), -1, 8, cv::noArray(), 2147483647, -offset);

            cv::erode(overflow_img, overflow_img, kernel5, cv::Point(-1, -1), 3);
            cv::dilate(overflow_img, overflow_img, kernel5, cv::Point(-1, -1), 3);

            std::vector<Contour> filtered_cnts, cnts;
            cv::findContours(overflow_img, cnts, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE, offset);

            filterContours(filtered_cnts, cnts, w, h, param.cnt_filter_th);

            if (filtered_cnts.size() != 0)
            {
                int circle_idx, formfactor = 100, max_idx  , max_area = -1;
                for (int k = 0; k < filtered_cnts.size(); k++)
                {
                    double arclen = cv::arcLength(filtered_cnts[k], true);
                    double area = cv::contourArea(filtered_cnts[k], false);
                    if (arclen * arclen / (area * 4 * PI) < formfactor)
                    {
                        formfactor = arclen * arclen / (area * 4 * PI);
                        circle_idx = k;
                    }

                    if (area > max_area)
                    {
                        max_area = area;
                        max_idx = k;
                    }
                }

                cv::Point2f tempCenter;
                float temp_radius;
                Contour result = filtered_cnts[circle_idx];
                cv::minEnclosingCircle(result, tempCenter, temp_radius);
                if ((double)temp_radius * temp_radius * PI / cv::contourArea(result, false) > param.combine_th)
                {
                    combined_cnts.push_back(result);
                }
                else
                {
                    dst.push_back(result);
                }
                if (max_idx != circle_idx)
                {
                    result = filtered_cnts[max_idx];
                    cv::minEnclosingCircle(result, tempCenter, temp_radius);
                    if ((double)temp_radius * temp_radius * PI / cv::contourArea(result, false) > param.combine_th)
                    {
                        combined_cnts.push_back(result);
                    }
                    else
                    {
                        dst.push_back(result);
                    }
                }

            }

        }
        else
            dst.push_back(src[i]);

    }
    return combined_cnts;
}



