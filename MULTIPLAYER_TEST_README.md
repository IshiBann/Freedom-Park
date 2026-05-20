# Multiplayer Test - 4 Instance Launcher

This folder contains automated scripts to launch 4 game instances for local multiplayer testing.

## Quick Start

### Option 1: Batch Script (Easiest)
Double-click: `run_multiplayer_test.bat`

### Option 2: PowerShell Script
1. Right-click `run_multiplayer_test.ps1` → Run with PowerShell
2. If you get a script execution error, run PowerShell as Admin first, then:
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```
   Then run the script again.

## What Happens

1. **Compiles** the project
2. **Launches 4 game windows:**
   - **HOST** (Stage 1 - Multiplayer): Waits for players to join
   - **CLIENT 1, 2, 3**: Join the host automatically

## How to Test

1. **HOST window** opens first - Select **Multiplayer Stage** and click **START**
2. **CLIENT windows** will join and appear in the lobby
3. Once all 4 players are connected, the game starts
4. Test the pressure plate sync (MP Stage 2):
   - One player steps on the button
   - Bridge platform should appear for all players

## Troubleshooting

- **Port 9876 already in use?** Modify the port in both scripts (search for `9876`)
- **"java not found"?** Add Java to your PATH or use full path to java.exe
- **Windows Defender warning?** Click "Run anyway" - it's just running compiled Java code

## For Development

Edit the scripts to:
- Change start stage: Replace `1` with stage index (0, 1, 2, etc.)
- Test different scenarios: Modify `host 1` or `join 127.0.0.1:9876` arguments
- Run more instances: Duplicate the `start` commands

---

**Script created**: 2026-05-20
**Purpose**: Local multiplayer testing with pressure plates and boxes sync
