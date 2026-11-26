@echo off
setlocal
set "PROJECT=%~dp0"
set "SOURCE=%USERPROFILE%\Downloads\Katie-drive.zip"
if not exist "%SOURCE%" set "SOURCE=%USERPROFILE%\Downloads\Katie - Version 2.01.zip"
set "BLENDER=C:\Program Files\Blender Foundation\Blender 5.3\blender.exe"
if not exist "%SOURCE%" (
 echo ERROR: Katie model ZIP missing from Downloads.
 pause
 exit /b 1
)
if not exist "%BLENDER%" (
 echo ERROR: Blender not found: %BLENDER%
 pause
 exit /b 1
)
cd /d "%PROJECT%"
"%BLENDER%" --background -t 2 --python-exit-code 1 --python "%PROJECT%blender\build_katie.py" -- "%SOURCE%"
if errorlevel 1 (
 echo ERROR: Katie mobile export failed.
 pause
 exit /b 1
)
echo Katie idle, walk, and punch GLB frames were exported successfully to godot\assets.
pause
