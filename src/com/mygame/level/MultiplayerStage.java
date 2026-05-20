package com.mygame.level;

import com.mygame.entity.Box;
import com.mygame.entity.Door;
import com.mygame.entity.Key;
import com.mygame.entity.PressurePlate;

public class MultiplayerStage extends Stage {

    private final int index;

    public MultiplayerStage(int idx) {
        super();
        this.index = idx; // ← MUST come before init()
        init();           // ← now loadStage() sees the correct index
    }

    @Override
    public void loadStage() {
        switch (index) {
            case 0:
                stageName = "MP 1 — Stack Up";
                playerSpawnX = 380;
                playerSpawnY = 617;
                loadBackground("/assets/stage 1/Background.png");
                platforms.add(new Platform(0, 700, 1200, 50));
                platforms.add(new Platform(418, 688, 84, 12));
                key = new Key(438, 235);
                door = new Door(1050, 604);
                requireAllPlayersToExit = true;
                break;
            case 1:
                stageName = "MP 2 — Staircase & Button";
                playerSpawnX = 50;
                playerSpawnY = 617;
                loadBackground("/assets/stage 2/Background.png");

                // Ground platform
                platforms.add(new Platform(0, 700, 1200, 50));

                // Climbing staircase (left side)
                platforms.add(new Platform(200, 540, 140, 20));   // Step 2
                platforms.add(new Platform(340, 460, 140, 20));   // Step 3

            
                final Platform bridgePlatform = new Platform(780, 380, 200, 20);
                bridgePlatform.setActive(false);
                platforms.add(bridgePlatform);


                // Pressure plate - appears only while standing on it, disappears when leaving
                pressurePlates.add(new PressurePlate(820, 680, 100, 20,
                    () -> bridgePlatform.setActive(true),
                    () -> bridgePlatform.setActive(false),
                    false));

                // Key at the end of the bridge
                key = new Key(820, 330);

                // Exit door on the far side
                door = new Door(950, 605);
                requireAllPlayersToExit = true;
                break;
            case 2:
                stageName = "MP 3 — Three-Button Lock";
                playerSpawnX = 100;
                playerSpawnY = 617;
                loadBackground("/assets/stage 3/Background.png");

                // Ground platform
                platforms.add(new Platform(0, 700, 1200, 50));

                // === STAIR 1 (controlled by Button 1) ===
                final Platform stair1 = new Platform(280, 560, 120, 20);
                stair1.setActive(false);
                platforms.add(stair1);

                pressurePlates.add(new PressurePlate(120, 680, 80, 15,
                    () -> stair1.setActive(true),
                    () -> stair1.setActive(false),
                    true));

                // === STAIR 2 (controlled by Button 2) ===
                final Platform stair2 = new Platform(420, 480, 120, 20);
                stair2.setActive(false);
                platforms.add(stair2);

                pressurePlates.add(new PressurePlate(420, 680, 80, 15,
                    () -> stair2.setActive(true),
                    () -> stair2.setActive(false),
                    false));

                // === STAIR 3 (controlled by Button 3) ===
                final Platform stair3 = new Platform(560, 400, 120, 20);
                stair3.setActive(false);
                platforms.add(stair3);

                pressurePlates.add(new PressurePlate(720, 680, 80, 15,
                    () -> stair3.setActive(true),
                    () -> stair3.setActive(false),
                    false));

                // === STAIR 4 (always visible) ===
                platforms.add(new Platform(700, 320, 120, 20));

                // === HIGH PLATFORM (LEFT SIDE) ===
                platforms.add(new Platform(30, 300, 120, 20));


                // === HIGH PLATFORM (LEFT SIDE) ===
                platforms.add(new Platform(0, 200, 120, 20));

                // === DOOR WALL (controlled by high platform button) ===
                final Wall doorWall = new Wall(1000, 405,50, 500);
                walls.add(doorWall);

                pressurePlates.add(new PressurePlate(40, 280, 100, 15,
                    () -> doorWall.setActive(false),
                    () -> doorWall.setActive(true),
                    false));
                boxes.add(new Box(60, 190));
                // === KEY AND DOOR ===
                key = new Key(1000, 280);
                door = new Door(1080, 604);
                requireAllPlayersToExit = true;
                break;
            case 3:
                stageName = "MP 4 — Four Corners";
                playerSpawnX = 80;
                playerSpawnY = 617;
                loadBackground("/assets/stage 4/Background.png");

                // === GROUND ===
                platforms.add(new Platform(0, 700, 1200, 50));

                // === MAIN CENTER PLATFORM ===
                platforms.add(new Platform(450, 520, 300, 20));

                // =========================================================
                // FOUR PRESSURE PLATES REQUIRED TO BUILD THE STAIRCASE
                // Each player must stay on one plate
                // =========================================================

                final Platform step1 = new Platform(780, 470, 100, 20);
                final Platform step2 = new Platform(900, 390, 100, 20);
                final Platform step3 = new Platform(1020, 310, 100, 20);

                step1.setActive(false);
                step2.setActive(false);
                step3.setActive(false);

                platforms.add(step1);
                platforms.add(step2);
                platforms.add(step3);

                // Plate 1
                pressurePlates.add(new PressurePlate(60, 680, 70, 15,
                    () -> step1.setActive(true),
                    () -> step1.setActive(false),
                    false));

                // Plate 2
                pressurePlates.add(new PressurePlate(260, 680, 70, 15,
                    () -> step2.setActive(true),
                    () -> step2.setActive(false),
                    false));

                // Plate 3
                pressurePlates.add(new PressurePlate(460, 680, 70, 15,
                    () -> step3.setActive(true),
                    () -> step3.setActive(false),
                    false));

                // Plate 4 opens key chamber
                final Platform keyWall = new Platform(1080, 180, 40, 140);
                platforms.add(keyWall);

                pressurePlates.add(new PressurePlate(660, 680, 70, 15,
                    () -> keyWall.setActive(false),
                    () -> keyWall.setActive(true),
                    false));

                // === FINAL PLATFORM ===
                platforms.add(new Platform(1020, 230, 140, 20));

                // === KEY ===
                key = new Key(1085, 140);

                // === EXIT DOOR ===
                door = new Door(1080, 604);

                requireAllPlayersToExit = true;
                break;
            case 4:
                stageName = "MP 5 — Sacrifice Tower";
                playerSpawnX = 100;
                playerSpawnY = 617;
                loadBackground("/assets/stage 5/Background.png");

                // === GROUND ===
                platforms.add(new Platform(0, 700, 1200, 50));

                // =========================================================
                // THREE PLAYERS MUST HOLD BUTTONS
                // ONE PLAYER CLIMBS TO GET THE KEY
                // =========================================================

                // Floating staircase
                final Platform climb1 = new Platform(300, 560, 120, 20);
                final Platform climb2 = new Platform(470, 470, 120, 20);
                final Platform climb3 = new Platform(640, 380, 120, 20);

                climb1.setActive(false);
                climb2.setActive(false);
                climb3.setActive(false);

                platforms.add(climb1);
                platforms.add(climb2);
                platforms.add(climb3);

                // Player 1 button
                pressurePlates.add(new PressurePlate(80, 680, 80, 15,
                    () -> climb1.setActive(true),
                    () -> climb1.setActive(false),
                    false));

                // Player 2 button
                pressurePlates.add(new PressurePlate(280, 680, 80, 15,
                    () -> climb2.setActive(true),
                    () -> climb2.setActive(false),
                    false));

                // Player 3 button
                pressurePlates.add(new PressurePlate(480, 680, 80, 15,
                    () -> climb3.setActive(true),
                    () -> climb3.setActive(false),
                    false));

                // =========================================================
                // TOP AREA
                // =========================================================

                platforms.add(new Platform(820, 300, 180, 20));

                // Door blocker
                final Wall exitWall = new Wall(850, 600, 120, 280);
                walls.add(exitWall);

                // Final switch ONLY reachable by climber
                pressurePlates.add(new PressurePlate(860, 280, 80, 15,
                    () -> exitWall.setActive(false),
                    () -> exitWall.setActive(true),
                    false));

                // Key above
                key = new Key(900, 210);

                // Door
                door = new Door(1080, 604);

                requireAllPlayersToExit = true;
                break;
        }
    }
}
