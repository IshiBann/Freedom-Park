package com.mygame.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import com.mygame.GamePanel;
import com.mygame.entity.Player;

public class GameClient extends Thread {

    private InetAddress serverAddress;
    private DatagramSocket socket;
    private GamePanel game;
    private int port = 9876;
    private int playerID = -1;
    private volatile boolean running = true;

    public GameClient(GamePanel game, String serverIP, int port) {
        this.game = game;
        this.port = port;
        try {
            this.socket = new DatagramSocket();
            this.serverAddress = InetAddress.getByName(serverIP);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        sendJoinRequest();

        while (running) {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                if (!running) {
                    break;
                }
                e.printStackTrace();
                continue;
            }
            parsePacket(packet.getData(), packet.getLength());
        }
    }

    public void stopClient() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private void parsePacket(byte[] data, int length) {
        String message = new String(data, 0, length, StandardCharsets.UTF_8).trim();
        String[] tokens = message.split(",");
        if (tokens.length < 1) return;
        String type = tokens[0];

        if (type.equals("JOINED")) {
            this.playerID = Integer.parseInt(tokens[1]);
            game.setLocalPlayerID(this.playerID);
            game.ensureLocalPlayer(this.playerID);
            System.out.println("Joined server as Player " + (this.playerID + 1));
        } else if (type.equals("LOBBY")) {
            int stageIndex = Integer.parseInt(tokens[1]);
            int playerCount = tokens.length > 2 ? Integer.parseInt(tokens[2]) : 1;
            game.setLobbyStageIndex(stageIndex);
            game.setLobbyPlayerCount(playerCount);
            if (tokens.length > 3) {
                int[] occupied = new int[tokens.length - 3];
                for (int i = 0; i < occupied.length; i++) {
                    occupied[i] = Integer.parseInt(tokens[i + 3]);
                }
                game.syncLobbyRoster(occupied);
            }
        } else if (type.equals("DENIED")) {
            String reason = tokens.length > 1 ? message.substring(message.indexOf(',') + 1) : "Join denied.";
            game.onJoinDenied(reason);
        } else if (type.equals("START")) {
            int stageIndex = Integer.parseInt(tokens[1]);
            game.startGameAtStage(stageIndex);
        } else if (type.equals("RESET")) {
            Player localPlayer = game.getLocalPlayer();
            if (localPlayer != null) {
                game.getStageManager().resetCurrentStage(localPlayer);
                // Reposition all other players to spawn
                for (Player p : game.getPlayers()) {
                    if (p.getPlayerID() != game.getLocalPlayerID()) {
                        p.setX(game.getStageManager().getCurrentStage().getSpawnXForPlayer(p.getPlayerID()));
                        p.setY(game.getStageManager().getCurrentStage().getSpawnYForPlayer(p.getPlayerID()));
                        p.setHasKey(false);
                        p.setWaitingAtExit(false);
                        p.stopMovement();
                    }
                }
            }
        } else if (type.equals("STATE")) {
            handleWorldState(tokens);
        } else if (type.equals("CHAT")) {
            int senderId = Integer.parseInt(tokens[1]);
            String msg = message.substring(message.indexOf(',', message.indexOf(',') + 1) + 1);
            game.onChatMessageReceived(senderId, msg);
        }
    }

    private void handleWorldState(String[] tokens) {
        // Format: STATE,StageIndex,P0x,P0y,P0ml,P0mr,P0ij, P1x,P1y,P1ml,P1mr,P1ij...
        int stageIndex = Integer.parseInt(tokens[1]);

        boolean stageChanged = game.getStageManager().getCurrentStageIndex() != stageIndex;
        if (stageChanged) {
            game.getStageManager().setCurrentStageIndex(stageIndex);
            synchronized (game.getPlayers()) {
                for (Player p : game.getPlayers()) {
                    p.setWaitingAtExit(false);
                }
            }
        }

        // Players: 7 tokens each (x, y, ml, mr, jump, hasKey, waitingAtExit)
        for (int i = 0; i < 4; i++) {
            int base = 2 + (i * 7);
            if (base + 6 >= tokens.length) {
                break;
            }
            int x = Integer.parseInt(tokens[base]);
            int y = Integer.parseInt(tokens[base + 1]);
            boolean ml = tokens[base + 2].equals("1");
            boolean mr = tokens[base + 3].equals("1");
            boolean ij = tokens[base + 4].equals("1");
            boolean hasKey = tokens[base + 5].equals("1");
            boolean waitingAtExit = tokens[base + 6].equals("1");

            if (x != 0 || y != 0 || waitingAtExit) {
                updateOrCreatePlayer(i, x, y, ml, mr, ij, hasKey, waitingAtExit);
            }
        }

        java.util.List<com.mygame.entity.Box> boxes = game.getStageManager().getCurrentStage().getBoxes();
        int boxTokenStart = 30;
        for (int i = 0; i < boxes.size(); i++) {
            int base = boxTokenStart + (i * 2);
            if (base + 1 >= tokens.length) break;

            int x = Integer.parseInt(tokens[base]);
            int y = Integer.parseInt(tokens[base+1]);

            com.mygame.entity.Box b = boxes.get(i);
            b.setX(x);
            b.setY(y);
        }

        // Key state (isUsed)
        com.mygame.entity.Key key = game.getStageManager().getCurrentStage().getKey();
        if (key != null) {
            int keyTokenIndex = 30 + (boxes.size() * 2);
            if (keyTokenIndex < tokens.length) {
                boolean keyUsed = tokens[keyTokenIndex].equals("1");

                int holder = -1;
                if (!keyUsed) {
                    synchronized (game.getPlayers()) {
                        for (Player p : game.getPlayers()) {
                            if (p.hasKey()) {
                                holder = p.getPlayerID();
                                break;
                            }
                        }
                    }
                }
                key.syncFromNetwork(holder, keyUsed);
            }
        }

        // Door state
        com.mygame.entity.Door door = game.getStageManager().getCurrentStage().getDoor();
        if (door != null) {
            int doorTokenIndex = 30 + (boxes.size() * 2) + 1;
            if (doorTokenIndex < tokens.length) {
                boolean doorUnlocked = tokens[doorTokenIndex].equals("1");
                door.syncFromNetwork(doorUnlocked);
            }
        }

        // Pressure plates
        java.util.List<com.mygame.entity.PressurePlate> plates = game.getStageManager().getCurrentStage().getPressurePlates();
        int plateTokenStart = 30 + (boxes.size() * 2) + 2;
        for (int i = 0; i < plates.size(); i++) {
            int tokenIndex = plateTokenStart + i;
            if (tokenIndex >= tokens.length) break;
            boolean isPressed = tokens[tokenIndex].equals("1");
            com.mygame.entity.PressurePlate plate = plates.get(i);
            plate.setPressedFromNetwork(isPressed);
        }
    }

    private void updateOrCreatePlayer(int id, int x, int y, boolean ml, boolean mr, boolean ij,
            boolean hasKey, boolean waitingAtExit) {
        int localId = game.getLocalPlayerID();
        synchronized(game.getPlayers()) {
            for (Player p : game.getPlayers()) {
                if (p.getPlayerID() != id) {
                    continue;
                }
                if (id == localId) {
                    p.setX(x);
                    p.setY(y);
                    p.setHasKey(hasKey);
                    p.setWaitingAtExit(waitingAtExit);
                    return;
                }
                int prevY = p.getY();
                p.setX(x);
                p.setY(y);
                p.setMovingLeft(ml);
                p.setMovingRight(mr);
                p.setHasKey(hasKey);
                p.setWaitingAtExit(waitingAtExit);
                if (ij) {
                    p.setIsJumping(true);
                } else if (y >= prevY) {
                    p.setIsJumping(false);
                }
                return;
            }

            if (id == localId) {
                game.ensureLocalPlayer(id);
                Player local = game.getLocalPlayer();
                if (local != null) {
                    local.setX(x);
                    local.setY(y);
                    local.setHasKey(hasKey);
                    local.setWaitingAtExit(waitingAtExit);
                }
            } else {
                Player newPlayer = new Player(id, x, y);
                newPlayer.setMovingLeft(ml);
                newPlayer.setMovingRight(mr);
                newPlayer.setIsJumping(ij);
                newPlayer.setHasKey(hasKey);
                newPlayer.setWaitingAtExit(waitingAtExit);
                game.getPlayers().add(newPlayer);
            }
        }
    }

    private void sendJoinRequest() {
        sendData("JOIN".getBytes(StandardCharsets.UTF_8));
    }

    public void sendInput(boolean left, boolean right, boolean jump) {
        if (playerID == -1) return;
        String msg = "INP," + playerID + "," + (left ? "1" : "0") + "," + (right ? "1" : "0") + "," + (jump ? "1" : "0");
        sendData(msg.getBytes(StandardCharsets.UTF_8));
    }

    public void sendData(byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendChatMessage(String msg) {
        if (playerID == -1) return;
        String data = "CHAT," + playerID + "," + msg;
        sendData(data.getBytes(StandardCharsets.UTF_8));
    }
}
