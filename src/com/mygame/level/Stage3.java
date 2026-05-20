package com.mygame.level;

import com.mygame.entity.Door;
import com.mygame.entity.Key;
import com.mygame.entity.PressurePlate;

/**
 * Stage 3 — "The Span": one switch, a wide gap, and tight jumps to the key.
 */
public class Stage3 extends Stage {

    public Stage3() {
        super();
    }

    @Override
    public void loadStage() {
        stageName = "Stage 3 — The Span";
        playerSpawnX = 60;
        playerSpawnY = 617;

        loadBackground("/assets/stage 3/Background.png");

        // Small starting ledge only — no safe floor across the map
        platforms.add(new Platform(0, 700, 240, 50));

        // Blocks skipping the puzzle by climbing early
        Platform shortcutBlock = new Platform(250, 420, 24, 280);
        platforms.add(shortcutBlock);

        // Bridge appears when the plate is pressed (latched)
        Platform spanBridge = new Platform(240, 605, 300, 20);
        spanBridge.setActive(false);
        platforms.add(spanBridge);

        // Two tight islands after the bridge — no handrails
        platforms.add(new Platform(560, 495, 72, 20));
        platforms.add(new Platform(700, 365, 72, 20));

        // Ledge below the door (must drop from the key route)
        platforms.add(new Platform(980, 625, 100, 20));

        pressurePlates.add(new PressurePlate(110, 684, 72, 16, () -> {
            shortcutBlock.setActive(false);
            spanBridge.setActive(true);
        }));

        key = new Key(720, 325);
        door = new Door(1080, 604);
    }
}
