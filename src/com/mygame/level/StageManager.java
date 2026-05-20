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
        update(player, java.util.Collections.singletonList(player));
    }

    public void update(Player player, List<Player> allPlayers) {
        if (allStagesCompleted) return;

        Stage currentStage = getCurrentStage();
        currentStage.update(player, allPlayers);

        if (currentStage.isCompleted()) {
            if (currentStageIndex < stages.size() - 1) {
                currentStageIndex++;
                for (Player p : allPlayers) {
                    if (p != null) {
                        resetPlayer(p);
                    }
                }
            } else {
                allStagesCompleted = true;
            }
        }
    }

    public void draw(Graphics2D g2d) {
        getCurrentStage().draw(g2d);
    }

    private void resetPlayer(Player player) {
        player.setX(getCurrentStage().getSpawnXForPlayer(player.getPlayerID()));
        player.setY(getCurrentStage().getSpawnYForPlayer(player.getPlayerID()));
        player.setHasKey(false);
        player.setWaitingAtExit(false);
        player.stopMovement();
    }

    public Stage getCurrentStage() {
        return stages.get(currentStageIndex);
    }

    public void setCurrentStageIndex(int stageIndex) {
        if (stageIndex < 0) {
            currentStageIndex = 0;
        } else if (stageIndex >= stages.size()) {
            currentStageIndex = stages.size() - 1;
        } else {
            currentStageIndex = stageIndex;
        }
        allStagesCompleted = false;
    }

    public boolean isAllStagesCompleted() {
        return allStagesCompleted;
    }
    
    public int getCurrentStageIndex() {
        return currentStageIndex;
    }

    /**
     * Resets the current stage to its initial state and respawns all players.
     */
    public void resetCurrentStage(List<Player> allPlayers) {
        getCurrentStage().reset();
        for (Player p : allPlayers) {
            if (p != null) {
                resetPlayer(p);
            }
        }
    }
}
