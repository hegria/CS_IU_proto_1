# CS_IU_proto_1
CurvSurf
PlZ Import OpenCV to :opencv

How To do it
1. Remove opencv dependencies ( Project Structure > Depenendcies > app > remove opencv)
2. Same window, Remove opencv module ( ... > opencv > remove module )
3. Import Opencv sdk using name space :opencv(4.5.2)
4. Add opencv dependencies

현재 진행상항

App팀과 OpenCV 팀의 Integeration 과정 진행중

input -> directallocated된 bytebuffer, image width, height
output -> clipspace로 변형된 좌표들의 집합인 Contour의 리스트. ( List<Contour> ) <- 이 Contour 자료형이 어떻게 될지 잘 모르겠음.
