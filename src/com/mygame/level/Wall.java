package com.mygame.level;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Wall {

    private int x;
    private int y;
    private int width;
    private int height;

    private Color color;
    private BufferedImage image;

    private boolean active = true;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public Wall(int x, int y, int width, int height) {

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.color = new Color(128, 128, 128);

        loadImage();
    }

    public Wall(int x, int y, int width, int height, Color color) {

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.color = color;

        loadImage();
    }

    // =========================================================
    // LOAD IMAGE
    // =========================================================

    private void loadImage() {

        try {

            image = ImageIO.read(
                Wall.class.getResourceAsStream(
                    "/assets/wall.png"
                )
            );

        } catch (IOException e) {

            System.err.println(
                "Could not load wall image: "
                + e.getMessage()
            );

            image = null;
        }
    }

    // =========================================================
    // ACTIVE
    // =========================================================

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // =========================================================
    // DRAW
    // =========================================================

    public void draw(Graphics g) {

        if (!active) {
            return;
        }

        // Draw image
        if (image != null) {

            g.drawImage(
                image,
                x,
                y,
                width,
                height,
                null
            );

        } else {

            // fallback color
            g.setColor(color);
            g.fillRect(x, y, width, height);

            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height);
        }

        // Debug collision box
        g.setColor(Color.RED);
        g.drawRect(x, y, width, height);
    }

    // =========================================================
    // BASIC COLLISION
    // =========================================================

    public boolean intersects(
        int px,
        int py,
        int pw,
        int ph
    ) {

        if (!active) {
            return false;
        }

        return px < x + width &&
               px + pw > x &&
               py < y + height &&
               py + ph > y;
    }

    // =========================================================
    // SIDE COLLISIONS
    // =========================================================

    // Player moving RIGHT into LEFT side of wall
    public boolean collidesLeft(
        int px,
        int py,
        int pw,
        int ph,
        int speed
    ) {

        if (!active) {
            return false;
        }

        return px + pw <= x &&
               px + pw + speed >= x &&
               py + ph > y &&
               py < y + height;
    }

    // Player moving LEFT into RIGHT side of wall
    public boolean collidesRight(
        int px,
        int py,
        int pw,
        int ph,
        int speed
    ) {

        if (!active) {
            return false;
        }

        return px >= x + width &&
               px - speed <= x + width &&
               py + ph > y &&
               py < y + height;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}