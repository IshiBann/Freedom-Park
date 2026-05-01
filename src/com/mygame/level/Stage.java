package com.mygame.level;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import com.mygame.entity.Player;
import com.mygame.entity.Door;
import com.mygame.entity.Key;

public abstract class Stage {
    protected List<Platform> platforms;
    protected BufferedImage background;
    protected int playerSpawnX;
    protected int playerSpawnY;
    protected String stageName;
    protected Key key;
    protected Door door;
    protected boolean completed;
    
    public Stage() {
        platforms = new ArrayList<>();
        playerSpawnX = 100;
        playerSpawnY = 250;
        completed = false;
        loadStage();
    }

    public abstract void loadStage();

    public void update(Player player) {
        if (key != null) {
            key.update(player);
        }

        if (door != null) {
            boolean previouslyUnlocked = door.isUnlocked();
            door.update(player);
            
            if (!previouslyUnlocked && door.isUnlocked() && key != null) {
                key.setUsed(true);
            }

            if (door.canEnter(player)) {
                completed = true;
            }
        }
    }

    public void draw(Graphics g) {
        if (background != null) {
            g.drawImage(background, 0, 0, null);
        }

        for (Platform platform : platforms) {
            platform.draw(g);
        }

        if (key != null) {
            key.draw(g);
        }

        if (door != null) {
            door.draw(g);
        }
    }

    protected void loadBackground(String backgroundPath) {
        try {
            background = ImageIO.read(Stage.class.getResourceAsStream(backgroundPath));
        } catch (IOException e) {
            System.err.println("Could not load background: " + backgroundPath);
            background = null;
        }
    }

    // Getters
    public List<Platform> getPlatforms() {
        return platforms;
    }

    public BufferedImage getBackground() {
        return background;
    }

    public int getPlayerSpawnX() {
        return playerSpawnX;
    }

    public int getPlayerSpawnY() {
        return playerSpawnY;
    }

    public String getStageName() {
        return stageName;
    }

    public Key getKey() {
        return key;
    }

    public Door getDoor() {
        return door;
    }

    public boolean isCompleted() {
        return completed;
    }
}
