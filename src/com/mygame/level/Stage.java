package com.mygame.level;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import com.mygame.entity.Box;
import com.mygame.entity.Door;
import com.mygame.entity.Key;
import com.mygame.entity.Player;
import com.mygame.entity.PressurePlate;

public abstract class Stage {

    protected List<Platform> platforms;
    protected List<Box> boxes;
    protected List<PressurePlate> pressurePlates;
    protected List<Wall> walls;

    protected BufferedImage background;

    protected int playerSpawnX;
    protected int playerSpawnY;

    protected String stageName;

    protected Key key;
    protected Door door;

    protected boolean completed;
    protected boolean requireAllPlayersToExit = false;
    protected final Set<Integer> playersAtExit = new HashSet<>();

    public Stage() {
        platforms = new ArrayList<>();
        boxes = new ArrayList<>();
        pressurePlates = new ArrayList<>();
        walls = new ArrayList<>();

        playerSpawnX = 100;
        playerSpawnY = 250;

        completed = false;

        // !! DO NOT call loadStage() here.
        // Subclasses must call init() at the end of their own constructor,
        // AFTER their own fields (e.g. index) have been assigned.
    }

    /**
     * Subclasses call this at the end of their constructor, after setting
     * their own fields. This ensures loadStage() sees the correct values.
     *
     * Example:
     *   public MyStage(int idx) {
     *       super();
     *       this.index = idx;  // set fields first
     *       init();            // then call init()
     *   }
     */
    protected final void init() {
        loadStage();
    }

    public abstract void loadStage();

    /**
     * Resets this stage back to its initial state by clearing all entities
     * and re-running loadStage(). Call this when the player wants to retry.
     */
    public void reset() {
        platforms.clear();
        boxes.clear();
        pressurePlates.clear();
        walls.clear();
        key = null;
        door = null;
        completed = false;
        playersAtExit.clear();
        loadStage();
        if (key != null) {
            key.reset();
        }
    }

    public void update(Player player) {
        update(player, Collections.singletonList(player));
    }

    public void update(Player player, List<Player> allPlayers) {
        for (PressurePlate plate : pressurePlates) {
            plate.update(allPlayers, boxes);
        }

        for (Box box : boxes) {
            box.update(player, platforms, boxes);
        }

        if (key != null) {
            key.update(allPlayers);
        }

        if (door != null) {
            boolean previouslyUnlocked = door.isUnlocked();
            for (Player p : allPlayers) {
                door.update(p);
            }

            if (!previouslyUnlocked && door.isUnlocked() && key != null) {
                key.setUsed(true);
                for (Player p : allPlayers) {
                    if (p != null) {
                        p.setHasKey(false);
                    }
                }
            }

            for (Player p : allPlayers) {
                if (p == null || !door.isUnlocked() || !door.canEnter(p)) {
                    continue;
                }

                if (requireAllPlayersToExit) {
                    if (!playersAtExit.contains(p.getPlayerID())) {
                        playersAtExit.add(p.getPlayerID());
                        p.setWaitingAtExit(true);
                        // Teleport them back to spawn so they don't block others
                        p.setX(playerSpawnX);
                        p.setY(playerSpawnY);
                    }
                } else {
                    // Single-exit mode: first player through wins
                    completed = true;
                    return;
                }
            }

            // All-players-must-exit mode:
            // Count only players who have NOT yet reached the exit.
            // When that number hits zero, everyone is through — stage complete.
            if (requireAllPlayersToExit && !playersAtExit.isEmpty()) {
                int stillPending = countPendingPlayers(allPlayers);
                if (stillPending == 0) {
                    completed = true;
                }
            }
        }
    }

    /**
     * Returns the number of non-null players who have NOT yet reached the exit.
     * Used to decide when all players have exited in requireAllPlayersToExit mode.
     *
     * Previously this counted ALL present players, so completed never triggered
     * because playersAtExit.size() could never exceed the total player count.
     */
    private int countPendingPlayers(List<Player> allPlayers) {
        int pending = 0;
        for (Player p : allPlayers) {
            if (p != null && !p.isWaitingAtExit()) {
                pending++;
            }
        }
        return pending;
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        if (background != null) {
            java.awt.Composite oldComposite = g2.getComposite();
            g2.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, 0.75f));
            g2.drawImage(background, 0, 0, null);
            g2.setComposite(oldComposite);
        }

        for (Platform platform : platforms) {
            platform.draw(g);
        }

        for (Wall wall : walls) {
            wall.draw(g);
        }

        for (Box box : boxes) {
            box.draw(g);
        }

        for (PressurePlate plate : pressurePlates) {
            plate.draw(g);
        }

        if (key != null) {
            key.draw(g);
        }

        if (door != null) {
            door.draw(g);
        }
    }

    public void drawKeyHolderOverlay(Graphics2D g2, List<Player> players) {
        if (key != null) {
            key.drawHolderIndicator(g2, players);
        }
    }

    public int getSpawnXForPlayer(int playerId) {
        return playerSpawnX + (playerId % 4) * 48;
    }

    public int getSpawnYForPlayer(int playerId) {
        return playerSpawnY;
    }

    protected void loadBackground(String backgroundPath) {
        try {
            background = ImageIO.read(
                Stage.class.getResourceAsStream(backgroundPath)
            );
        } catch (IOException e) {
            System.err.println("Could not load background: " + backgroundPath);
            background = null;
        }
    }

    public List<Platform> getPlatforms()         { return platforms; }
    public List<Box> getBoxes()                  { return boxes; }
    public List<PressurePlate> getPressurePlates(){ return pressurePlates; }
    public List<Wall> getWalls()                 { return walls; }
    public BufferedImage getBackground()          { return background; }
    public int getPlayerSpawnX()                 { return playerSpawnX; }
    public int getPlayerSpawnY()                 { return playerSpawnY; }
    public String getStageName()                 { return stageName; }
    public Key getKey()                          { return key; }
    public Door getDoor()                        { return door; }
    public boolean isCompleted()                 { return completed; }
    public boolean isRequireAllPlayersToExit()   { return requireAllPlayersToExit; }
    public int getExitWaitingCount()             { return playersAtExit.size(); }
}