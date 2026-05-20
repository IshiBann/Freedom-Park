package com.mygame.level;

import com.mygame.entity.Key;
import com.mygame.entity.Box;
import com.mygame.entity.Door;
import com.mygame.entity.PressurePlate;

public class Stage2 extends Stage {

	private boolean firstPlateActive = false;
	private Door barrierDoor;

	public Stage2() {
		super();
	}

	@Override
	public void loadStage() {
		stageName = "Stage 2";
		playerSpawnX = 100;
		playerSpawnY = 617;

		loadBackground("/assets/stage 2/Background.png");

		// Ground and basic platforms
		platforms.add(new Platform(0, 700, 1200, 50));
		platforms.add(new Platform(200, 500, 200, 20));
		platforms.add(new Platform(500, 600, 150, 20));
		platforms.add(new Platform(750, 500, 150, 20));
		platforms.add(new Platform(1000, 600, 200, 20));

		// Pressure plate 1 (ground level) - one player holds to open barrier
		PressurePlate plate1 = new PressurePlate(250, 670, 80, 20, () -> {
			firstPlateActive = true;
			if (barrierDoor != null) {
				barrierDoor.unlock();
			}
		}, () -> {
			firstPlateActive = false;
			if (barrierDoor != null) {
				barrierDoor.lock();
			}
		}, false);
		pressurePlates.add(plate1);

		// Barrier door (blocks access to stacking area)
		barrierDoor = new Door(450, 480);
		barrierDoor.lock();

		// Box for stacking (players stack on this to reach high plate)
		boxes.add(new Box(800, 645));

		// High pressure plate (only reachable by stacking) - opens exit
		PressurePlate plate2 = new PressurePlate(820, 415, 80, 20, () -> {
			door.unlock();
		}, () -> {
			door.lock();
		}, true);
		pressurePlates.add(plate2);

		// Key at moderate height (accessible after stacking)
		key = new Key(1050, 550);

		// Exit door
		door = new Door(1100, 510);
		door.lock();

		// Require all players to reach exit
		requireAllPlayersToExit = true;
	}
}

