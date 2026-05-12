package com.mygame;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Main {
    public static void main(String[] args) {
        String[] options = {"Host Game (Server + Player)", "Join Game (Client)", "Single Player"};
        int selection = JOptionPane.showOptionDialog(
            null,
            "Select Game Mode",
            "Freedom Park Multiplayer",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[2]
        );

        if (selection == 0) {
            startServer();
        } else if (selection == 1) {
            String serverIP = JOptionPane.showInputDialog("Enter Server IP:", "localhost");
            if (serverIP != null && !serverIP.isEmpty()) {
                startClient(serverIP);
            }
        } else if (selection == 2) {
            startSinglePlayer();
        }
    }

    private static void startSinglePlayer() {
        JFrame window = new JFrame("Freedom Park - Single Player");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gamePanel.startGameThread();
    }

    private static void startServer() {
        JFrame window = new JFrame("Freedom Park - Server");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gamePanel.initServer(); // Start UDP Server
        gamePanel.startGameThread();
    }

    private static void startClient(String serverIP) {
        JFrame window = new JFrame("Freedom Park - Client");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gamePanel.initClient(serverIP); // Connect to UDP Server
        gamePanel.startGameThread();
    }
}