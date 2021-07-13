cmake_minimum_required( VERSION 3.10.2 )

project( CS_IU_proto_1 )

set( pathPROJECT ${PROJECT_SOURCE_DIR}/.. )
set( pathOPENCV ${pathPROJECT}/opencv )
set( pathLIBOPENCV_JAVA ${pathOPENCV}/native/libs/${ANDROID_ABI}/libopencv_java4.so )
set( pathCPP ./src/main/cpp )

set( CMAKE_VERBOSE_MAKEFILE on )

include_directories( ${pathOPENCV}/native/jni/include )

add_library( native-lib SHARED ${pathCPP}/native-lib.cpp )
add_library( lib-opencv SHARED IMPORTED )
set_target_properties( lib-opencv PROPERTIES IMPORTED_LOCATION ${pathLIBOPENCV_JAVA} )

find_library(log-lib log)
target_link_libraries( native-lib lib-opencv ${log-lib} )
