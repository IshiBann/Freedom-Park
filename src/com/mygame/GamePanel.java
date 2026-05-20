package com.mygame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.mygame.entity.Player;
import com.mygame.level.Stage;
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
    private Runnable joinDeniedAction;
    private String lastJoinError = "";
    private int lobbyStageIndex = 0;
    private int lobbyPlayerCount = 1;
    private java.util.function.BiConsumer<Integer, String> chatListener;

    /** When true, allows joining localhost / same PC (for dev testing with two terminals). */
    private static boolean allowLocalJoin = false;

    private static final class JoinTarget {
        final String host;
        final int port;

        JoinTarget(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    public static void setAllowLocalJoin(boolean allow) {
        allowLocalJoin = allow;
    }

    public static boolean isAllowLocalJoin() {
        return allowLocalJoin;
    }

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(new Color(30, 30, 30));
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        stageManager = new StageManager();
        players = new java.util.ArrayList<>();

        Player localPlayer = new Player(
            0,
            stageManager.getCurrentStage().getPlayerSpawnX(),
            stageManager.getCurrentStage().getPlayerSpawnY()
        );
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
                } else if (buttons[1].contains(e.getPoint())) {
                    if ((server != null || client != null) && buttons.length > 2) {
                        stageManager.resetCurrentStage(getLocalPlayer());
                        synchronized (players) {
                            for (Player p : players) {
                                if (p.getPlayerID() != localPlayerID) {
                                    p.setX(stageManager.getCurrentStage().getSpawnXForPlayer(p.getPlayerID()));
                                    p.setY(stageManager.getCurrentStage().getSpawnYForPlayer(p.getPlayerID()));
                                    p.setHasKey(false);
                                    p.setWaitingAtExit(false);
                                    p.stopMovement();
                                }
                            }
                        }
                        gameFinished = false;
                        if (server != null) {
                            server.broadcastReset();
                        }
                        inGameMenuOpen = false;
                        repaint();
                    }
                } else if (buttons.length > 2 && buttons[2].contains(e.getPoint()) && homeAction != null) {
                    inGameMenuOpen = false;
                    homeAction.run();
                } else if (buttons.length == 2 && buttons[1].contains(e.getPoint()) && homeAction != null) {
                    inGameMenuOpen = false;
                    homeAction.run();
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Player p = getLocalPlayer();
                if (p != null) {
                    p.stopMovement();
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

    public int getLocalPlayerID() {
        return localPlayerID;
    }

    public void setHomeAction(Runnable action) {
        this.homeAction = action;
    }

    public void setGameStartAction(Runnable action) {
        this.gameStartAction = action;
    }

    public void setJoinDeniedAction(Runnable action) {
        this.joinDeniedAction = action;
    }

    public String getLastJoinError() {
        return lastJoinError;
    }

    public boolean isHosting() {
        return server != null;
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

    public void setChatListener(java.util.function.BiConsumer<Integer, String> listener) {
        this.chatListener = listener;
    }

    public void onChatMessageReceived(int playerID, String message) {
        if (chatListener != null) {
            chatListener.accept(playerID, message);
        }
    }

    public void sendChatMessage(String message) {
        if (server != null) {
            String data = "CHAT,0," + message;
            server.broadcast(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            onChatMessageReceived(0, message);
        } else if (client != null) {
            client.sendChatMessage(message);
        }
    }

    public void ensureLocalPlayer(int id) {
        synchronized (players) {
            for (Player p : players) {
                if (p.getPlayerID() == id) {
                    localPlayerID = id;
                    return;
                }
            }

            var stage = stageManager.getCurrentStage();
            Player localPlayer = new Player(
                id,
                stage.getSpawnXForPlayer(id),
                stage.getSpawnYForPlayer(id)
            );
            players.add(localPlayer);
            localPlayerID = id;
        }
    }

    /** Remote lobby occupant (host or other clients) for the waiting-room UI. */
    public void ensureLobbyPlayer(int id) {
        if (id == localPlayerID) {
            return;
        }
        synchronized (players) {
            for (Player p : players) {
                if (p.getPlayerID() == id) {
                    return;
                }
            }
            players.add(new Player(
                id,
                stageManager.getCurrentStage().getPlayerSpawnX(),
                stageManager.getCurrentStage().getPlayerSpawnY()
            ));
        }
    }

    public void syncLobbyRoster(int[] occupiedIds) {
        if (occupiedIds == null) {
            return;
        }
        for (int id : occupiedIds) {
            if (id == localPlayerID) {
                ensureLocalPlayer(id);
            } else {
                ensureLobbyPlayer(id);
            }
        }
    }

    /** Ensures the host occupies player slot 1 (internal id 0) in the ready room. */
    public void ensureHostInLobby() {
        ensureLocalPlayer(0);
        Player host = getLocalPlayer();
        if (host != null) {
            host.setX(stageManager.getCurrentStage().getSpawnXForPlayer(0));
            host.setY(stageManager.getCurrentStage().getSpawnYForPlayer(0));
        }
        setLobbyPlayerCount(1);
    }

    public void initServer() {
        ensureHostInLobby();
        server = new GameServer(this);
        if (server != null && server.isBound()) {
            server.start();
        } else {
            System.err.println("Server failed to bind. Host mode unavailable.");
        }
    }

    public boolean tryInitClient(String ip) {
        lastJoinError = "";

        if (ip == null || ip.trim().isEmpty()) {
            if (allowLocalJoin) {
                ip = "127.0.0.1:9876";
            } else {
                lastJoinError = "Enter the host computer's address (e.g. 192.168.1.50:9876).";
                return false;
            }
        }

        if (server != null) {
            lastJoinError = "You are already hosting a game. Go back home before joining another room.";
            return false;
        }

        String trimmed = ip.trim();
        if (!allowLocalJoin && (trimmed.matches("\\d+") || trimmed.startsWith(":"))) {
            lastJoinError = "Enter the host IP as well (e.g. 192.168.1.50:9876), not only the port.";
            return false;
        }

        JoinTarget target = parseJoinTarget(ip);
        if (!allowLocalJoin && isLocalHostName(target.host)) {
            lastJoinError = "You cannot join your own game on this computer. Use another player's address.";
            return false;
        }

        initClient(target.host, target.port);
        return true;
    }

    private JoinTarget parseJoinTarget(String ip) {
        String host = "localhost";
        int port = 9876;
        if (ip != null && !ip.trim().isEmpty()) {
            String s = ip.trim();
            if (s.startsWith(":")) {
                try {
                    port = Integer.parseInt(s.substring(1));
                } catch (NumberFormatException e) {
                    // keep default port
                }
            } else if (s.matches("\\d+")) {
                try {
                    port = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    // keep default port
                }
            } else if (s.contains(":")) {
                String[] parts = s.split(":", 2);
                host = parts[0].isEmpty() ? "localhost" : parts[0];
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (Exception e) {
                    // keep default port
                }
            } else {
                host = s;
            }
        }
        return new JoinTarget(host, port);
    }

    private boolean isLocalHostName(String host) {
        if (host == null || host.isEmpty()) {
            return true;
        }
        String h = host.toLowerCase();
        return h.equals("localhost")
            || h.equals("0.0.0.0")
            || h.equals("::1")
            || h.startsWith("127.");
    }

    public void onJoinDenied(String reason) {
        lastJoinError = reason;
        if (client != null) {
            client.stopClient();
            client = null;
        }
        synchronized (players) {
            players.clear();
        }
        localPlayerID = -1;
        if (joinDeniedAction != null) {
            joinDeniedAction.run();
        }
    }

    private void initClient(String host, int port) {
        synchronized (players) {
            players.clear();
        }
        localPlayerID = -1;
        try {
            stageManager = new com.mygame.level.MultiplayerStageManager();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        client = new com.mygame.net.GameClient(this, host, port);
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
        if (server != null && lobbyPlayerCount < 4) {
            return;
        }
        lobbyStageIndex = stageIndex;
        if (server != null) {
            server.broadcastStart(stageIndex);
        }
        // If we're in a multiplayer session (host or client), use separate multiplayer levels
        if (server != null || client != null) {
            try {
                stageManager = new com.mygame.level.MultiplayerStageManager();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        startGameAtStage(stageIndex);
    }

    public void startGameAtStage(int stageIndex) {
        stageManager.setCurrentStageIndex(stageIndex);
        // Single player: player list is cleared on returnToHome — recreate before reset
        if (client == null && server == null) {
            ensureLocalPlayer(0);
        }
        synchronized (players) {
            stageManager.getCurrentStage().reset();
            for (Player p : players) {
                p.setX(stageManager.getCurrentStage().getSpawnXForPlayer(p.getPlayerID()));
                p.setY(stageManager.getCurrentStage().getSpawnYForPlayer(p.getPlayerID()));
                p.setHasKey(false);
                p.setWaitingAtExit(false);
                p.stopMovement();
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
        int buttonCount = (server != null || client != null) ? 3 : 2;
        int totalH = (bh * buttonCount) + (gap * (buttonCount - 1));
        int x = (W - bw) / 2;
        int y = H / 2 - totalH / 2 + 20;
        Rectangle[] rects = new Rectangle[buttonCount];
        for (int i = 0; i < buttonCount; i++) {
            rects[i] = new Rectangle(x, y + i * (bh + gap), bw, bh);
        }
        return rects;
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
            if (lp != null && !lp.isWaitingAtExit()) {
                client.sendInput(lp.isMovingLeft(), lp.isMovingRight(), lp.consumeJumpRequest());
            }
        }

        // Only server (or single player) should update physics authority
        if (server != null || client == null) {
            if (server != null) {
                server.applyNetworkInputs();
            }

            Player lp = getLocalPlayer();
            java.util.List<Player> playersSnapshot;
            synchronized (players) {
                playersSnapshot = new java.util.ArrayList<>(players);
            }
            if (lp != null) {
                stageManager.update(lp, playersSnapshot);
            }

            synchronized(players) {
                for (Player p : players) {
                    if (p.isWaitingAtExit()) {
                        continue;
                    }
                    p.update(
                        stageManager.getCurrentStage().getPlatforms(),
                        stageManager.getCurrentStage().getBoxes(),
                        players,
                        stageManager.getCurrentStage().getWalls()
                    );
                }
            }

            if (server != null) {
                broadcastState();
            }

            // =====================
            // VOID / FELL OFF SCREEN
            // =====================
            boolean anyFell = false;
            synchronized(players) {
                for (Player p : players) {
                    if (p.getY() > screenHeight) {
                        anyFell = true;
                        break;
                    }
                }
            }
            if (anyFell) {
                Player fellLp = getLocalPlayer();
                if (fellLp != null) {
                    stageManager.resetCurrentStage(fellLp);
                }
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
                      .append(",").append(p.isJumping() ? 1 : 0)
                      .append(",").append(p.hasKey() ? 1 : 0)
                      .append(",").append(p.isWaitingAtExit() ? 1 : 0);
                } else {
                    sb.append(",0,0,0,0,0,0,0");
                }
            }
        }

        // Boxes
        java.util.List<com.mygame.entity.Box> boxes = stageManager.getCurrentStage().getBoxes();
        for (com.mygame.entity.Box b : boxes) {
            sb.append(",").append(b.getX()).append(",").append(b.getY());
        }

        // Key and door state
        com.mygame.entity.Key key = stageManager.getCurrentStage().getKey();
        if (key != null) {
            sb.append(",").append(key.isUsed() ? 1 : 0);
        }

        com.mygame.entity.Door door = stageManager.getCurrentStage().getDoor();
        if (door != null) {
            sb.append(",").append(door.isUnlocked() ? 1 : 0);
        }

        // Pressure plates
        java.util.List<com.mygame.entity.PressurePlate> plates = stageManager.getCurrentStage().getPressurePlates();
        for (com.mygame.entity.PressurePlate plate : plates) {
            sb.append(",").append(plate.isPressed() ? 1 : 0);
        }

        if (server != null) {
            server.broadcast(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
                if (!p.isWaitingAtExit()) {
                    p.draw(g2d);
                }
            }
        }

        java.util.List<Player> playersSnapshot;
        synchronized (players) {
            playersSnapshot = new java.util.ArrayList<>(players);
        }
        stageManager.getCurrentStage().drawKeyHolderOverlay(g2d, playersSnapshot);
        drawKeyHolderHud(g2d, playersSnapshot);
        drawExitWaitingHud(g2d, playersSnapshot);

        Player localPlayer = getLocalPlayer();
        if (localPlayer != null && !localPlayer.isWaitingAtExit() && shouldShowLocalPlayerIndicator()) {
            drawLocalPlayerIndicator(g2d, localPlayer);
        }

        // HUD
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));

        // Stage name (top-left)
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(10, 10, 160, 28, 8, 8);
        g2d.setColor(Color.WHITE);
        String stageName = stageManager.getCurrentStage().getStageName();
        if (stageName != null) {
            g2d.drawString(stageName, 20, 29);
        }

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
            int panelH = buttons.length == 3 ? 340 : 300;
            g2d.setColor(new Color(20, 10, 24, 240));
            g2d.fillRoundRect(screenWidth / 2 - 280, screenHeight / 2 - 150, 560, panelH, 18, 18);
            g2d.setColor(new Color(220, 20, 20, 120));
            g2d.drawRoundRect(screenWidth / 2 - 280, screenHeight / 2 - 150, 560, panelH, 18, 18);

            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 28));
            String title = "MAIN MENU";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.setColor(Color.WHITE);
            g2d.drawString(title, screenWidth / 2 - fm.stringWidth(title) / 2, screenHeight / 2 - 95);

            drawGameMenuButton(g2d, buttons[0], "RESUME");
            if (buttons.length > 2) {
                drawGameMenuButton(g2d, buttons[1], "RESTART");
                drawGameMenuButton(g2d, buttons[2], "HOME PAGE");
            } else {
                drawGameMenuButton(g2d, buttons[1], "HOME PAGE");
            }
        }
    }

    private boolean shouldShowLocalPlayerIndicator() {
        if (server != null || client != null) {
            return true;
        }
        synchronized (players) {
            return players.size() > 1;
        }
    }

    private void drawExitWaitingHud(Graphics2D g2d, java.util.List<Player> players) {
        Stage stage = stageManager.getCurrentStage();
        if (!stage.isRequireAllPlayersToExit()) {
            return;
        }
        com.mygame.entity.Door door = stage.getDoor();
        if (door == null || !door.isUnlocked()) {
            return;
        }
        int waiting = stage.getExitWaitingCount();
        if (waiting == 0) {
            return;
        }
        int total = 0;
        for (Player p : players) {
            if (p != null) {
                total++;
            }
        }
        Player local = getLocalPlayer();
        String msg = (local != null && local.isWaitingAtExit())
            ? "You entered — waiting for team (" + waiting + "/" + total + ")"
            : "EXIT: " + waiting + "/" + total + " inside — waiting for all";
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(msg) + 24;
        int x = (screenWidth - w) / 2;
        int y = 74;
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(x, y, w, 26, 8, 8);
        g2d.setColor(new Color(120, 200, 255));
        g2d.drawRoundRect(x, y, w, 26, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawString(msg, x + 12, y + 18);
    }

    private void drawKeyHolderHud(Graphics2D g2d, java.util.List<Player> players) {
        com.mygame.entity.Key key = stageManager.getCurrentStage().getKey();
        if (key == null || !key.isCollected() || key.isUsed()) {
            return;
        }
        int holderId = key.getHolderId();
        if (holderId < 0) {
            return;
        }
        Player local = getLocalPlayer();
        String msg = (local != null && local.getPlayerID() == holderId)
            ? "YOU HAVE THE KEY"
            : "PLAYER " + (holderId + 1) + " HAS THE KEY";
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(msg) + 24;
        int x = (screenWidth - w) / 2;
        int y = 44;
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(x, y, w, 26, 8, 8);
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawRoundRect(x, y, w, 26, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawString(msg, x + 12, y + 18);
    }

    private void drawLocalPlayerIndicator(Graphics2D g2d, Player player) {
        int cx = player.getX() + player.getWidth() / 2;
        int top = player.getY();
        double bob = Math.sin(System.currentTimeMillis() * 0.006) * 5;
        int tipY = (int) (top - 12 + bob);
        int baseY = tipY - 16;

        int[] xs = { cx, cx - 12, cx + 12 };
        int[] ys = { tipY, baseY, baseY };

        Object oldHint = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillPolygon(
            new int[] { xs[0] + 1, xs[1] + 1, xs[2] + 1 },
            new int[] { ys[0] + 1, ys[1] + 1, ys[2] + 1 },
            3
        );
        g2d.setColor(new Color(255, 215, 0, 230));
        g2d.fillPolygon(xs, ys, 3);
        g2d.setColor(new Color(220, 20, 20));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawPolygon(xs, ys, 3);

        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 11));
        String label = "YOU";
        FontMetrics fm = g2d.getFontMetrics();
        int labelX = cx - fm.stringWidth(label) / 2;
        int labelY = baseY - 6;
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.drawString(label, labelX + 1, labelY + 1);
        g2d.setColor(new Color(255, 230, 80));
        g2d.drawString(label, labelX, labelY);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldHint);
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