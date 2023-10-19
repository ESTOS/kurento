if (${CMAKE_SYSTEM_NAME} MATCHES "Windows")
  set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DWIN32_LEAN_AND_MEAN=1")
  set (WINSOCK_LIBRARIES libws2_32.a libmswsock.a librpcrt4.a libole32.a)
endif ()

set (WEBSOCKET_SOURCES
  WebSocketTransport.cpp
  WebSocketTransport.hpp
  WebSocketTransportFactory.cpp
  WebSocketTransportFactory.hpp
  WebSocketEventHandler.cpp
  WebSocketEventHandler.hpp
  WebSocketRegistrar.cpp
  WebSocketRegistrar.hpp
)

add_library(websocketTransport
  ${WEBSOCKET_SOURCES}
)
if(SANITIZERS_ENABLED)
  add_sanitizers(websocketTransport)
endif()

if (${CMAKE_SYSTEM_NAME} MATCHES "Windows")
target_link_libraries(websocketTransport
  ${GSTREAMER_LIBRARIES}
  ${OPENSSL_LIBRARIES}
  ${KmsJsonRpc_LIBRARIES}
  ${KMSCORE_LIBRARIES}
  ${WINSOCK_LIBRARIES}
)
else ()
target_link_libraries(websocketTransport
  ${GSTREAMER_LIBRARIES}

  ${OPENSSL_LIBRARIES}
  ${KmsJsonRpc_LIBRARIES}
  ${KMSCORE_LIBRARIES}

)
endif ()

set_property (TARGET websocketTransport
  PROPERTY INCLUDE_DIRECTORIES
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/..
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${GSTREAMER_INCLUDE_DIRS}
    ${KMSCORE_INCLUDE_DIRS}
)
