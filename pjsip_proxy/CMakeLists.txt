cmake_minimum_required(VERSION 3.22)
project(pjsip_proxy)

set(CMAKE_CXX_STANDARD 20)

find_package(PkgConfig)

if (PkgConfig_FOUND)
    pkg_check_modules(PJPROJECT QUIET libpjproject)
endif()

if (PJPROJECT_FOUND)
    message(STATUS "Found PJSIP via pkg-config")

    add_executable(pjsip_proxy
            src/app.cpp
            src/sip/AppAccount.cpp
            src/sip/AppCall.cpp
            src/sip/AppEndpoint.cpp
            src/transfer/udp/UDPAudioMediaPort.cpp
            src/transfer/udp/UdpChannel.cpp
            src/transfer/socket/UnixSocketAudioMediaPort.cpp
            src/transfer/socket/UnixSocketChannel2.cpp
    )

    target_include_directories(pjsip_proxy PRIVATE
            ${PJPROJECT_INCLUDE_DIRS}
    )

    target_link_libraries(pjsip_proxy
            ${PJPROJECT_LIBRARIES}
            pthread
            m
    )

    target_compile_options(pjsip_proxy PRIVATE
            ${PJPROJECT_CFLAGS_OTHER}
    )

else()
    message(STATUS "pkg-config not found or libpjproject.pc missing, fallback to manual paths")

    set(PJSIP_ROOT "/usr/local" CACHE PATH "PJSIP root directory")

    find_path(PJSIP_INCLUDE_DIR
            NAMES pjsua-lib/pjsua.h
            PATHS
            ${PJSIP_ROOT}/include
            /usr/include
    )

    find_library(PJSUA_LIB NAMES pjsua PATHS ${PJSIP_ROOT}/lib /usr/lib)
    find_library(PJSIP_LIB NAMES pjsip PATHS ${PJSIP_ROOT}/lib /usr/lib)
    find_library(PJSIP_UA_LIB NAMES pjsip-ua PATHS ${PJSIP_ROOT}/lib /usr/lib)
    find_library(PJSIP_SIMPLE_LIB NAMES pjsip-simple PATHS ${PJSIP_ROOT}/lib /usr/lib)
    find_library(PJMEDIA_LIB NAMES pjmedia PATHS ${PJSIP_ROOT}/lib /usr/lib)
    find_library(PJMEDIA_CODEC_LIB NAMES pjmedia-codec PATHS ${PJSIP_ROOT}/lib /usr/lib)
    find_library(PJMEDIA_VIDEODEV_LIB NAMES pjmedia-videodev PATHS ${PJSIP_ROOT}/lib /usr/lib)
    find_library(PJ_LIB NAMES pj PATHS ${PJSIP_ROOT}/lib /usr/lib)

    if (NOT PJSIP_INCLUDE_DIR OR NOT PJSUA_LIB)
        message(FATAL_ERROR "PJSIP not found. Set -DPJSIP_ROOT=/path/to/pjproject")
    endif()

    add_executable(pjsip_proxy
            src/app.cpp
            src/sip/AppAccount.cpp
            src/sip/AppCall.cpp
            src/sip/AppEndpoint.cpp
            src/transfer/udp/UDPAudioMediaPort.cpp
            src/transfer/udp/UdpChannel.cpp
            src/transfer/socket/UnixSocketAudioMediaPort.cpp
            src/transfer/socket/UnixSocketAudioMediaPort.h
            src/transfer/socket/UnixSocketAudioMediaPort.cpp
            src/transfer/socket/UnixSocketChannel2.cpp
    )

    target_include_directories(pjsip_proxy PRIVATE
            ${PJSIP_INCLUDE_DIR}
    )

    target_link_libraries(pjsip_proxy
            ${PJSUA_LIB}
            ${PJSIP_LIB}
            ${PJSIP_UA_LIB}
            ${PJSIP_SIMPLE_LIB}
            ${PJMEDIA_LIB}
            ${PJMEDIA_CODEC_LIB}
            ${PJMEDIA_VIDEODEV_LIB}
            ${PJ_LIB}
            pthread
            m
    )
endif()