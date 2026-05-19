package com.mygame;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private volatile boolean inGameMenuOpen = false;

    private java.util.List<Player> players;
    private int localPlayerID = -1; // -1 means no local player yet (e.g. server or connecting)
    private StageManager stageManager;

    private GameServer server;
    private GameClient client;
    private Runnable homeAction;
    private Runnable gameStartAction;
    private int lobbyStageIndex = 0;
    private int lobbyPlayerCount = 1;

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
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    inGameMenuOpen = !inGameMenuOpen;
                    repaint();
                    return;
                }

                if (inGameMenuOpen) {
                    return;
                }

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
                if (inGameMenuOpen) {
                    return;
                }
                Player p = getLocalPlayer();
                if (p != null) p.keyReleased(e);
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!inGameMenuOpen) {
                    return;
                }

                Rectangle[] buttons = getGameMenuButtonRects();
                if (buttons[0].contains(e.getPoint())) {
                    inGameMenuOpen = false;
                    repaint();
                } else if (buttons[1].contains(e.getPoint()) && homeAction != null) {
                    inGameMenuOpen = false;
                    homeAction.run();
                }
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

    public void setHomeAction(Runnable action) {
        this.homeAction = action;
    }

    public void setGameStartAction(Runnable action) {
        this.gameStartAction = action;
    }

    public void setLobbyStageIndex(int stageIndex) {
        this.lobbyStageIndex = stageIndex;
        if (server != null) {
            server.setLobbyStageIndex(stageIndex);
        }
    }

    public int getLobbyStageIndex() {
        return lobbyStageIndex;
    }

    public void setLobbyPlayerCount(int count) {
        this.lobbyPlayerCount = count;
    }

    public int getLobbyPlayerCount() {
        return lobbyPlayerCount;
    }

    public void ensureLocalPlayer(int id) {
        synchronized (players) {
            for (Player p : players) {
                if (p.getPlayerID() == id) {
                    localPlayerID = id;
                    return;
                }
            }

            Player localPlayer = new Player(
                id,
                stageManager.getCurrentStage().getPlayerSpawnX(),
                stageManager.getCurrentStage().getPlayerSpawnY()
            );
            localPlayer.setPlayerID(id);
            players.add(localPlayer);
            localPlayerID = id;
        }
    }

    public void initServer() {
        server = new GameServer(this);
        server.start();
    }

    public void initClient(String ip) {
        synchronized (players) {
            players.clear();
        }
        localPlayerID = -1;
        client = new GameClient(this, ip);
        client.start();
    }

    public void returnToHome() {
        inGameMenuOpen = false;
        gameFinished = false;
        stopGameThread();

        if (client != null) {
            client.stopClient();
            client = null;
        }

        if (server != null) {
            server.stopServer();
            server = null;
        }

        synchronized (players) {
            players.clear();
        }

        localPlayerID = -1;
        stageManager = new StageManager();
        lobbyStageIndex = 0;
        lobbyPlayerCount = 1;
        repaint();
    }

    public void startLobbyGame(int stageIndex) {
        lobbyStageIndex = stageIndex;
        if (server != null) {
            server.broadcastStart(stageIndex);
        }
        startGameAtStage(stageIndex);
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
        if (gameStartAction != null) {
            gameStartAction.run();
        }
        requestFocusInWindow();
    }

    public void startGameThread() {
        if (gameThread != null) {
            return;
        }
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void stopGameThread() {
        gameThread = null;
    }

    private Rectangle[] getGameMenuButtonRects() {
        int W = getWidth();
        int H = getHeight();
        int bw = 240;
        int bh = 48;
        int gap = 16;
        int totalH = (bh * 2) + gap;
        int x = (W - bw) / 2;
        int y = H / 2 - totalH / 2 + 20;
        return new Rectangle[] {
            new Rectangle(x, y, bw, bh),
            new Rectangle(x, y + bh + gap, bw, bh)
        };
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

        if (inGameMenuOpen) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, screenWidth, screenHeight);

            Rectangle[] buttons = getGameMenuButtonRects();
            g2d.setColor(new Color(20, 10, 24, 240));
            g2d.fillRoundRect(screenWidth / 2 - 280, screenHeight / 2 - 150, 560, 300, 18, 18);
            g2d.setColor(new Color(220, 20, 20, 120));
            g2d.drawRoundRect(screenWidth / 2 - 280, screenHeight / 2 - 150, 560, 300, 18, 18);

            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 28));
            String title = "MAIN MENU";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.setColor(Color.WHITE);
            g2d.drawString(title, screenWidth / 2 - fm.stringWidth(title) / 2, screenHeight / 2 - 95);

            drawGameMenuButton(g2d, buttons[0], "RESUME");
            drawGameMenuButton(g2d, buttons[1], "HOME PAGE");
        }
    }

    private void drawGameMenuButton(Graphics2D g2d, Rectangle rect, String label) {
        g2d.setColor(new Color(35, 20, 40, 240));
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
        g2d.setColor(new Color(255, 215, 0, 150));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(Color.WHITE);
        g2d.drawString(label, rect.x + (rect.width - fm.stringWidth(label)) / 2, rect.y + (rect.height + fm.getAscent()) / 2 - 3);
    }
}
