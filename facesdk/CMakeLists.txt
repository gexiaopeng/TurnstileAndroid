# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.6)


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

set(CMAKE_VERBOSE_MAKEFILE on)
set(libs "${CMAKE_SOURCE_DIR}/src/main/jniLibs")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11 -fexceptions -frtti")

include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)

#--------------------------------------------------- import ---------------------------------------------------#
add_library(libavcodec SHARED IMPORTED )
set_target_properties(libavcodec PROPERTIES
                        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libavcodec.a")

add_library(libavformat SHARED IMPORTED )
set_target_properties(libavformat PROPERTIES
                        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libavformat.a")

add_library(libavutil SHARED IMPORTED )
set_target_properties(libavutil PROPERTIES
                        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libavutil.a")

add_library(libswresample SHARED IMPORTED )
set_target_properties(libswresample PROPERTIES
                        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libswresample.a")

add_library(libswscale SHARED IMPORTED )
set_target_properties(libswscale PROPERTIES
                        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libswscale.a")

add_library(libx264 SHARED IMPORTED )
set_target_properties(libx264 PROPERTIES
                        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libx264.a")

add_library(libyuv SHARED IMPORTED )
set_target_properties(libyuv PROPERTIES
                        IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libyuv.a")

add_library( # Sets the name of the library.
             rtsp

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             # src/main/cpp/rtsp.cpp
             src/main/cpp/RtspJni.cpp
             src/main/cpp/RtspClient.hpp
             src/main/cpp/RtspClient.cpp
              )

target_link_libraries(
    rtsp
    android
    log
    m
    z
    libavformat
    libavcodec
    libavutil
    libswresample
    libswscale
    libx264
    libyuv
    )

