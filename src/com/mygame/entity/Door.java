package com.mygame.entity;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Door {

    private int x;
    private int y;

    private boolean unlocked = false;

    private BufferedImage lockedImage;
    private BufferedImage unlockedImage;

    public Door(int x, int y) {

        this.x = x;
        this.y = y;

        try {

            lockedImage = ImageIO.read(
                getClass().getResourceAsStream("/assets/DoorLocked.png")
            );

            unlockedImage = ImageIO.read(
                getClass().getResourceAsStream("/assets/DoorUnlocked.png")
            );

        } catch (IOException e) {
            System.out.println("Could not load door images");
        }
    }

    public void update(Player player) {

        boolean touching =
                player.getX() < x + 64 &&
                player.getX() + player.getWidth() > x &&
                player.getY() < y + 96 &&
                player.getY() + player.getHeight() > y;

        if (touching && player.hasKey() && !unlocked) {
            unlocked = true;
            player.setHasKey(false);
            System.out.println("Door unlocked!");
        }
    }

    public void draw(Graphics g) {

        if (unlocked) {
            g.drawImage(unlockedImage, x, y, 64, 96, null);
        }
        else {
            g.drawImage(lockedImage, x, y, 64, 96, null);
        }
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean canEnter(Player player) {
        // Player center must be within the door's width to "enter"
        int playerCenterX = player.getX() + player.getWidth() / 2;
        boolean inDoorCenter = playerCenterX > x + 10 && playerCenterX < x + 54;
        
        boolean touchingY = 
                player.getY() < y + 96 &&
                player.getY() + player.getHeight() > y;

        return unlocked && inDoorCenter && touchingY;
    }
}