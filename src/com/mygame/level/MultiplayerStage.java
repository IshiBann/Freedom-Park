package com.mygame.level;

import com.mygame.entity.Box;
import com.mygame.entity.Door;
import com.mygame.entity.Key;

public class MultiplayerStage extends Stage {

    private final int index;

    public MultiplayerStage(int idx) {
        super();
        this.index = idx;
    }

    @Override
    public void loadStage() {
        // Use same asset files as single-player but keep layout separate
        switch (index) {
            case 0:
                stageName = "MP 1 — Stack Up";
                playerSpawnX = 380;
                playerSpawnY = 617;
                loadBackground("/assets/stage 1/Background.png");

                platforms.add(new Platform(0, 700, 1200, 50));
                platforms.add(new Platform(418, 688, 84, 12));

                // Higher key — needs a full 4-player stack
                key = new Key(438, 235);
                door = new Door(1050, 604);
                requireAllPlayersToExit = true;
                break;
            case 1:
                stageName = "Multiplayer Stage 2";
                playerSpawnX = 120;
                playerSpawnY = 617;
                loadBackground("/assets/stage 2/Background.png");
                platforms.add(new Platform(0,   700, 1200, 50));
                platforms.add(new Platform(220, 540, 200, 20));
                platforms.add(new Platform(520, 460, 200, 20));
                boxes.add(new Box(860, 660));
                key = new Key(800, 300);
                door = new Door(950, 605);
                requireAllPlayersToExit = true;
                break;
            case 2:
                stageName = "Multiplayer Stage 3";
                playerSpawnX = 100;
                playerSpawnY = 617;
                loadBackground("/assets/stage 3/Background.png");
                platforms.add(new Platform(0,   700, 1200, 50));
                platforms.add(new Platform(200, 560, 160, 20));
                platforms.add(new Platform(460, 470, 160, 20));
                boxes.add(new Box(240, 512));
                key = new Key(15, 200);
                door = new Door(1080, 604);
                requireAllPlayersToExit = true;
                break;
            case 3:
                stageName = "Multiplayer Stage 4";
                playerSpawnX = 140;
                playerSpawnY = 617;
                loadBackground("/assets/stage 4/Background.png");
                platforms.add(new Platform(0,   700, 1200, 50));
                platforms.add(new Platform(140, 620, 160, 20));
                platforms.add(new Platform(360, 530, 160, 20));
                boxes.add(new Box(370, 482));
                key = new Key(40, 120);
                door = new Door(1080, 604);
                requireAllPlayersToExit = true;
                break;
            default:
                stageName = "Multiplayer Stage 5";
                playerSpawnX = 100;
                playerSpawnY = 617;
                loadBackground("/assets/stage 5/Background.png");
                platforms.add(new Platform(0,   700, 1200, 50));
                platforms.add(new Platform(160, 590, 140, 20));
                boxes.add(new Box(190, 542));
                key = new Key(30, 80);
                door = new Door(1080, 604);
                requireAllPlayersToExit = true;
                break;
        }
    }
}
