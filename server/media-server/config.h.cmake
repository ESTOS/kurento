#ifndef __KURENTO_MEDIA_SERVER_CONFIG_H__
#define __KURENTO_MEDIA_SERVER_CONFIG_H__

#if defined(_WIN32)
/* Boost defaults MinGW to WinXP; Boost.Log needs WaitOnAddress (Win8+). */
#if !defined(_WIN32_WINNT)
#define _WIN32_WINNT 0x0A00
#endif
#if !defined(BOOST_USE_WINAPI_VERSION)
#define BOOST_USE_WINAPI_VERSION 0x0A00
#endif
#endif

/* Version */
#cmakedefine PROJECT_VERSION "@PROJECT_VERSION@"

#cmakedefine TEST_DIRECTORY "@TEST_DIRECTORY@"

/* Root URI with test files (e.g. http:// or file://) */
#cmakedefine TEST_FILES_LOCATION "@TEST_FILES_LOCATION@"

#endif /* __KURENTO_MEDIA_SERVER_CONFIG_H__ */
