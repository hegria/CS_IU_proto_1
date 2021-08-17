# CS_IU_proto_1

Introduction
---------------
This repository is about the Android AR Timber measureing Application.

We are a CurveSurf team of the 2021 Sungkyunkwan University Industry-Academic Cooperation Project, and we created this measuring app, which is the company's task.

The app distinguishes and recognizes a single piece of wood while filming the cross section of the wood pile in real time, which measures the diameter and number of wood without any tools, visualizes it on-screen, and also provides the ability to manipulate the measured information by capturing

![KakaoTalk_20210817_145203813](https://user-images.githubusercontent.com/79516073/129670965-dba2114e-c074-4b3e-ade1-78a62c4bc46e.png)
.

Development Enviorment
-------------------
JDK Version and Taget API

minSdkVersion 24

targetSdkVersion 30

buildToolsVersion 30.0.3

Jdk Version 1_8

Implementation

Google AR Core API 1.25.0

org.florescu.android.rangeseekbar:rangeseekbar-library:0.3.0

GLESVersion 0x00020000

OpenCV SDK Module 4.5.2

Requirement
-------------------
Because of Size issue, this repo has not openCV API
Import OpenCV to :opencv
1. Remove opencv dependencies ( Project Structure > Depenendcies > app > remove opencv)
2. Same window, Remove opencv module ( ... > opencv > remove module )
3. Import Opencv sdk using name space :opencv(4.5.2)
4. Add opencv dependencies


