@echo off
setlocal
cd /d "%~dp0"
where node.exe >nul 2>&1
if errorlevel 1 (
  echo Install Node.js LTS and JDK 21, then run this launcher again.
  exit /b 1
)
if not exist "workspace-engine\build\install\workspace-engine\lib\workspace-engine.jar" (
  call gradlew.bat installDist :workspace-engine:installDist
  if errorlevel 1 exit /b 1
)
cd workspace-desktop
if not exist "node_modules\electron\cli.js" (
  call npm.cmd ci --no-audit --no-fund
  if errorlevel 1 exit /b 1
)
node node_modules\electron\cli.js .
