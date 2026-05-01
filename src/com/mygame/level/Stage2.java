package com.mygame.level;

import com.mygame.entity.Key;
import com.mygame.entity.Door;

public class Stage2 extends Stage {
    public Stage2() {
        super();
    }

    @Override
    public void loadStage() {
        stageName = "Stage 2";
        playerSpawnX = 50;
        playerSpawnY = 667;

        // Using Stage 1 background for now as a placeholder
        loadBackground("/assets/stage 1/Background.png");

        // Create a different platform layout for Stage 2
        platforms.add(new Platform(0,   750, 1200, 50)); // Ground
        
        // Zig-zag pattern
        platforms.add(new Platform(100, 600, 200, 20));
        platforms.add(new Platform(400, 500, 200, 20));
        platforms.add(new Platform(700, 400, 200, 20));
        platforms.add(new Platform(400, 300, 200, 20));
        platforms.add(new Platform(100, 200, 200, 20));
        
        // Key at the top left
        key = new Key(150, 160);
        
        // Door at the bottom right
        door = new Door(1000, 667);
    }
}
