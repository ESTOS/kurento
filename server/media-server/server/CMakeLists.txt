if (${CMAKE_SYSTEM_NAME} MATCHES "Windows")
  set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DWIN32_LEAN_AND_MEAN=1 -DWIN32=1")
endif ()

if(NOT CMAKE_GENERATOR MATCHES "MSYS Makefiles")
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
  ${PROJECT_SOURCE_DIR}/3rdparty/DeathHandler/death_handler.cc
  ${PROJECT_SOURCE_DIR}/3rdparty/DeathHandler/death_handler.h
)
else()
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
  death_handler.cc
  death_handler.h
)
endif()

add_definitions(-DBOOST_LOG_DYN_LINK)

add_executable(kurento-media-server ${KMSC_SOURCE})
if(SANITIZERS_ENABLED)
  add_sanitizers(kurento-media-server)
endif()

add_dependencies(kurento-media-server transport)

if(NOT CMAKE_GENERATOR MATCHES "MSYS Makefiles")
target_link_libraries (kurento-media-server
  ${Boost_LIBRARIES}
  transport
  dl
)

set_property (TARGET kurento-media-server
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/..
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/transport
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${KMSCORE_INCLUDE_DIRS}
    ${PROJECT_SOURCE_DIR}/3rdparty/DeathHandler
)
else()
target_link_libraries (kurento-media-server
  kmscoreinterface
  ${Boost_LIBRARIES}
  transport
)

set_property (TARGET kurento-media-server
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_BINARY_DIR}/..
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/transport
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${KMSCORE_INCLUDE_DIRS}
)
endif()

install(TARGETS kurento-media-server RUNTIME DESTINATION bin)

add_subdirectory(transport)
