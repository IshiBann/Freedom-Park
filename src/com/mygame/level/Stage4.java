package com.mygame.level;

import com.mygame.entity.Box;
import com.mygame.entity.Door;
import com.mygame.entity.Key;

public class Stage4 extends Stage {

    public Stage4() {
        super();
    }

    @Override
    public void loadStage() {
        stageName = "Stage 4";
        playerSpawnX = 100;
        playerSpawnY = 617;

        loadBackground("/assets/stage 2/Background.png");

        platforms.add(new Platform(0,   700, 1200, 50));
        platforms.add(new Platform(120, 620, 160, 20));
        platforms.add(new Platform(340, 530, 160, 20));
        platforms.add(new Platform(560, 440, 160, 20));
        platforms.add(new Platform(780, 350, 160, 20));
        platforms.add(new Platform(1000, 260, 160, 20));

        boxes.add(new Box(370, 482));
        boxes.add(new Box(590, 392));
        boxes.add(new Box(810, 302));

        key = new Key(40, 120);
        door = new Door(1080, 604);
    }
}