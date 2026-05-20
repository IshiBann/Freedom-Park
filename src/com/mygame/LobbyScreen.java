package com.mygame;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.LineBorder;

import com.mygame.entity.Player;

public class LobbyScreen extends JPanel {

    private final GamePanel gamePanel;
    private final Timer repaintTimer;

    private Runnable startAction;
    private Runnable backAction;

    private String lobbyTitle = "LOBBY";
    private String lobbySubtitle = "WAITING FOR PLAYERS";
    private String roomLabel = "ROOM: LOCALHOST";
    private int stageIndex = 0;
    private boolean canStart = false;

    private final java.util.List<String> chatMessages = new java.util.ArrayList<>();
    private JTextField chatInput;

    private static final Color BG_A = new Color(8, 4, 18);
    private static final Color BG_B = new Color(22, 8, 34);
    private static final Color PANEL_BG = new Color(12, 8, 24, 235);
    private static final Color PANEL_BORDER = new Color(220, 20, 20, 90);
    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color GOLD_DIM = new Color(255, 215, 0, 140);
    private static final Color RED = new Color(220, 20, 20);
    private static final int MIN_PLAYERS_TO_START = 4;

    public LobbyScreen(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        setPreferredSize(new Dimension(1200, 800));
        setFocusable(true);

        repaintTimer = new Timer(33, e -> repaint());
        repaintTimer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (backAction != null) backAction.run();
                    return;
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Rectangle[] buttons = getButtonRects();
                if (buttons[0].contains(e.getPoint()) && isStartAllowed()) {
                    if (startAction != null) startAction.run();
                } else if (buttons[1].contains(e.getPoint())) {
                    if (backAction != null) backAction.run();
                }
            }
        });

        setLayout(null);
        setupChatInput();

        gamePanel.setChatListener((id, msg) -> {
            addChatMessage("Player " + (id + 1) + ": " + msg);
        });
    }

    private void setupChatInput() {
        chatInput = new JTextField();
        chatInput.setBounds(840, 510, 240, 30);
        chatInput.setBackground(new Color(20, 10, 30));
        chatInput.setForeground(Color.WHITE);
        chatInput.setCaretColor(GOLD);
        chatInput.setBorder(new LineBorder(PANEL_BORDER, 1));
        chatInput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        
        chatInput.addActionListener(e -> {
            String msg = chatInput.getText().trim();
            if (!msg.isEmpty()) {
                gamePanel.sendChatMessage(msg);
                chatInput.setText("");
            }
            requestFocusInWindow(); // Return focus to screen for ESC key etc.
        });

        add(chatInput);
    }

    private void addChatMessage(String msg) {
        synchronized (chatMessages) {
            chatMessages.add(msg);
            if (chatMessages.size() > 15) {
                chatMessages.remove(0);
            }
        }
        repaint();
    }

    public void setStartAction(Runnable startAction) {
        this.startAction = startAction;
    }

    public void setBackAction(Runnable backAction) {
        this.backAction = backAction;
    }

    public void configure(String title, String subtitle, String roomLabel, int stageIndex, boolean canStart) {
        this.lobbyTitle = title;
        this.lobbySubtitle = subtitle;
        this.roomLabel = roomLabel;
        this.stageIndex = stageIndex;
        this.canStart = canStart;
        this.gamePanel.setLobbyStageIndex(stageIndex);
        repaint();
    }

    public int getStageIndex() {
        return stageIndex;
    }

    private boolean isStartAllowed() {
        return canStart && gamePanel.getLobbyPlayerCount() >= MIN_PLAYERS_TO_START;
    }

    private Rectangle[] getButtonRects() {
        int W = getWidth();
        int H = getHeight();
        int bw = 250;
        int bh = 48;
        int gap = 16;
        int x = (W - bw) / 2;
        int y = H - 210;
        return new Rectangle[] {
            new Rectangle(x, y, bw, bh),
            new Rectangle(x, y + bh + gap, bw, bh)
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth();
        int H = getHeight();

        g2.setPaint(new GradientPaint(0, 0, BG_A, W, H, BG_B));
        g2.fillRect(0, 0, W, H);

        g2.setColor(new Color(255, 255, 255, 15));
        for (int y = 0; y < H; y += 6) {
            g2.fillRect(0, y, W, 2);
        }

        g2.setColor(PANEL_BG);
        g2.fillRoundRect(110, 70, W - 220, H - 140, 18, 18);
        g2.setColor(PANEL_BORDER);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(110, 70, W - 220, H - 140, 18, 18);

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(GOLD);
        g2.drawString(lobbyTitle, (W - fm.stringWidth(lobbyTitle)) / 2, 120);

        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        fm = g2.getFontMetrics();
        g2.setColor(GOLD_DIM);
        g2.drawString(lobbySubtitle, (W - fm.stringWidth(lobbySubtitle)) / 2, 145);

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(150, 170, W - 300, 60, 12, 12);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.drawString(roomLabel, 170, 205);

        String stageName = getStageName(gamePanel.getLobbyStageIndex());
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(150, 245, W - 300, 60, 12, 12);
        g2.setColor(GOLD);
        g2.drawString("STAGE: " + stageName, 170, 280);

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(150, 290, W - 300, 34, 10, 10);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.drawString("PLAYERS: " + Math.min(gamePanel.getLobbyPlayerCount(), 4) + "/4", 170, 313);

        drawPlayerPods(g2, W);
        drawChat(g2);
        drawButtons(g2, W, H);
    }

    private void drawChat(Graphics2D g2) {
        int x = 840;
        int y = 170;
        int w = 240;
        int h = 330;

        // Chat Box Background
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(PANEL_BORDER);
        g2.drawRoundRect(x, y, w, h, 12, 12);

        // Header
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        g2.setColor(GOLD_DIM);
        g2.drawString("LOBBY CHAT", x + 10, y + 20);
        g2.drawLine(x + 10, y + 25, x + w - 10, y + 25);

        // Messages
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        int msgY = y + 45;
        synchronized (chatMessages) {
            for (String msg : chatMessages) {
                g2.setColor(new Color(255, 255, 255, 200));
                // Simple wrapping if too long (optional, just truncate for now)
                String display = msg;
                if (display.length() > 28) display = display.substring(0, 25) + "...";
                g2.drawString(display, x + 10, msgY);
                msgY += 18;
            }
        }
        
        // Input label hint
        g2.setFont(new Font(Font.MONOSPACED, Font.ITALIC, 11));
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawString("Press ENTER to send", x, y + h + 45);
    }

    private void drawPlayerPods(Graphics2D g2, int W) {
        List<Player> players = gamePanel.getPlayers();
        int baseY = 340;
        int podW = 220;
        int podH = 88;
        int gap = 18;
        int totalW = (podW * 2) + gap;
        int startX = (W - totalW) / 2;

        for (int i = 0; i < 4; i++) {
            int row = i / 2;
            int col = i % 2;
            int x = startX + col * (podW + gap);
            int y = baseY + row * (podH + gap);

            Player player = null;
            for (Player p : players) {
                if (p.getPlayerID() == i) {
                    player = p;
                    break;
                }
            }

            g2.setColor(player != null ? new Color(25, 18, 36, 240) : new Color(12, 10, 18, 220));
            g2.fillRoundRect(x, y, podW, podH, 14, 14);
            g2.setColor(player != null ? RED : new Color(255, 255, 255, 40));
            g2.drawRoundRect(x, y, podW, podH, 14, 14);

            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
            g2.setColor(player != null ? Color.WHITE : new Color(255, 255, 255, 80));
            String label = player != null ? "PLAYER " + (i + 1) : "EMPTY SLOT";
            g2.drawString(label, x + 18, y + 34);

            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            g2.setColor(GOLD_DIM);
            String status;
            if (player == null) {
                status = "WAITING...";
            } else if (player.getPlayerID() == gamePanel.getLocalPlayerID()) {
                status = "YOU";
            } else if (i == 0) {
                status = "HOST";
            } else {
                status = "READYING UP";
            }
            g2.drawString(status, x + 18, y + 58);
        }
    }

    private void drawButtons(Graphics2D g2, int W, int H) {
        Rectangle[] buttons = getButtonRects();

        boolean startAllowed = isStartAllowed();
        String startLabel;
        if (!canStart) {
            startLabel = "WAITING FOR HOST";
        } else if (gamePanel.getLobbyPlayerCount() < MIN_PLAYERS_TO_START) {
            startLabel = "NEED " + MIN_PLAYERS_TO_START + " PLAYERS";
        } else {
            startLabel = "START GAME";
        }
        drawButton(g2, buttons[0], startLabel, startAllowed);
        drawButton(g2, buttons[1], "BACK TO HOME", true);

        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        g2.setColor(new Color(255, 255, 255, 80));
        String hint = "ESC = BACK   |   LOBBY MODE";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hint, (W - fm.stringWidth(hint)) / 2, H - 28);
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String label, boolean enabled) {
        g2.setColor(enabled ? new Color(40, 20, 36, 240) : new Color(20, 18, 24, 230));
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
        g2.setColor(enabled ? GOLD : new Color(255, 255, 255, 50));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(enabled ? Color.WHITE : new Color(255, 255, 255, 80));
        g2.drawString(label, rect.x + (rect.width - fm.stringWidth(label)) / 2, rect.y + (rect.height + fm.getAscent()) / 2 - 4);
    }

    private String getStageName(int idx) {
        String[] names = {"STAGE 1", "STAGE 2", "STAGE 3", "STAGE 4", "STAGE 5"};
        if (idx < 0 || idx >= names.length) return "STAGE 1";
        return names[idx];
    }
}