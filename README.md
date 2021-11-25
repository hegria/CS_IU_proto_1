# LogScannAR

Introduction
---------------
This repository is about the Android AR Timber measureing Application.

We are a CurveSurf team of the 2021 Sungkyunkwan University Industry-Academic Cooperation Project, and we created this measuring app, which is the company's task.

The app distinguishes and recognizes a single piece of wood while filming the cross section of the wood pile in real time, which measures the diameter and number of wood without any tools, visualizes it on-screen, and also provides the ability to manipulate the measured information by capturing.

![LogScannAR_image](https://user-images.githubusercontent.com/79516073/129670965-dba2114e-c074-4b3e-ade1-78a62c4bc46e.png)

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

GLESVersion 0x00020000 (2.0)

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
5. Perform image processing (OpenCV) to get timber contours  
    *Method I*: perform classical image processing (heuristic) method (see cpp/TimberDetector.h for more details)  
    *Mehtod II*: perform hog & svm classifier to find regions that encloses a timber, which are later than converted into circular contours*  
6. Find Bounding Box from each Contour.
7. Find Ellipse inscribed in Bounding box and derive diameter information.
8. Display the number of timber and diameter of each timber on the screen.
9. Move the information at the time to the Result View by pressing the capture button.
10. Filter specific timber by touching image or adjusting the range of diameter.

(* although when using method II, converting enclosing rectangles to contours is redundant because they're going to be converted back to boxes again, due to maintain compatability with method I, which returns a series of contours, such extra step was employed)
