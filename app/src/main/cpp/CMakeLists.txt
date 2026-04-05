cmake_minimum_required(VERSION 3.22.1)

project(nexterm_pty)

add_library(
        nexterm-pty
        SHARED
        pty_jni.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        nexterm-pty
        ${log-lib}
)