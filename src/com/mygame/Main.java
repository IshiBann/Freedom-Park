package com.mygame;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame("Walking Animation Demo");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        // Create the game logic panel and add it to the window
        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);

        // Size the window to fit the preferred size of the GamePanel
        window.pack();
        
        window.setLocationRelativeTo(null); // Centers the window on screen
        window.setVisible(true);

        // Start the game loop thread
        gamePanel.startGameThread();
    }
}