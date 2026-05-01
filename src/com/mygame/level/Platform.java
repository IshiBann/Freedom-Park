package com.mygame.level;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Platform {
    private int x;
    private int y;
    private int width;
    private int height;
    private Color color;
    private BufferedImage image;

    public Platform(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = new Color(100, 150, 200); // Blue-grey default
        loadImage();
    }

    public Platform(int x, int y, int width, int height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        loadImage();
    }

    private void loadImage() {
        try {
            image = ImageIO.read(Platform.class.getResourceAsStream("/assets/stage 1/Platform.png"));
        } catch (IOException e) {
            System.err.println("Could not load platform image: " + e.getMessage());
            image = null;
        }
    }

    public void draw(Graphics g) {
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        } else {
            g.setColor(color);
            g.fillRect(x, y, width, height);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height); // Outline
        }
    }

    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // Collision check
    public boolean intersects(int px, int py, int pw, int ph) {
        return px < x + width && px + pw > x && py < y + height && py + ph > y;
    }

    public int getTopY() { return y; }
}
