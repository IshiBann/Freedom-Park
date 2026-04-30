package com.mygame.entity;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import com.mygame.graphics.Animation;

public class Player {
    private int x = 100;
    private int y = 250;
    private int speed = 3;
    private boolean facingLeft = false;
    private boolean movingLeft, movingRight, movingUp, movingDown;

    private Animation walkAnim;
    private BufferedImage idleFrame;

    public Player() {
        loadAnimations();
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

        System.out.println("idleFrame: " + idleFrame);
        System.out.println("walkAnim: " + walkAnim);

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
        case KeyEvent.VK_W -> movingUp = true;
        case KeyEvent.VK_A -> { movingLeft = true; facingLeft = true; }
        case KeyEvent.VK_S -> movingDown = true;
        case KeyEvent.VK_D -> { movingRight = true; facingLeft = false; }
    }
}

    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> movingUp = false;
            case KeyEvent.VK_A -> movingLeft = false;
            case KeyEvent.VK_S -> movingDown = false;
            case KeyEvent.VK_D -> movingRight = false;
        }
    }

    private boolean isMoving() {
        return movingLeft || movingRight || movingUp || movingDown;
    }

    public void update() {
        if (movingRight) x += speed;
        if (movingLeft)  x -= speed;
        if (movingDown)  y += speed;
        if (movingUp)    y -= speed;

        if (isMoving() && walkAnim != null) {
            walkAnim.update();
        } else if (!isMoving() && walkAnim != null) {
            walkAnim.reset();
        }
    }

    public void draw(Graphics g) {
    BufferedImage frame = null;

    if (isMoving() && walkAnim != null) {
        frame = walkAnim.getCurrentFrame();
    } else if (idleFrame != null) {
        frame = idleFrame;
    }

    if (frame == null) return;

    if (facingLeft) {
        // Draw the image flipped horizontally
        g.drawImage(frame, x + frame.getWidth(), y, -frame.getWidth(), frame.getHeight(), null);
    } else {
        g.drawImage(frame, x, y, null);
    }
}

    
}