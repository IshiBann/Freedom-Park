package com.mygame.level;

import com.mygame.entity.Box;
import com.mygame.entity.Door;
import com.mygame.entity.Key;

public class Stage3 extends Stage {

    public Stage3() {
        super();
    }

    @Override
    public void loadStage() {
        stageName = "Stage 3";
        playerSpawnX = 100;
        playerSpawnY = 617;

        loadBackground("/assets/stage 2/Background.png");

        platforms.add(new Platform(0,   700, 1200, 50));
        platforms.add(new Platform(180, 560, 180, 20));
        platforms.add(new Platform(420, 470, 180, 20));
        platforms.add(new Platform(680, 380, 180, 20));
        platforms.add(new Platform(920, 290, 180, 20));

        boxes.add(new Box(220, 512));
        boxes.add(new Box(760, 332));

        key = new Key(15, 200);
        door = new Door(1080, 604);
    }
}