package com.mygame.level;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import com.mygame.entity.Player;

public class StageManager {
    private List<Stage> stages;
    private int currentStageIndex;
    private boolean allStagesCompleted;

    public StageManager() {
        stages = new ArrayList<>();
        currentStageIndex = 0;
        allStagesCompleted = false;
        
        // Load initial stages
        loadStages();
    }

    private void loadStages() {
        stages.add(new Stage1());
        stages.add(new Stage2());
        stages.add(new Stage3());
        stages.add(new Stage4());
        stages.add(new Stage5());
    }

    public void update(Player player) {
        if (allStagesCompleted) return;

        Stage currentStage = getCurrentStage();
        currentStage.update(player);

        if (currentStage.isCompleted()) {
            if (currentStageIndex < stages.size() - 1) {
                currentStageIndex++;
                resetPlayer(player);
            } else {
                allStagesCompleted = true;
            }
        }
    }

    public void draw(Graphics2D g2d) {
        getCurrentStage().draw(g2d);
    }

    private void resetPlayer(Player player) {
        player.setX(getCurrentStage().getPlayerSpawnX());
        player.setY(getCurrentStage().getPlayerSpawnY());
        // Reset movement if necessary
        player.stopMovement();
    }

    public Stage getCurrentStage() {
        return stages.get(currentStageIndex);
    }

    public boolean isAllStagesCompleted() {
        return allStagesCompleted;
    }
    
    public int getCurrentStageIndex() {
        return currentStageIndex;
    }
}
