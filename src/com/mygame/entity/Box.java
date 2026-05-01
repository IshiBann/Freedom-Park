package com.mygame.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import com.mygame.level.Platform;

public class Box {

    private double x;
    private double y;

    private final int width = 48;
    private final int height = 48;

    private double velocityY = 0;
    private final double gravity = 0.6;
    private final double maxFallSpeed = 12;

    private BufferedImage image;

    public Box(int x, int y) {
        this.x = x;
        this.y = y;

        try {
            image = ImageIO.read(
                getClass().getResourceAsStream("/assets/Box.png")
            );
        } catch (IOException e) {
            System.out.println("Could not load box image");
        }
    }

    public void update(Player player, List<Platform> platforms, List<Box> boxes) {

        // =====================
        // GRAVITY
        // =====================
        velocityY += gravity;
        if (velocityY > maxFallSpeed) velocityY = maxFallSpeed;
        y += velocityY;

        // =====================
        // PLATFORM COLLISION
        // =====================
        for (Platform platform : platforms) {

            boolean overlapX =
                    x + width > platform.getX() &&
                    x < platform.getX() + platform.getWidth();

            boolean overlapY =
                    y + height > platform.getY() &&
                    y < platform.getY() + platform.getHeight();

            if (overlapX && overlapY) {

                if (velocityY >= 0) {
                    y = platform.getY() - height;
                    velocityY = 0;
                }
            }
        }

        // =====================
        // BOX-BOX COLLISION
        // =====================
        for (Box other : boxes) {
            if (other == this) continue;

            boolean overlapX =
                    x + width > other.x &&
                    x < other.x + other.width;

            boolean overlapY =
                    y + height > other.y &&
                    y < other.y + other.height;

            if (overlapX && overlapY) {
                double centerX = x + width / 2.0;
                double centerY = y + height / 2.0;
                double otherCenterX = other.x + other.width / 2.0;
                double otherCenterY = other.y + other.height / 2.0;

                double dx = centerX - otherCenterX;
                double dy = centerY - otherCenterY;

                // Resolve along the shallower axis first
                if (Math.abs(dx) > Math.abs(dy)) {
                    // Separate horizontally
                    if (dx > 0) {
                        x = other.x + other.width;
                    } else {
                        x = other.x - width;
                    }
                } else {
                    // Separate vertically
                    if (dy > 0) {
                        y = other.y + other.height;
                        velocityY = 0;
                    } else {
                        y = other.y - height;
                        velocityY = 0;
                    }
                }
            }
        }

        // =====================
        // PLAYER PUSHING (FIXED)
        // =====================
        boolean overlapX =
                player.getX() < x + width &&
                player.getX() + player.getWidth() > x;

        boolean overlapY =
                player.getY() < y + height &&
                player.getY() + player.getHeight() > y;

        if (overlapX && overlapY) {

            // push only if coming from correct side
            if (player.isMovingRight() && player.getX() < x) {
                x += 3;
            }
            else if (player.isMovingLeft() && player.getX() > x) {
                x -= 3;
            }
        }
    }

    public void draw(Graphics g) {
        if (image != null) {
            g.drawImage(image, (int)x, (int)y, width, height, null);
        } else {
            g.setColor(new Color(139, 69, 19));
            g.fillRect((int)x, (int)y, width, height);
        }
        // Collision box
        g.setColor(java.awt.Color.MAGENTA);
        g.drawRect((int)x, (int)y, width, height);
    }

    public int getX() { return (int)x; }
    public int getY() { return (int)y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}