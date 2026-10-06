@echo off
setlocal
set "APP_HOME=%~dp0"
cd /d "%APP_HOME%"

if not defined JAVA_HOME (
  echo ERROR: JAVA_HOME is not set. Select JDK 17 in Android Studio or set JAVA_HOME to JDK 17.
  exit /b 3
)

set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVA_EXE%" (
  echo ERROR: %JAVA_EXE% not found.
  exit /b 3
)

"%JAVA_EXE%" -cp "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
