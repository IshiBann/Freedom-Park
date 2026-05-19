package com.mygame.level;

public class MultiplayerStageManager extends StageManager {

    public MultiplayerStageManager() {
        super();
        // Replace stages loaded by the parent with multiplayer-specific stages
        initMultiplayerStages();
    }
    private void initMultiplayerStages() {
        try {
            java.lang.reflect.Field f = StageManager.class.getDeclaredField("stages");
            f.setAccessible(true);
            java.util.List<Stage> stages = new java.util.ArrayList<>();
            for (int i = 0; i < 5; i++) stages.add(new MultiplayerStage(i));
            f.set(this, stages);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
