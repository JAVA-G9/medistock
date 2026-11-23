@echo off
setlocal enabledelayedexpansion
title MediStock - Pharmacy Exchange Platform

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
echo ===================================================================
echo  1. Launch Member 3 Standalone UI (Exchange, Marketplace, Transfers)
echo  2. Launch Full Integrated Application (Login -^> Dashboard)
echo  3. Run Member 3 Verification Suite (7/7 Concurrency Tests)
echo ===================================================================
set /p choice="Select an option (1, 2, or 3) [Default: 1]: "

if "%choice%"=="" set choice=1

set JAVAFX_PATH=%USERPROFILE%\.m2\repository\org\openjfx
set MYSQL_JAR=%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar
set MODULE_PATH=target\classes;%MYSQL_JAR%;%JAVAFX_PATH%\javafx-base\21.0.6\javafx-base-21.0.6-win.jar;%JAVAFX_PATH%\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar;%JAVAFX_PATH%\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar;%JAVAFX_PATH%\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar

if not exist "target\classes\module-info.class" (
    echo [INFO] Compiling project classes before initial launch...
    call .\mvnw.cmd test-compile
)

if "%choice%"=="1" (
    echo Starting Member 3 Standalone UI...
    java --module-path "%MODULE_PATH%" -m org.example.medistock/org.example.medistock.ExchangeStandaloneLauncher
) else if "%choice%"=="2" (
    echo Starting MediStock Full Application...
    java --module-path "%MODULE_PATH%" -m org.example.medistock/com.medistock.MainApp
) else if "%choice%"=="3" (
    echo Running Member 3 Verification Suite...
    java --module-path "%MODULE_PATH%" -m org.example.medistock/org.example.medistock.ExchangeBackendVerification
    pause
) else (
    echo Invalid choice.
    pause
)
