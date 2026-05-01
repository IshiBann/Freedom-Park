# Freedom-Park

UPLB-inspired Pico Park-style 2D Java game.

## How to Run on Windows

This project is started with `run.bat`.

1. Open the project folder in VS Code.
2. Make sure your Java files are inside `src\com\mygame\...`.
3. Double-click `run.bat`, or run it from Command Prompt in the project folder.
4. The script compiles the Java sources into `bin` and then launches the game.

## Important

If you add a new `.java` file, you must update `run.bat` so the new file is included in the compile command. Otherwise, the game may fail to build.

## Current Run Flow

- `run.bat` creates the `bin` folder if needed.
- `run.bat` copies assets from `src\assets` into `bin\assets`.
- `run.bat` compiles the Java files.
- `run.bat` runs `com.mygame.Main`.

## VS Code Note

If you use VS Code, just keep the workspace open, edit the source files, and use `run.bat` whenever you want to test the game.
