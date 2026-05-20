@echo off
cd /d "%~dp0"

REM Build the project
echo [1/5] Compiling project...
javac -d bin -sourcepath src ^
    src\com\mygame\Main.java ^
    src\com\mygame\GamePanel.java ^
    src\com\mygame\net\GameServer.java ^
    src\com\mygame\net\GameClient.java ^
    src\com\mygame\entity\*.java ^
    src\com\mygame\graphics\*.java ^
    src\com\mygame\level\*.java ^
    src\com\mygame\MenuScreen.java ^
    src\com\mygame\LobbyScreen.java >nul 2>&1
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo [2/5] Build complete. Launching 4 instances...

REM Wait a moment
timeout /t 1 /nobreak >nul

REM Launch HOST instance (Stage 1)
echo [3/5] Starting HOST instance...
start "Freedom Park - HOST (Stage 1)" cmd /c "cd /d "%~dp0" && java -cp bin com.mygame.Main allowlocal host 1 && pause"
timeout /t 2 /nobreak >nul

REM Launch 3 CLIENT instances
echo [4/5] Starting CLIENT instances...
start "Freedom Park - CLIENT 1" cmd /c "cd /d "%~dp0" && java -cp bin com.mygame.Main allowlocal join 127.0.0.1:9876 && pause"
timeout /t 1 /nobreak >nul

start "Freedom Park - CLIENT 2" cmd /c "cd /d "%~dp0" && java -cp bin com.mygame.Main allowlocal join 127.0.0.1:9876 && pause"
timeout /t 1 /nobreak >nul

start "Freedom Park - CLIENT 3" cmd /c "cd /d "%~dp0" && java -cp bin com.mygame.Main allowlocal join 127.0.0.1:9876 && pause"

echo [5/5] All instances launched! Check the windows above.
timeout /t 2 /nobreak >nul
