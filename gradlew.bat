@echo off
setlocal
set "GRADLE_USER_HOME=%~dp0.gradle-user-home"
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle 9.1 or newer and JDK 25 must already be available on PATH.
  echo This project does not download or install Gradle or Java.
  exit /b 1
)
call gradle -p "%~dp0." %*