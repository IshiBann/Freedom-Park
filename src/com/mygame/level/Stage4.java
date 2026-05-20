package com.mygame.level;

import com.mygame.entity.Box;
import com.mygame.entity.Door;
import com.mygame.entity.Key;
import com.mygame.entity.PressurePlate;

/**
 * Stage 4 — "Hold & Haul": sprint a fading bridge, then wedge a crate on the exit switch.
 */
public class Stage4 extends Stage {

    public Stage4() {
        super();
    }

    @Override
    public void loadStage() {
        stageName = "Stage 4 — Hold & Haul";
        playerSpawnX = 50;
        playerSpawnY = 617;

        loadBackground("/assets/stage 4/Background.png");

        platforms.add(new Platform(0, 700, 200, 50));

        // Permanent bridge after first plate (latched)
        Platform entryBridge = new Platform(200, 610, 260, 20);
        entryBridge.setActive(false);
        platforms.add(entryBridge);

        Platform entryBlock = new Platform(210, 440, 24, 260);
        platforms.add(entryBlock);

        pressurePlates.add(new PressurePlate(90, 684, 72, 16, () -> {
            entryBlock.setActive(false);
            entryBridge.setActive(true);
        }));

        // Central tower — only stop after crossing the bridge
        platforms.add(new Platform(500, 485, 88, 20));

        // Exit route: platform only exists while the switch is held (player or box)
        Platform escapeLedge = new Platform(720, 600, 220, 20);
        escapeLedge.setActive(false);
        platforms.add(escapeLedge);

        Platform exitBlock = new Platform(680, 420, 24, 280);
        platforms.add(exitBlock);

        // Hold switch on the tower — run right or leave the crate here
        pressurePlates.add(new PressurePlate(508, 469, 72, 16,
            () -> {
                exitBlock.setActive(false);
                escapeLedge.setActive(true);
            },
            () -> {
                exitBlock.setActive(true);
                escapeLedge.setActive(false);
            },
            false
        ));

        // Crate spawns on the tower; push it onto the plate to free your hands for the sprint
        boxes.add(new Box(520, 433));

        key = new Key(530, 445);
        door = new Door(1080, 604);
    }
}
