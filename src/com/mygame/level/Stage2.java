package com.mygame.level;

import com.mygame.entity.Key;
import com.mygame.entity.Box;
import com.mygame.entity.Door;

public class Stage2 extends Stage {

	public Stage2() {
		super();
	}

	@Override
	public void loadStage() {
		stageName = "Stage 2";
		playerSpawnX = 100;
		playerSpawnY = 617; // adjust as needed

		// Background (replace path with an actual asset when available)
		loadBackground("/assets/stage 2/Background.png");

		// Sample platforms (tweak positions/sizes for your level)
		platforms.add(new Platform(0,   700, 1200, 50)); // Ground
		platforms.add(new Platform(200,  550, 200, 20));
		platforms.add(new Platform(500,  450, 200, 20));
		platforms.add(new Platform(800,  350, 200, 20));

		// Optional entities for this stage - uncomment and set positions when ready
		// key = new Key(400, 420);
		// door = new Door(1050, 304);
		// boxes.add(new Box(600, 400));
	}
}
