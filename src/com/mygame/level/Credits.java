package com.mygame.level;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.List;
import com.mygame.entity.Player;

public class Credits extends Stage {
    private long startTime;
    private float scrollOffset = 0;

    public Credits() {
        super();
        stageName = "CREDITS";
        playerSpawnX = 600;
        playerSpawnY = 400;
        startTime = System.currentTimeMillis();
        init();
    }

    @Override
    public void loadStage() {
        completed = true;
    }

    @Override
    public void update(Player player, List<Player> allPlayers) {
        // Credits are automatic, no interaction needed
    }

    @Override
    public void reset() {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void draw(java.awt.Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int W = 1200;
        int H = 800;

        g2d.setColor(new Color(10, 5, 16));
        g2d.fillRect(0, 0, W, H);

        long elapsed = System.currentTimeMillis() - startTime;
        scrollOffset = (elapsed / 50f) % 1000;

        int creditsY = (int)(H + scrollOffset);

        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
        g2d.setColor(new Color(220, 20, 20));
        FontMetrics fm = g2d.getFontMetrics();

        String title = "GAME COMPLETE";
        int titleX = (W - fm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, creditsY);

        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        fm = g2d.getFontMetrics();

        String[] credits = {
            "",
            "THANK YOU FOR PLAYING",
            "",
            "DESIGN & PROGRAMMING",
            "Justine Ivanne Antonio",
            "",
            "ART & ASSETS",
            "Custom Pixel Art",
            "",
            "MUSIC & SOUND",
            "Royalty Free Audio",
            "",
            "SPECIAL THANKS",
            "All Players",
            "",
            "",
            "FREEDOM PARK",
            "© 2026",
            "",
            "THE END"
        };

        int y = creditsY + 100;
        for (String line : credits) {
            if (line.isEmpty()) {
                y += 30;
            } else {
                g2d.setColor(new Color(255, 215, 0));
                int x = (W - fm.stringWidth(line)) / 2;
                g2d.drawString(line, x, y);
                y += 40;
            }
        }
    }
}
