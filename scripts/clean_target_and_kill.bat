@echo off
REM Kill any running installer processes and remove the target directory (requires admin if processes are elevated)
echo Stopping any running Typing Speed Test installer processes...
tasklist | findstr /I "Typing Speed Test" >nul
if %ERRORLEVEL%==0 (
  for /f "tokens=2" %%p in ('tasklist ^| findstr /I "Typing Speed Test"') do (
    echo Killing PID %%p
    taskkill /PID %%p /F >nul 2>&1
  )
) else (
  echo No running installer processes found.
)
echo Removing target directory...
rmdir /s /q "%~dp0target"
echo Done.
pause
