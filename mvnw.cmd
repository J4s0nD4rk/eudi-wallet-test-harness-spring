@echo off
setlocal

set WRAPPER_DIR=%~dp0
set PROPERTIES_FILE=%WRAPPER_DIR%.mvn\wrapper\maven-wrapper.properties

if not exist "%PROPERTIES_FILE%" (
  echo Fehler: %PROPERTIES_FILE% nicht gefunden.
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%PROPERTIES_FILE%") do (
  if "%%A"=="distributionUrl" set DISTRIBUTION_URL=%%B
)

if "%DISTRIBUTION_URL%"=="" (
  echo Fehler: distributionUrl fehlt in %PROPERTIES_FILE%.
  exit /b 1
)

for %%F in ("%DISTRIBUTION_URL%") do set DIST_ZIP_NAME=%%~nF
set DIR_NAME=%DIST_ZIP_NAME%
if "%DIR_NAME:~-4%"=="-bin" set DIR_NAME=%DIR_NAME:~0,-4%
set DIST_CACHE_DIR=%USERPROFILE%\.m2\wrapper\dists\%DIST_ZIP_NAME%
set MAVEN_HOME=%DIST_CACHE_DIR%\%DIR_NAME%

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo Lade Maven-Distribution herunter: %DISTRIBUTION_URL%
  if not exist "%DIST_CACHE_DIR%" mkdir "%DIST_CACHE_DIR%"
  powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DIST_CACHE_DIR%\download.zip'"
  powershell -Command "Expand-Archive -Path '%DIST_CACHE_DIR%\download.zip' -DestinationPath '%DIST_CACHE_DIR%' -Force"
  del "%DIST_CACHE_DIR%\download.zip"
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
