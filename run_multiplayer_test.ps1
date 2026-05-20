$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectDir

Write-Host "[1/5] Compiling project..." -ForegroundColor Green
javac -d bin src/com/mygame/**/*.java src/com/mygame/*.java 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}
Write-Host "[2/5] Build complete. Launching 4 instances..." -ForegroundColor Green

Start-Sleep -Seconds 1

# Launch HOST instance (Stage 1 - Multiplayer Stage)
Write-Host "[3/5] Starting HOST instance..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectDir'; java -cp bin com.mygame.Main allowlocal host 1" -WindowStyle Normal

Start-Sleep -Seconds 2

# Launch 3 CLIENT instances
Write-Host "[4/5] Starting CLIENT instances..." -ForegroundColor Cyan
1..3 | ForEach-Object {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectDir'; java -cp bin com.mygame.Main allowlocal join 127.0.0.1:9876" -WindowStyle Normal
    Start-Sleep -Seconds 1
}

Write-Host "[5/5] All instances launched!" -ForegroundColor Green
Write-Host "You should see 4 game windows opening. Follow these steps:" -ForegroundColor Yellow
Write-Host "  1. In the HOST window: Select Multiplayer Stage and click START" -ForegroundColor Yellow
Write-Host "  2. The 3 CLIENT windows will join automatically" -ForegroundColor Yellow
Write-Host "  3. You can now test multiplayer with 4 players!" -ForegroundColor Yellow
