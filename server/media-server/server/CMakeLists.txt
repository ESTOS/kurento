if (${CMAKE_SYSTEM_NAME} MATCHES "Windows")
  # Boost defaults MinGW to WinXP API; Boost.Log needs WaitOnAddress (Win8+).
  set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DWIN32_LEAN_AND_MEAN=1 -DWIN32=1 -D_WIN32_WINNT=0x0A00 -DBOOST_USE_WINAPI_VERSION=0x0A00")
  set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -D_WIN32_WINNT=0x0A00 -DBOOST_USE_WINAPI_VERSION=0x0A00")
endif ()

SET(KMSC_SOURCE
  main.cpp
  version.cpp
  version.hpp
  ServerMethods.cpp
  ServerMethods.hpp
  ResourceManager.cpp
  ResourceManager.hpp
  RequestCache.cpp
  RequestCache.hpp
  CacheEntry.cpp
  CacheEntry.hpp
  logging.cpp
  logging.hpp
  modules.cpp
  modules.hpp
  loadConfig.cpp
  loadConfig.hpp
)

# DeathHandler uses execinfo.h, fork(), etc. — Unix only, not available on MinGW.
if(NOT CMAKE_GENERATOR MATCHES "MSYS Makefiles")
  list(APPEND KMSC_SOURCE
  ${PROJECT_SOURCE_DIR}/3rdparty/DeathHandler/death_handler.cc
  ${PROJECT_SOURCE_DIR}/3rdparty/DeathHandler/death_handler.h
)
endif()

add_definitions(-DBOOST_LOG_DYN_LINK)

add_executable(kurento-media-server ${KMSC_SOURCE})
if(SANITIZERS_ENABLED)
  add_sanitizers(kurento-media-server)
endif()

# Boost.Log triggers false-positive -Warray-bounds on MinGW (main.cpp).
if(CMAKE_SYSTEM_NAME MATCHES "Windows")
  include(CheckCXXCompilerFlag)
  check_cxx_compiler_flag("-Wno-array-bounds" COMPILER_SUPPORTS_WNO_ARRAY_BOUNDS)
  if(COMPILER_SUPPORTS_WNO_ARRAY_BOUNDS)
    set_source_files_properties(main.cpp PROPERTIES COMPILE_FLAGS "-Wno-array-bounds")
  endif()
endif()

add_dependencies(kurento-media-server transport)

set(KMSC_INCLUDE_DIRS
  ${CMAKE_CURRENT_BINARY_DIR}/..
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${CMAKE_CURRENT_SOURCE_DIR}/transport
  ${KmsJsonRpc_INCLUDE_DIRS}
  ${KMSCORE_INCLUDE_DIRS}
)
if(NOT CMAKE_GENERATOR MATCHES "MSYS Makefiles")
  list(APPEND KMSC_INCLUDE_DIRS
    ${PROJECT_SOURCE_DIR}/3rdparty/DeathHandler
  )
endif()
target_include_directories(kurento-media-server PRIVATE ${KMSC_INCLUDE_DIRS})

if(NOT CMAKE_GENERATOR MATCHES "MSYS Makefiles")
target_link_libraries (kurento-media-server
  ${Boost_LIBRARIES}
  transport
  dl
)
else()
target_link_libraries (kurento-media-server
  kmscoreinterface
  ${Boost_LIBRARIES}
  transport
)
endif()

# Export symbols for dlsym to work with dlopen(NULL, ...)
set_target_properties(kurento-media-server PROPERTIES
  ENABLE_EXPORTS ON
)

install(TARGETS kurento-media-server RUNTIME DESTINATION bin)

add_subdirectory(transport)
