cmake_minimum_required(VERSION 3.22.1)

project("atox_domain_native")

# Путь к собранным нативным зависимостям
set(INSTALL_DIR "${CMAKE_SOURCE_DIR}/../../../../_install/aarch64-linux-android")

add_library(
        nativetox
        SHARED
        NativeToxCore.cpp
        NativeToxAv.cpp
)

# Заголовочные файлы: папка include проекта + заголовки из _install/
target_include_directories(nativetox PRIVATE
        "${CMAKE_CURRENT_SOURCE_DIR}/include"
        "${INSTALL_DIR}/include"
)

find_library(log-lib log)

# Подключаем собранные .a библиотеки из _install/
add_library(libsodium STATIC IMPORTED)
set_target_properties(libsodium PROPERTIES IMPORTED_LOCATION "${INSTALL_DIR}/lib/libsodium.a")

add_library(libopus STATIC IMPORTED)
set_target_properties(libopus PROPERTIES IMPORTED_LOCATION "${INSTALL_DIR}/lib/libopus.a")

add_library(libvpx STATIC IMPORTED)
set_target_properties(libvpx PROPERTIES IMPORTED_LOCATION "${INSTALL_DIR}/lib/libvpx.a")

add_library(libtoxcore STATIC IMPORTED)
set_target_properties(libtoxcore PROPERTIES IMPORTED_LOCATION "${INSTALL_DIR}/lib/libtoxcore.a")

target_link_libraries(
        nativetox
        libtoxcore
        libsodium
        libopus
        libvpx
        ${log-lib}
)