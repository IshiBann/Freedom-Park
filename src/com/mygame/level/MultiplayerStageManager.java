package com.mygame.level;

import java.util.ArrayList;

public class MultiplayerStageManager extends StageManager {

    public MultiplayerStageManager() {
        super();
    }

    @Override
    protected void loadStages() {
        stages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            stages.add(new MultiplayerStage(i));
        }
        stages.add(new Credits());
    }
}