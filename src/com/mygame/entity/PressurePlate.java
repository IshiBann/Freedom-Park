package com.mygame.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Floor button: activates when a player's feet stand on it.
 * <p>
 * Optional sprites (place under {@code src/assets/}):
 * <ul>
 *   <li>{@code PressurePlateOff.png} — unpressed (suggested ~64×16 px)</li>
 *   <li>{@code PressurePlateOn.png} — pressed / lit (same size)</li>
 * </ul>
 * If images are missing, colored rectangles are drawn instead.
 */
public class PressurePlate {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final Runnable onPress;
    private final Runnable onRelease;
    /** If true, effect stays after the player steps off. */
    private final boolean latch;

    private boolean pressed;
    private boolean activated;

    private BufferedImage offImage;
    private BufferedImage onImage;

    public PressurePlate(int x, int y, int width, int height, Runnable onPress) {
        this(x, y, width, height, onPress, null, true);
    }

    public PressurePlate(int x, int y, int width, int height,
                         Runnable onPress, Runnable onRelease, boolean latch) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onPress = onPress;
        this.onRelease = onRelease;
        this.latch = latch;
        loadImages();
    }

    private void loadImages() {
        try {
            offImage = ImageIO.read(getClass().getResourceAsStream("/assets/PressurePlateOff.png"));
            onImage = ImageIO.read(getClass().getResourceAsStream("/assets/PressurePlateOn.png"));
        } catch (IOException e) {
            offImage = null;
            onImage = null;
        }
    }

    public void update(List<Player> players) {
        update(players, null);
    }

    public void update(List<Player> players, List<Box> boxes) {
        boolean anyStanding = false;
        for (Player player : players) {
            if (isPlayerStandingOn(player)) {
                anyStanding = true;
                break;
            }
        }
        if (!anyStanding && boxes != null) {
            for (Box box : boxes) {
                if (isBoxStandingOn(box)) {
                    anyStanding = true;
                    break;
                }
            }
        }

        if (anyStanding && !pressed) {
            pressed = true;
            if (!activated && onPress != null) {
                onPress.run();
                activated = true;
            }
        } else if (!anyStanding && pressed) {
            pressed = false;
            if (!latch && activated && onRelease != null) {
                onRelease.run();
                activated = false;
            }
        }
    }

    /** Feet overlap the top surface of the plate. */
    private boolean isPlayerStandingOn(Player player) {
        int feetY = player.getY() + player.getHeight();
        int pw = player.getWidth();
        int px = player.getX();

        boolean overlapX = px + pw > x && px < x + width;
        boolean onTop = feetY >= y && feetY <= y + height + 8;
        boolean abovePlate = player.getY() + player.getHeight() / 2 <= y + height;

        return overlapX && onTop && abovePlate;
    }

    private boolean isBoxStandingOn(Box box) {
        int feetY = box.getY() + box.getHeight();
        boolean overlapX = box.getX() + box.getWidth() > x && box.getX() < x + width;
        boolean onTop = feetY >= y && feetY <= y + height + 6;
        return overlapX && onTop;
    }

    public void draw(Graphics g) {
        BufferedImage frame = (pressed || activated) && onImage != null ? onImage : offImage;
        if (frame != null) {
            g.drawImage(frame, x, y, width, height, null);
        } else {
            g.setColor(pressed || activated ? new Color(255, 200, 40) : new Color(80, 80, 90));
            g.fillRoundRect(x, y, width, height, 6, 6);
            g.setColor(pressed || activated ? new Color(255, 120, 0) : new Color(40, 40, 50));
            g.drawRoundRect(x, y, width, height, 6, 6);
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isPressed() { return pressed; }
    public boolean isActivated() { return activated; }
}
