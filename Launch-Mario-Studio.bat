@echo off
setlocal
cd /d "%~dp0"
if exist "bin\Fraynix-Studio.bat" (
  call "bin\Fraynix-Studio.bat" --mario %*
) else (
  call gradlew.bat run --args="--mario %*"
)
