package com.mygame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.mygame.entity.Player;
import com.mygame.level.StageManager;
import com.mygame.net.GameClient;
import com.mygame.net.GameServer;

public class GamePanel extends JPanel implements Runnable {

    public final int screenWidth = 1200;
    public final int screenHeight = 800;

    private boolean gameFinished = false;
    private Thread gameThread;

    private java.util.List<Player> players;
    private int localPlayerID = -1; // -1 means no local player yet (e.g. server or connecting)
    private StageManager stageManager;

    private GameServer server;
    private GameClient client;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(new Color(30, 30, 30));
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        stageManager = new StageManager();
        players = new java.util.ArrayList<>();

        // Initialize local player for initial testing (will be replaced by network join)
        Player localPlayer = new Player(
            stageManager.getCurrentStage().getPlayerSpawnX(),
            stageManager.getCurrentStage().getPlayerSpawnY()
        );
        localPlayer.setPlayerID(0);
        players.add(localPlayer);
        localPlayerID = 0;

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Player p = getLocalPlayer();
                if (p != null) p.keyPressed(e);

                if (e.getKeyCode() == KeyEvent.VK_R) {
                    if (p != null) stageManager.resetCurrentStage(p);
                    gameFinished = false;
                }

                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    JFrame topFrame = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(GamePanel.this);
                    if (topFrame != null) {
                        boolean isFullscreen =
                            (topFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH)
                            == JFrame.MAXIMIZED_BOTH;

                        topFrame.setExtendedState(
                            isFullscreen ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH
                        );
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                Player p = getLocalPlayer();
                if (p != null) p.keyReleased(e);
            }
        });
    }

    public Player getLocalPlayer() {
        for (Player p : players) {
            if (p.getPlayerID() == localPlayerID) return p;
        }
        return null;
    }

    public java.util.List<Player> getPlayers() {
        return players;
    }

    public void setLocalPlayerID(int id) {
        this.localPlayerID = id;
    }

    public void initServer() {
        server = new GameServer(this);
        server.start();
    }

    public void initClient(String ip) {
        client = new GameClient(this, ip);
        client.start();
    }

    public void startGameAtStage(int stageIndex) {
        stageManager.setCurrentStageIndex(stageIndex);
        synchronized (players) {
            for (Player p : players) {
                stageManager.resetCurrentStage(p);
            }
        }
        gameFinished = false;
        startGameThread();
        requestFocusInWindow();
    }

    public void startGameThread() {
        if (gameThread != null) {
            return;
        }
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / 60;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        if (client != null) {
            // Client sends its input to server
            Player lp = getLocalPlayer();
            if (lp != null) {
                client.sendInput(lp.isMovingLeft(), lp.isMovingRight(), lp.consumeJumpRequest()); 
            }
        }

        // Only server (or single player) should update physics authority
        if (server != null || client == null) {
            Player lp = getLocalPlayer();
            if (lp != null) {
                stageManager.update(lp);
            }

            synchronized(players) {
                for (Player p : players) {
                    p.update(
                        stageManager.getCurrentStage().getPlatforms(),
                        stageManager.getCurrentStage().getBoxes(),
                        players
                    );
                }
            }

            if (server != null) {
                broadcastState();
            }
        } else {
            // Client side: positions are set by server, but we tick animations locally
            synchronized(players) {
                for (Player p : players) {
                    p.updateAnimation();
                }
            }
        }

        if (stageManager.isAllStagesCompleted()) {
            gameFinished = true;
        }
    }

    private void broadcastState() {
        StringBuilder sb = new StringBuilder("STATE,");
        sb.append(stageManager.getCurrentStageIndex());
        
        // Players (4 slots)
        synchronized(players) {
            for (int i = 0; i < 4; i++) {
                Player p = null;
                for (Player pl : players) {
                    if (pl.getPlayerID() == i) {
                        p = pl;
                        break;
                    }
                }
                if (p != null) {
                    sb.append(",").append(p.getX()).append(",").append(p.getY())
                      .append(",").append(p.isMovingLeft() ? 1 : 0)
                      .append(",").append(p.isMovingRight() ? 1 : 0)
                      .append(",").append(p.isJumping() ? 1 : 0);
                } else {
                    sb.append(",0,0,0,0,0"); // Placeholder
                }            }
        }

        // Boxes
        java.util.List<com.mygame.entity.Box> boxes = stageManager.getCurrentStage().getBoxes();
        for (com.mygame.entity.Box b : boxes) {
            sb.append(",").append(b.getX()).append(",").append(b.getY());
        }
        
        if (server != null) {
            server.broadcast(sb.toString().getBytes());
        }
    }

    public StageManager getStageManager() {
        return stageManager;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        stageManager.draw(g2d);
        
        synchronized(players) {
            for (Player p : players) {
                p.draw(g2d);
            }
        }

        // HUD
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));

        // Stage name (top-left)
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(10, 10, 160, 28, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawString(stageManager.getCurrentStage().getStageName(), 20, 29);

        // Reset hint (top-right)
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(screenWidth - 110, 10, 100, 28, 8, 8);
        g2d.setColor(new Color(255, 220, 80));
        g2d.drawString("R \u2014 Reset", screenWidth - 100, 29);

        if (gameFinished) {
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 48));
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRoundRect(screenWidth / 2 - 160, screenHeight / 2 - 50, 320, 70, 16, 16);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Game Clear!", screenWidth / 2 - 140, screenHeight / 2 + 8);
        }
    }
}
