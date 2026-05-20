@echo off
cd /d "%~dp0"

echo.
echo ========== FREEDOM PARK - MULTIPLAYER TEST ==========
echo.
echo Checking compilation...
if not exist "bin\com\mygame\Main.class" (
    echo ERROR: bin\com\mygame\Main.class not found!
    echo Attempting to compile...
    javac -d bin src/com/mygame/**/*.java src/com/mygame/*.java
    if errorlevel 1 (
        echo Compilation FAILED!
        pause
        exit /b 1
    )
)
echo [OK] Code compiled

echo.
echo Starting SINGLE PLAYER TEST (Stage 1)...
echo This will help us see if the game engine works at all.
echo.
echo If you see "white screen":
echo - Wait 5 seconds
echo - Try pressing ESC or clicking the window
echo.

java -cp bin com.mygame.Main allowlocal single 1

echo.
echo Game closed. Check if you saw the menu or stage.
pause
