package com.mygame.graphics;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Animation {
    private BufferedImage[] frames;
    private int currentFrameIndex;
    private int delay;
    private int timer;

    public Animation(BufferedImage[] frames, int delay) {
        this.frames = frames;
        this.delay = delay;
        this.currentFrameIndex = 0;
        this.timer = 0;
    }

    public void update() {
        timer++;
        if (timer >= delay) {
            currentFrameIndex++;
            timer = 0;

            if (currentFrameIndex >= frames.length) {
                currentFrameIndex = 0;
            }
        }
    }

    public void draw(Graphics g, int x, int y) {
        g.drawImage(frames[currentFrameIndex], x, y, null);
    }

    // Returns the current frame so Player can draw it with flipping
    public BufferedImage getCurrentFrame() {
        return frames[currentFrameIndex];
    }

    public void reset() {
        currentFrameIndex = 0;
        timer = 0;
    }
}