@echo off
echo ========================================
echo Freedom Park - Multiplayer Test Launcher
echo ========================================

:: Step 1: Compile the project once
echo [1/3] Compiling project...
if not exist bin mkdir bin
xcopy /E /I /Y src\assets bin\assets >nul
javac -d bin -sourcepath src ^
src\com\mygame\Main.java ^
src\com\mygame\GamePanel.java ^
src\com\mygame\net\GameServer.java ^
src\com\mygame\net\GameClient.java ^
src\com\mygame\entity\*.java ^
src\com\mygame\graphics\*.java ^
src\com\mygame\level\*.java

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed. Please check your code.
    pause
    exit /b 1
)

:: Step 2: Launch the Host
echo [2/3] Launching HOST instance...
start "Freedom Park - HOST" java -cp bin com.mygame.Main

:: Small delay to prevent windows from overlapping perfectly
timeout /t 1 >nul

:: Step 3: Launch the Client
echo [3/3] Launching CLIENT instance...
start "Freedom Park - CLIENT" java -cp bin com.mygame.Main

echo.
echo Success! Both instances should now be open.
echo ----------------------------------------
echo Window 1: Select 'Host Game'
echo Window 2: Select 'Join Game' (use 'localhost')
echo ----------------------------------------
pause