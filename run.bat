@echo off
echo Compiling Java project...

if not exist bin mkdir bin

xcopy /E /I /Y src\assets bin\assets >nul

javac -d bin src\com\mygame\Main.java src\com\mygame\GamePanel.java src\com\mygame\entity\Player.java src\com\mygame\graphics\Animation.java

if %errorlevel% neq 0 (
    echo Compilation FAILED.
    pause
    exit /b 1
)

echo Running game...
java -cp bin com.mygame.Main
pause