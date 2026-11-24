@echo off
setlocal enabledelayedexpansion
title MediStock - Pharmacy Surplus & Exchange Platform

:: Detect JDK
if exist "C:\Program Files\Java\jdk-25.0.3\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.3"
) else (
    set "JAVA_HOME=%JAVA_HOME%"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"

echo ===================================================================
echo               MEDISTOCK - PHARMACY SURPLUS PLATFORM
echo               Integrated Application (All Modules)
echo ===================================================================

set JAVAFX_PATH=%USERPROFILE%\.m2\repository\org\openjfx
set MYSQL_JAR=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar
set MODULE_PATH=target\classes;%MYSQL_JAR%;%JAVAFX_PATH%\javafx-base\21.0.6\javafx-base-21.0.6-win.jar;%JAVAFX_PATH%\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar;%JAVAFX_PATH%\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar;%JAVAFX_PATH%\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar

if not exist "target\classes\module-info.class" (
    echo [INFO] Compiling project classes before initial launch...
    call .\mvnw.cmd test-compile
)

echo Starting MediStock Application...
java --module-path "%MODULE_PATH%" -m org.example.medistock/com.medistock.MainApp
pause
