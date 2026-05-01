package com.mygame.level;

import com.mygame.entity.Box;
import com.mygame.entity.Door;
import com.mygame.entity.Key;

public class Stage5 extends Stage {

    public Stage5() {
        super();
    }

    @Override
    public void loadStage() {
        stageName = "Stage 5";
        playerSpawnX = 100;
        playerSpawnY = 617;

        loadBackground("/assets/stage 2/Background.png");

        platforms.add(new Platform(0,   700, 1200, 50));
        platforms.add(new Platform(160, 590, 140, 20));
        platforms.add(new Platform(360, 500, 140, 20));
        platforms.add(new Platform(560, 410, 140, 20));
        platforms.add(new Platform(760, 320, 140, 20));
        platforms.add(new Platform(960, 230, 140, 20));

        boxes.add(new Box(190, 542));
        boxes.add(new Box(390, 452));
        boxes.add(new Box(590, 362));
        boxes.add(new Box(790, 272));

        key = new Key(30, 80);
        door = new Door(1080, 604);
    }
}