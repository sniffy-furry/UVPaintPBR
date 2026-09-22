cmake_minimum_required(VERSION 3.22.1)
project("uvpaint")

include(FetchContent)

set(ASSIMP_BUILD_ASSIMP_TOOLS OFF CACHE BOOL "" FORCE)
set(ASSIMP_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(ASSIMP_INSTALL OFF CACHE BOOL "" FORCE)
set(ASSIMP_NO_EXPORT ON CACHE BOOL "" FORCE)
set(ASSIMP_BUILD_ALL_IMPORTERS_BY_DEFAULT OFF CACHE BOOL "" FORCE)
set(ASSIMP_BUILD_OBJ_IMPORTER ON CACHE BOOL "" FORCE)
set(ASSIMP_BUILD_GLTF_IMPORTER ON CACHE BOOL "" FORCE)
set(ASSIMP_BUILD_FBX_IMPORTER ON CACHE BOOL "" FORCE)
set(ASSIMP_BUILD_ZLIB ON CACHE BOOL "" FORCE)

FetchContent_Declare(
    assimp
    GIT_REPOSITORY https://github.com/assimp/assimp.git
    GIT_TAG v5.4.1
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(assimp)

add_library(uvpaint SHARED
    native-lib.cpp
    mesh_loader.cpp
)

find_library(log-lib log)

target_include_directories(uvpaint PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(uvpaint assimp ${log-lib})
