@echo off
rem Starts the report server on Windows. The working directory is set to the
rem directory of this script, so local.properties next to the jar is picked
rem up and overrides the packaged configuration. Close the window to stop it.
setlocal
cd /d "%~dp0"

set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

"%JAVA_EXE%" -version >nul 2>&1
if errorlevel 1 (
	echo Java not found: %JAVA_EXE%
	echo The Fronius Report Server needs Java 21 or newer, e.g. from https://adoptium.net
	echo Install it or set JAVA_HOME to its directory.
	pause
	exit /b 1
)

title Fronius Report Server
echo Fronius Report Server - open http://localhost:8084/ ^(default port^)
echo Close this window to stop the server.
echo.
"%JAVA_EXE%" -Xmx256m -jar FroniusReportServer.jar %*
if errorlevel 1 pause
