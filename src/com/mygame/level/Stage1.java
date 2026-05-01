package com.mygame.level;

import com.mygame.entity.Key;
import com.mygame.entity.Box;
import com.mygame.entity.Door;

public class Stage1 extends Stage {
    public Stage1() {
        super();
    }

    @Override
    public void loadStage() {
        stageName = "Stage 1";
        playerSpawnX = 100;
        playerSpawnY = 617; // Ground level (750 - player height of 64)

        // Load background
        loadBackground("/assets/stage 1/Background.png");

        // Create platforms
        platforms.add(new Platform(0,   700, 1200, 50)); // Ground
        platforms.add(new Platform(300, 600, 200, 20)); // Platform 1
        platforms.add(new Platform(600, 500, 200, 20)); // Platform 2
        platforms.add(new Platform(900, 400, 200, 20)); // Platform 3
        platforms.add(new Platform(600, 250, 200, 20)); // Platform 4
        platforms.add(new Platform(0, 200, 425, 20)); // Platform 5
        key = new Key(20, 100);
        door = new Door(950, 304);
    
    }
}
