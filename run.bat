@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-25.0.3
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0"
echo Starting MediStock - Full Integrated Application...
java --module-path "%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21.0.6\javafx-base-21.0.6-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar" --add-modules javafx.controls,javafx.fxml -cp "target\classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar" com.medistock.MainApp
