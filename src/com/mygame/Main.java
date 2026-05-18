package com.mygame;

import java.awt.CardLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
        EventQueue.invokeLater(() -> {
            JFrame window = new JFrame("Freedom Park - Single Player");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(true);

            GamePanel gamePanel = new GamePanel();
            JPanel cards = new JPanel(new CardLayout());
            MenuScreen menu = new MenuScreen();

            menu.setStageSelectedAction((levelIndex) -> {
                CardLayout cl = (CardLayout) cards.getLayout();
                cl.show(cards, "game");
                gamePanel.startGameAtStage(levelIndex);
            });
            menu.setExitAction(window::dispose);

            cards.add(menu, "menu");
            cards.add(gamePanel, "game");
            window.add(cards);

            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            menu.requestFocusInWindow();
        });
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

        gamePanel.initServer();
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

        gamePanel.initClient(serverIP);
        gamePanel.startGameThread();
    }
}
