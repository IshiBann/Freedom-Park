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
import com.mygame.level.Stage;
import com.mygame.level.Stage1;
import com.mygame.level.Stage2;
import com.mygame.level.StageManager;

public class GamePanel extends JPanel implements Runnable {
    public final int screenWidth = 1200;
    public final int screenHeight = 800;
    private boolean gameFinished = false;
    private Thread gameThread;
    private Player player;
    private StageManager stageManager;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(new Color(30, 30, 30));
        this.setDoubleBuffered(true);
        this.setFocusable(true);

        stageManager = new StageManager();

        player = new Player(
            stageManager.getCurrentStage().getPlayerSpawnX(),
            stageManager.getCurrentStage().getPlayerSpawnY()
        );

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                player.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    JFrame topFrame = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(GamePanel.this);
                    if (topFrame != null) {
                        boolean isFullscreen = (topFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
                        if (isFullscreen) {
                            topFrame.setExtendedState(JFrame.NORMAL);
                        } else {
                            topFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                        }
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                player.keyReleased(e);
            }
        });
    }

    public void startGameThread() {
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
        player.update(
        currentStage.getPlatforms(),
        currentStage.getBoxes()
    );
        currentStage.update(player);

        if (currentStage.isCompleted()) {
            if (currentStage instanceof Stage1) {
                setCurrentStage(new Stage2());
                player.setHasKey(false);
                gameFinished = false;
            } else {
                gameFinished = true;
            }
        }
    }

    public void setCurrentStage(Stage stage) {
        this.currentStage = stage;
        player.setX(stage.getPlayerSpawnX());
        player.setY(stage.getPlayerSpawnY());
        player.setHasKey(false);
    public StageManager getStageManager() {
        return stageManager;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        stageManager.draw(g2d);

        player.draw(g2d);

        if (gameFinished) {
            g2d.setColor(java.awt.Color.WHITE);
            g2d.drawString("Game Clear!", 40, 40);
        }
    }
}
