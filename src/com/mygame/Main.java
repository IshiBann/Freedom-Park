package com.mygame;

import java.awt.CardLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main {

    private enum LobbyMode { SINGLE_PLAYER, HOST, CLIENT }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JFrame window = new JFrame("Freedom Park");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(true);

            GamePanel gamePanel = new GamePanel();
            LobbyScreen lobby = new LobbyScreen(gamePanel);
            JPanel cards = new JPanel(new CardLayout());
            MenuScreen menu = new MenuScreen();

            final LobbyMode[] currentLobbyMode = {LobbyMode.SINGLE_PLAYER};

            menu.setSinglePlayerAction(() -> currentLobbyMode[0] = LobbyMode.SINGLE_PLAYER);
            menu.setHostGameAction(() -> {
                currentLobbyMode[0] = LobbyMode.HOST;
                menu.openStageSelect();
            });
            menu.setStageSelectedAction((levelIndex) -> {
                if (currentLobbyMode[0] == LobbyMode.HOST) {
                    gamePanel.initServer();
                }
                if (currentLobbyMode[0] == LobbyMode.SINGLE_PLAYER) {
                    gamePanel.startGameAtStage(levelIndex);
                    showGameCard(window, cards, gamePanel);
                    return;
                }
                gamePanel.setLobbyStageIndex(levelIndex);
                lobby.configure(
                    currentLobbyMode[0] == LobbyMode.CLIENT ? "LOBBY" : "READY ROOM",
                    currentLobbyMode[0] == LobbyMode.CLIENT ? "WAITING FOR HOST" : "SELECTED STAGE: " + (levelIndex + 1),
                    currentLobbyMode[0] == LobbyMode.CLIENT ? "ROOM: JOINED" : "ROOM: LOCAL",
                    levelIndex,
                    currentLobbyMode[0] != LobbyMode.CLIENT
                );
                showLobbyCard(window, cards, lobby);
            });
            menu.setJoinGameAction((serverIP) -> {
                currentLobbyMode[0] = LobbyMode.CLIENT;
                gamePanel.initClient(serverIP);
                lobby.configure("LOBBY", "WAITING FOR HOST", "ROOM: " + serverIP, gamePanel.getLobbyStageIndex(), false);
                showLobbyCard(window, cards, lobby);
            });
            menu.setExitAction(window::dispose);

            gamePanel.setHomeAction(() -> {
                gamePanel.returnToHome();
                showMenuCard(window, cards, menu);
            });
            gamePanel.setGameStartAction(() -> showGameCard(window, cards, gamePanel));

            lobby.setStartAction(() -> {
                gamePanel.startLobbyGame(lobby.getStageIndex());
            });
            lobby.setBackAction(() -> {
                gamePanel.returnToHome();
                showMenuCard(window, cards, menu);
            });

            cards.add(menu, "menu");
            cards.add(lobby, "lobby");
            cards.add(gamePanel, "game");
            window.add(cards);

            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            menu.requestFocusInWindow();
        });
    }

    private static void showGameCard(JFrame window, JPanel cards, GamePanel gamePanel) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, "game");
        window.revalidate();
        window.repaint();
        gamePanel.requestFocusInWindow();
    }

    private static void showLobbyCard(JFrame window, JPanel cards, LobbyScreen lobby) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, "lobby");
        window.revalidate();
        window.repaint();
        lobby.requestFocusInWindow();
    }

    private static void showMenuCard(JFrame window, JPanel cards, MenuScreen menu) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, "menu");
        window.revalidate();
        window.repaint();
        menu.requestFocusInWindow();
    }
}
