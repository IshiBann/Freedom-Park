package com.mygame.entity;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Key {

    private int x;
    private int y;

    private boolean collected = false;
    private boolean used = false;

    private BufferedImage image;

    public Key(int x, int y) {
        this.x = x;
        this.y = y;

        try {
            image = ImageIO.read(
                getClass().getResourceAsStream("/assets/Key.png")
            );
        } catch (IOException e) {
            System.out.println("Could not load key image");
        }
    }

    public void update(Player player) {
        if (used) return;

        if (!collected) {

            boolean touching =
                    player.getX() < x + 32 &&
                    player.getX() + player.getWidth() > x &&
                    player.getY() < y + 32 &&
                    player.getY() + player.getHeight() > y;

            if (touching) {
                collected = true;
                player.setHasKey(true);
            }
        }
        else {
            x = player.getX() + 20;
            y = player.getY() - 20;
        }
    }

    public void draw(Graphics g) {
        if (image != null && !used) {
            g.drawImage(image, x, y, 32, 32, null);
        }
        // Collision box highlight (Green)
        g.setColor(java.awt.Color.GREEN);
        g.drawRect(x, y, 32, 32);
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
