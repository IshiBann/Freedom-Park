@echo off
echo Compiling Java project...

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
    echo Compilation FAILED.
    pause
    exit /b 1
)

echo Running game...
java -cp bin com.mygame.Main
pause