@echo off
setlocal
pushd "%~dp0"
if exist "bin\Fraynix-Studio.bat" (
    call "bin\Fraynix-Studio.bat" %*
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Run-Studio.ps1" %*
)
set "STUDIO_EXIT=%ERRORLEVEL%"
popd
if not "%STUDIO_EXIT%"=="0" pause
exit /b %STUDIO_EXIT%
