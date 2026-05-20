package com.mygame;

import java.awt.CardLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Main {

    private enum LobbyMode { SINGLE_PLAYER, HOST, CLIENT }

    public static void main(String[] args) {
        final String[] startupArgs = filterAllowLocalArg(args);
        GamePanel.setAllowLocalJoin(hasAllowLocalArg(args));
        EventQueue.invokeLater(() -> {
            JFrame window = new JFrame("Freedom Park");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(true);

            GamePanel gamePanel = new GamePanel();
            LobbyScreen lobby = new LobbyScreen(gamePanel);
            JPanel cards = new JPanel(new CardLayout());
            MenuScreen menu = new MenuScreen();
            if (GamePanel.isAllowLocalJoin()) {
                menu.setLocalTestMode(true);
            }

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
                if (!gamePanel.tryInitClient(serverIP)) {
                    JOptionPane.showMessageDialog(
                        window,
                        gamePanel.getLastJoinError(),
                        "Cannot Join",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                currentLobbyMode[0] = LobbyMode.CLIENT;
                lobby.configure("LOBBY", "WAITING FOR HOST", "ROOM: " + serverIP, gamePanel.getLobbyStageIndex(), false);
                showLobbyCard(window, cards, lobby);
            });
            gamePanel.setJoinDeniedAction(() -> {
                currentLobbyMode[0] = LobbyMode.SINGLE_PLAYER;
                JOptionPane.showMessageDialog(
                    window,
                    gamePanel.getLastJoinError(),
                    "Cannot Join",
                    JOptionPane.WARNING_MESSAGE
                );
                showMenuCard(window, cards, menu);
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
            // Handle optional command-line startup modes:
            if (startupArgs != null && startupArgs.length > 0) {
                String mode = startupArgs[0].toLowerCase();
                try {
                    if (mode.equals("single") || mode.equals("singleplayer")) {
                        int stage = 0;
                        if (startupArgs.length > 1) {
                            stage = Integer.parseInt(startupArgs[1]);
                        }
                        gamePanel.startGameAtStage(stage);
                        showGameCard(window, cards, gamePanel);
                    } else if (mode.equals("host")) {
                        if (startupArgs.length > 1) {
                            int stage = Integer.parseInt(startupArgs[1]);
                            currentLobbyMode[0] = LobbyMode.HOST;
                            gamePanel.initServer();
                            gamePanel.startLobbyGame(stage);
                            showGameCard(window, cards, gamePanel);
                        } else {
                            currentLobbyMode[0] = LobbyMode.HOST;
                            menu.openStageSelect();
                            showMenuCard(window, cards, menu);
                        }
                    } else if (mode.equals("join") || mode.equals("client")) {
                        if (startupArgs.length > 1) {
                            String ip = startupArgs[1];
                            currentLobbyMode[0] = LobbyMode.CLIENT;
                            if (!gamePanel.tryInitClient(ip)) {
                                JOptionPane.showMessageDialog(
                                    window,
                                    gamePanel.getLastJoinError(),
                                    "Cannot Join",
                                    JOptionPane.WARNING_MESSAGE
                                );
                            } else {
                                lobby.configure("LOBBY", "WAITING FOR HOST", "ROOM: " + ip, gamePanel.getLobbyStageIndex(), false);
                                showLobbyCard(window, cards, lobby);
                            }
                        } else {
                            // No IP specified: show multiplayer menu so user can input
                            menu.showModeSelect();
                            showMenuCard(window, cards, menu);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
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

    private static boolean hasAllowLocalArg(String[] args) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (arg != null && arg.equalsIgnoreCase("allowlocal")) {
                return true;
            }
        }
        return false;
    }

    private static String[] filterAllowLocalArg(String[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        int kept = 0;
        for (String arg : args) {
            if (arg != null && !arg.equalsIgnoreCase("allowlocal")) {
                kept++;
            }
        }
        if (kept == args.length) {
            return args;
        }
        String[] filtered = new String[kept];
        int i = 0;
        for (String arg : args) {
            if (arg != null && !arg.equalsIgnoreCase("allowlocal")) {
                filtered[i++] = arg;
            }
        }
        return filtered;
    }

    private static void showMenuCard(JFrame window, JPanel cards, MenuScreen menu) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, "menu");
        window.revalidate();
        window.repaint();
        menu.requestFocusInWindow();
    }
}
