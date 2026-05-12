package com.mygame.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.mygame.GamePanel;
import com.mygame.entity.Player;

public class GameClient extends Thread {

    private InetAddress serverAddress;
    private DatagramSocket socket;
    private GamePanel game;
    private int port = 9876;
    private int playerID = -1;

    public GameClient(GamePanel game, String serverIP) {
        this.game = game;
        try {
            this.socket = new DatagramSocket();
            this.serverAddress = InetAddress.getByName(serverIP);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        sendJoinRequest();

        while (true) {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parsePacket(packet.getData());
        }
    }

    private void parsePacket(byte[] data) {
        String message = new String(data).trim();
        String[] tokens = message.split(",");
        String type = tokens[0];

        if (type.equals("JOINED")) {
            this.playerID = Integer.parseInt(tokens[1]);
            game.setLocalPlayerID(this.playerID);
            System.out.println("Joined server as Player " + this.playerID);
        } else if (type.equals("STATE")) {
            handleWorldState(tokens);
        }
    }

    private void handleWorldState(String[] tokens) {
        // Format: STATE,StageIndex,P0x,P0y,P0ml,P0mr,P0ij, P1x,P1y,P1ml,P1mr,P1ij...
        int stageIndex = Integer.parseInt(tokens[1]);
        
        // Handle stage sync
        if (game.getStageManager().getCurrentStageIndex() != stageIndex) {
            game.getStageManager().setCurrentStageIndex(stageIndex);
        }

        // Update players (index 2-21 are player coordinates and states: 4 players * 5 tokens)
        for (int i = 0; i < 4; i++) {
            int base = 2 + (i * 5);
            int x = Integer.parseInt(tokens[base]);
            int y = Integer.parseInt(tokens[base+1]);
            boolean ml = tokens[base+2].equals("1");
            boolean mr = tokens[base+3].equals("1");
            boolean ij = tokens[base+4].equals("1");

            if (x != 0 || y != 0) {
                updateOrCreatePlayer(i, x, y, ml, mr, ij);
            }
        }

        // Update boxes (index 22 onwards)
        java.util.List<com.mygame.entity.Box> boxes = game.getStageManager().getCurrentStage().getBoxes();
        int boxTokenStart = 22;
        for (int i = 0; i < boxes.size(); i++) {
            int base = boxTokenStart + (i * 2);
            if (base + 1 >= tokens.length) break;

            int x = Integer.parseInt(tokens[base]);
            int y = Integer.parseInt(tokens[base+1]);

            com.mygame.entity.Box b = boxes.get(i);
            b.setX(x);
            b.setY(y);
        }
    }

    private void updateOrCreatePlayer(int id, int x, int y, boolean ml, boolean mr, boolean ij) {
        boolean found = false;
        synchronized(game.getPlayers()) {
            for (Player p : game.getPlayers()) {
                if (p.getPlayerID() == id) {
                    p.setX(x);
                    p.setY(y);
                    p.setMovingLeft(ml);
                    p.setMovingRight(mr);
                    p.setIsJumping(ij);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Player newPlayer = new Player(id, x, y);
                newPlayer.setMovingLeft(ml);
                newPlayer.setMovingRight(mr);
                newPlayer.setIsJumping(ij);
                game.getPlayers().add(newPlayer);
            }
        }
    }

    private void sendJoinRequest() {
        sendData("JOIN".getBytes());
    }

    public void sendInput(boolean left, boolean right, boolean jump) {
        if (playerID == -1) return;
        String msg = "INP," + playerID + "," + (left ? "1" : "0") + "," + (right ? "1" : "0") + "," + (jump ? "1" : "0");
        sendData(msg.getBytes());
    }

    public void sendData(byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
