# CS_IU_proto_1

Introduction
---------------
This repository is about the Android AR Timber measureing Application.

We are a CurveSurf team of the 2021 Sungkyunkwan University Industry-Academic Cooperation Project, and we created this measuring app, which is the company's task.

The app distinguishes and recognizes a single piece of wood while filming the cross section of the wood pile in real time, which measures the diameter and number of wood without any tools, visualizes it on-screen, and also provides the ability to manipulate the measured information by capturing.

![KakaoTalk_20210817_145203813](https://user-images.githubusercontent.com/79516073/129670965-dba2114e-c074-4b3e-ade1-78a62c4bc46e.png)

We use Google AR Core Sdk, CurvSurf FindSurfaceAPI, OpenGL version 2.0 and OpenCV JAVA Sdk module for development. 

Development Enviorment
-------------------
JDK Version and Taget API

minSdkVersion 24

targetSdkVersion 30

buildToolsVersion 30.0.3

Jdk Version 1_8

Implementation

Google AR Core Sdk 1.25.0

org.florescu.android.rangeseekbar:rangeseekbar-library:0.3.0

GLESVersion 0x00020000

OpenCV SDK Module 4.5.2

Requirement
-------------------
Due to size issue, this repo does not contain OpenCV module.  
To build our app, you must manually import OpenCV to `:opencv` by following steps:
1. Remove opencv dependencies ( Project Structure > Depenendcies > app > remove opencv)
2. Same window, Remove opencv module ( ... > opencv > remove module )
3. Import Opencv sdk using name space :opencv(4.5.2)
4. Add opencv dependencies

Note that this procedure is only needed after cloning this repo. You do not need to repeat this.

Operation Procedure
-------------------
1. Collect Point Cloud by pressing the record button and slowly moving the smartphone left and right.
2. Fix the collected Point Cloud by pressing the stop button.
3. Set Seed Point and find Plane by touching the screen.
4. Send camera image data (format: YUV420_N12) to JNI. (OpenCV codes)
5. Convert image data into `cv::Mat` and resize so that the longest side is 900px wide.
6. Normalize and threshold the *V channel* of the converted `cv::Mat` to get **binary image**.
7. Perform morphological operations on **binary image** to get rid of some noises.
8. Perform watershed segmentation on **binary image**.
  - Distance transform the binary image to get a *distance map*.
  - Get the local maximum points from distance map.
  - Enlarge the points by the factor of `0.7 * distance_value` (from distance map)
  - Label the enlarged points (circles) using `cv::connectedComponents`.
  - Run `cv::watershed` algorithm, while labeled points (circles) being `markers`.
9. Run `cv::findContours` algorithm to get contours.
10. Filter non-circular contours.
11. Find Bounding Box from each Contour.
12. Find Ellipse inscribed in Bounding box and derive diameter information.

13. Display the number of timber and diameter of each timber on the screen.
14. Move the information at the time to the Result View by pressing the capture button.
15. Filter specific timber by touching image or adjusting the range of diameter.
