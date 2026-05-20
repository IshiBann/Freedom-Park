package com.mygame.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

public class Key {

    private int x;
    private int y;

    private boolean collected = false;
    private boolean used = false;
    private int holderId = -1;

    private BufferedImage image;

    public Key(int x, int y) {
        this.x = x;
        this.y = y;
        loadImage();
    }

    private void loadImage() {
        try {
            image = ImageIO.read(getClass().getResourceAsStream("/assets/Key.png"));
        } catch (IOException e) {
            System.out.println("Could not load key image");
        }
    }

    public void reset() {
        collected = false;
        used = false;
        holderId = -1;
    }

    /** Client-side: mirror who has the key from network state. */
    public void syncFromNetwork(int networkHolderId, boolean used) {
        this.used = used;
        if (used || networkHolderId < 0) {
            collected = false;
            holderId = -1;
            return;
        }
        collected = true;
        holderId = networkHolderId;
    }

    public void update(Player player) {
        update(List.of(player));
    }

    public void update(List<Player> players) {
        if (used || players == null) {
            return;
        }

        if (!collected) {
            for (Player player : players) {
                if (player != null && touches(player)) {
                    collected = true;
                    holderId = player.getPlayerID();
                    applyHolderToPlayers(players);
                    break;
                }
            }
            return;
        }

        applyHolderToPlayers(players);
        Player holder = getHolder(players);
        if (holder != null) {
            x = holder.getX() + holder.getWidth() / 2 - 16;
            y = holder.getY() - 30;
        }
    }

    private void applyHolderToPlayers(List<Player> players) {
        for (Player p : players) {
            if (p != null) {
                p.setHasKey(p.getPlayerID() == holderId);
            }
        }
    }

    private boolean touches(Player player) {
        return player.getX() < x + 32
            && player.getX() + player.getWidth() > x
            && player.getY() < y + 32
            && player.getY() + player.getHeight() > y;
    }

    public Player getHolder(List<Player> players) {
        if (!collected || holderId < 0 || players == null) {
            return null;
        }
        for (Player p : players) {
            if (p != null && p.getPlayerID() == holderId) {
                return p;
            }
        }
        return null;
    }

    public int getHolderId() {
        return holderId;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setUsed(boolean used) {
        this.used = used;
        if (used) {
            holderId = -1;
        }
    }

    public boolean isUsed() {
        return used;
    }

    public void draw(Graphics g) {
        if (used) {
            return;
        }
        if (!collected && image != null) {
            g.drawImage(image, x, y, 32, 32, null);
        }
    }

    /** Badge above the holder so everyone sees who has the key. */
    public void drawHolderIndicator(Graphics2D g2, List<Player> players) {
        if (!collected || used) {
            return;
        }
        Player holder = getHolder(players);
        if (holder == null) {
            return;
        }

        int cx = holder.getX() + holder.getWidth() / 2;
        int badgeY = holder.getY() - 42;

        if (image != null) {
            g2.drawImage(image, cx - 14, badgeY, 28, 28, null);
        }

        String label = "P" + (holderId + 1);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int lw = fm.stringWidth(label) + 12;
        int lx = cx - lw / 2;
        int ly = badgeY - 18;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(lx, ly, lw, 18, 6, 6);
        g2.setColor(new Color(255, 215, 0));
        g2.drawRoundRect(lx, ly, lw, 18, 6, 6);
        g2.setColor(Color.WHITE);
        g2.drawString(label, lx + 6, ly + 13);
    }
}
