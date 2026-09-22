cmake_minimum_required(VERSION 3.22.1)
project("uvpaint")

add_library(uvpaint SHARED
    native-lib.cpp
)

find_library(log-lib log)

target_link_libraries(uvpaint ${log-lib})

# NEXT STEP: vendor or FetchContent assimp (BSD-licensed) here for
# OBJ / glTF / FBX import, e.g.:
# include(FetchContent)
# FetchContent_Declare(assimp GIT_REPOSITORY https://github.com/assimp/assimp.git GIT_TAG v5.4.1)
# FetchContent_MakeAvailable(assimp)
# target_link_libraries(uvpaint assimp)
