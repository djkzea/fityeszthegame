@echo off
rem Builds the Windows installer (.msi). jpackage needs the WiX Toolset 3.x on
rem PATH for this; the CI workflow installs it with "choco install wixtoolset".
rem
rem   packaging\package.bat
rem
setlocal

cd /d "%~dp0.."

set NAME=Fityesz Kronika
if "%VERSION%"=="" set VERSION=1.0.0
if "%TYPE%"=="" set TYPE=msi

echo ==^> building the jar
call mvnw.cmd -q clean package -DskipTests || exit /b 1

echo ==^> collecting the dependencies
call mvnw.cmd -q dependency:copy-dependencies -DoutputDirectory=target/app-input -DincludeScope=runtime || exit /b 1
copy /y target\demo-*.jar target\app-input\ >nul

for %%f in (target\app-input\demo-*.jar) do set MAIN_JAR=%%~nxf

rem The wizard's bitmaps are named by absolute path in the WiX source, and WiX
rem compiles in a temporary directory - so the path is filled in here.
powershell -NoProfile -Command ^
  "(Get-Content 'packaging\win\main.wxs.in' -Raw) -replace '@ART@', (Resolve-Path 'packaging\win').Path | Set-Content -NoNewline -Encoding UTF8 'packaging\win\main.wxs'" || exit /b 1

if exist target\winwork rmdir /s /q target\winwork

if exist target\installer rmdir /s /q target\installer

echo ==^> jpackage --type %TYPE%
rem Launcher, not HelloApplication: a main class that extends Application refuses
rem to start when JavaFX is on the class path rather than the module path.
jpackage ^
  --type %TYPE% ^
  --input target\app-input ^
  --main-jar %MAIN_JAR% ^
  --main-class com.example.demo.Launcher ^
  --name "%NAME%" ^
  --app-version %VERSION% ^
  --vendor fityeszthegame.com ^
  --icon packaging\app.ico ^
  --resource-dir packaging\win ^
  --verbose ^
  --win-menu ^
  --win-shortcut ^
  --win-dir-chooser ^
  --temp target\winwork ^
  --dest target\installer || exit /b 1

echo ===== main.wxs =====
type target\winwork\config\main.wxs
echo ===== main.wxs vege =====

echo.
echo kesz:
dir /b target\installer
