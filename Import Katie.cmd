@echo off
setlocal
set "PROJECT=%~dp0"
set "SOURCE=%USERPROFILE%\Downloads\Katie - Version 2.01.zip"
set "BLENDER=C:\Program Files\Blender Foundation\Blender 5.3\blender.exe"
if not exist "%SOURCE%" (
 echo ERROR: Save Katie - Version 2.01.zip to your Downloads folder first.
 echo Expected: %SOURCE%
 pause
 exit /b 1
)
if not exist "%BLENDER%" (
 echo ERROR: Blender 5.3 not found at %BLENDER%.
 pause
 exit /b 1
)
"%BLENDER%" --background --python-exit-code 1 --python "%PROJECT%blender\import_katie.py" -- "%SOURCE%" --output "%PROJECT%godot\assets\katie.glb"
if errorlevel 1 (
 echo Katie conversion failed. See errors above.
 pause
 exit /b 1
)
echo Katie converted. Verify in Godot before adding this asset to a release.
echo Output: %PROJECT%godot\assets\katie.glb
pause
