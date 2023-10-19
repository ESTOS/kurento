@echo off
echo Parameter: %*

set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
set JAVA_OPTS=

set MY_PATH=%~dp0\..\module-creator
echo MY_PATH: %MY_PATH%

echo Not system kurento-module-creator, running from %MY_PATH%
set JAVA_JAR=%MY_PATH%/kurento-module-creator-jar-with-dependencies.jar

echo %JAVA_CMD% %JAVA_OPTS% -jar %JAVA_JAR% %*
%JAVA_CMD% %JAVA_OPTS% -jar %JAVA_JAR% %*
