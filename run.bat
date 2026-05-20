@echo off
pushd "%~dp0"
echo Compiling Java project...

if not exist bin mkdir bin

if exist src\assets (
    xcopy /E /I /Y src\assets bin\assets >nul
) else (
    echo No assets folder to copy; skipping.
)

javac -d bin -sourcepath src ^
    src\com\mygame\Main.java ^
    src\com\mygame\GamePanel.java ^
    src\com\mygame\net\GameServer.java ^
    src\com\mygame\net\GameClient.java ^
    src\com\mygame\entity\*.java ^
    src\com\mygame\graphics\*.java ^
    src\com\mygame\level\*.java ^
    src\com\mygame\MenuScreen.java ^
    src\com\mygame\LobbyScreen.java

if %errorlevel% neq 0 (
    echo Compilation FAILED.
    pause
    popd
    exit /b 1
)

echo Running game...
if "%~1"=="" (
    java -cp bin com.mygame.Main
) else (
    java -cp bin com.mygame.Main %*
)

echo.
echo Usage examples:
echo   run.bat                         - start normally (menu)
echo   run.bat single [stage]          - start single-player
echo   run.bat host [stage]            - host a game
echo   run.bat join 127.0.0.1:9876 allowlocal  - same-PC test (two terminals)
echo   run.bat allowlocal              - menu with local join allowed
echo.
echo Same-PC test: Terminal 1: run.bat host allowlocal
echo                Terminal 2: run.bat join 127.0.0.1:9876 allowlocal
pause
popd
