package com.mygame;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.EventQueue;

public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JFrame window = new JFrame("Freedom Park - Walking Animation Demo");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(true);

            GamePanel gamePanel = new GamePanel();

            JPanel cards = new JPanel(new CardLayout());
            MenuScreen menu = new MenuScreen();

            // Stage selection action: switch to game and start the chosen level
            menu.setStageSelectedAction((levelIndex) -> {
                CardLayout cl = (CardLayout) (cards.getLayout());
                cl.show(cards, "game");
                gamePanel.startGameAtStage(levelIndex);
            });

            // Exit button action
            menu.setExitAction(() -> {
                window.dispose();
            });

            cards.add(menu, "menu");
            cards.add(gamePanel, "game");

            window.add(cards);

            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            // Ensure menu has keyboard focus so "Press any key" works
            menu.requestFocusInWindow();
        });
    }
}