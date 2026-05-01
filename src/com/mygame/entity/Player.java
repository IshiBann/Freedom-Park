package com.mygame.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import com.mygame.graphics.Animation;
import com.mygame.level.Platform;

public class Player {
    private int x = 100;
    private int y = 686;
    private int speed = 3;
    private boolean facingLeft = false;
    private boolean movingLeft, movingRight;
    private boolean isJumping = false;
    private double jumpVelocity = 0;
    private final double gravity = 0.6;
    private final double jumpPower = -15.0;
    private boolean hasKey = false;
    private Animation walkAnim;
    private BufferedImage idleFrame;
    private BufferedImage jumpIcon;
    List<Box> boxes;

    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        loadAnimations();
        System.out.println(getSpriteHeight());
    }

    public void draw(Graphics g) {
        BufferedImage frame = null;
        
        if (isJumping && jumpIcon != null) {
            frame = jumpIcon;
        } else if (isMoving() && walkAnim != null) {
            frame = walkAnim.getCurrentFrame();
        } else if (idleFrame != null) {
            frame = idleFrame;
        }

        if (frame == null) return;

        if (facingLeft) {
            g.drawImage(frame, x + frame.getWidth(), y, -frame.getWidth(), frame.getHeight(), null);
        } else {
            g.drawImage(frame, x, y, null);
        }

        g.setColor(java.awt.Color.RED);
        g.drawRect(x, y, getSpriteWidth(), getSpriteHeight()); 
    }

    private void loadAnimations() {
        try {
            BufferedImage idleRaw = ImageIO.read(getClass().getResourceAsStream("/assets/Standing.png"));
            BufferedImage f1Raw = ImageIO.read(getClass().getResourceAsStream("/assets/Animation/walking/Walking Frame 1.png"));
            BufferedImage f2Raw = ImageIO.read(getClass().getResourceAsStream("/assets/Animation/walking/Walking Frame 2.png"));
            BufferedImage f3Raw = ImageIO.read(getClass().getResourceAsStream("/assets/Animation/walking/Walking Frame 3.png"));
            BufferedImage f4Raw = ImageIO.read(getClass().getResourceAsStream("/assets/Animation/walking/Walking Frame 4.png"));

            int targetWidth = maxWidth(idleRaw, f1Raw, f2Raw, f3Raw, f4Raw);
            int targetHeight = maxHeight(idleRaw, f1Raw, f2Raw, f3Raw, f4Raw);

            idleFrame = normalizeFrame(idleRaw, targetWidth, targetHeight);
            BufferedImage f1 = normalizeFrame(f1Raw, targetWidth, targetHeight);
            BufferedImage f2 = normalizeFrame(f2Raw, targetWidth, targetHeight);
            BufferedImage f3 = normalizeFrame(f3Raw, targetWidth, targetHeight);
            BufferedImage f4 = normalizeFrame(f4Raw, targetWidth, targetHeight);

            walkAnim = new Animation(new BufferedImage[] { f1, f2, f3, f4 }, 10);
            jumpIcon = normalizeFrame(ImageIO.read(getClass().getResourceAsStream("/assets/Jump Icon.png")), targetWidth, targetHeight);

        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not load player images!");
            e.printStackTrace();
        }
    }

    private BufferedImage normalizeFrame(BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Keep original sprite scale and align feet by anchoring to bottom-center.
        int drawX = (targetWidth - source.getWidth()) / 2;
        int drawY = targetHeight - source.getHeight();
        g2.drawImage(source, drawX, drawY, null);
        g2.dispose();
        return output;
    }

    private int maxWidth(BufferedImage... frames) {
        int max = 0;
        for (BufferedImage frame : frames) {
            max = Math.max(max, frame.getWidth());
        }
        return max;
    }

    private int maxHeight(BufferedImage... frames) {
        int max = 0;
        for (BufferedImage frame : frames) {
            max = Math.max(max, frame.getHeight());
        }
        return max;
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A -> { movingLeft = true; facingLeft = true; }
            case KeyEvent.VK_D -> { movingRight = true; facingLeft = false; }
            case KeyEvent.VK_SPACE -> {
                if (!isJumping) {
                    isJumping = true;
                    jumpVelocity = jumpPower;
                }
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A -> movingLeft = false;
            case KeyEvent.VK_D -> movingRight = false;
        }
    }

    private boolean isMoving() {
        return movingLeft || movingRight;
    }

public void update(List<Platform> platforms, List<Box> boxes) {

    // =====================
    // HORIZONTAL MOVE
    // =====================
    if (movingLeft)  x -= speed;
    if (movingRight) x += speed;

    // =====================
    // GRAVITY
    // =====================
    jumpVelocity += gravity;

    int deltaY = (int) jumpVelocity;

    int pw = getSpriteWidth();
    int ph = getSpriteHeight();

    int prevFeet = y + ph;

    y += deltaY;

    boolean landed = false;

    // =====================
    // PLATFORM COLLISION
    // =====================
    for (Platform platform : platforms) {

        int left = platform.getX();
        int right = platform.getX() + platform.getWidth();
        int top = platform.getY();
        int bottom = platform.getY() + platform.getHeight();

        boolean overlapX = x + pw > left && x < right;
        boolean overlapY = y + ph > top && y < bottom;

        if (overlapX && overlapY) {

            if (jumpVelocity >= 0 && prevFeet <= top) {
                y = top - ph;
                jumpVelocity = 0;
                isJumping = false;
                landed = true;
            }

            else if (jumpVelocity < 0 && y >= bottom - 10) {
                y = bottom;
                jumpVelocity = 0;
            }
        }
    }

    // =====================
    // BOX COLLISION (stand + block)
    // =====================
    for (Box box : boxes) {

        int left = box.getX();
        int right = box.getX() + box.getWidth();
        int top = box.getY();

        boolean overlapX = x + pw > left && x < right;

        // land on box
        if (overlapX &&
            y + ph >= top &&
            prevFeet <= top &&
            jumpVelocity >= 0) {

            y = top - ph;
            jumpVelocity = 0;
            isJumping = false;
            landed = true;
        }
    }

    // =====================
    // ANIMATION
    // =====================
    if (isMoving() && !isJumping && walkAnim != null) {
        walkAnim.update();
    } else if (walkAnim != null) {
        walkAnim.reset();
    }
}

private int getSpriteWidth() {
    return idleFrame != null ? idleFrame.getWidth() : 64;
}

private int getSpriteHeight() {
    return idleFrame != null ? idleFrame.getHeight() : 64;
}
public boolean hasKey() {
return hasKey;
}
public boolean isMovingLeft() {
    return movingLeft;
}

public boolean isMovingRight() {
    return movingRight;
}

public void setHasKey(boolean hasKey) {
    this.hasKey = hasKey;
}

public int getWidth() {
    return getSpriteWidth();
}

public int getHeight() {
    return getSpriteHeight();
}
    // Getters and Setters
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    public void stopMovement() {
        movingLeft = false;
        movingRight = false;
        jumpVelocity = 0;
        isJumping = false;
        hasKey = false;
    }
}