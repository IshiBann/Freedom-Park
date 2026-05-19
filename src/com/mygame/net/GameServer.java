package com.mygame.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import com.mygame.GamePanel;
import com.mygame.entity.Player;

public class GameServer extends Thread {

    private DatagramSocket socket;
    private GamePanel game;
    private List<ClientInfo> clients = new ArrayList<>();
    private int port = 9876;
    private volatile boolean running = true;
    private int lobbyStageIndex = 0;

    public GameServer(GamePanel game) {
        this.game = game;
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        System.out.println("Server started on port " + port);
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
            parsePacket(packet.getData(), packet.getAddress(), packet.getPort());
        }
    }

    public void stopServer() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public void setLobbyStageIndex(int lobbyStageIndex) {
        this.lobbyStageIndex = lobbyStageIndex;
        broadcastLobbyState();
    }

    private void parsePacket(byte[] data, InetAddress address, int port) {
        String message = new String(data).trim();
        String[] tokens = message.split(",");
        String type = tokens[0];

        if (type.equals("JOIN")) {
            handleJoin(address, port);
        } else if (type.equals("INP")) {
            handleInput(tokens);
        }
    }

    private void handleJoin(InetAddress address, int port) {
        if (clients.size() >= 3) return; // Host is 0, max 3 additional clients (total 4)

        int playerID = clients.size() + 1; // Clients get ID 1, 2, 3
        clients.add(new ClientInfo(address, port, playerID));
        
        // Add player to the server's authoritative game world (Host's screen)
        Player newPlayer = new Player(playerID, 
            game.getStageManager().getCurrentStage().getPlayerSpawnX(),
            game.getStageManager().getCurrentStage().getPlayerSpawnY());
        
        // Synchronize list modification
        synchronized(game.getPlayers()) {
            game.getPlayers().add(newPlayer);
        }
        game.setLobbyPlayerCount(game.getPlayers().size());
        
        System.out.println("Client joined: " + address.getHostAddress() + ":" + port + " as Player " + playerID);
        
        // Send confirmation to client
        sendData(("JOINED," + playerID).getBytes(), address, port);
        sendData(("LOBBY," + lobbyStageIndex + "," + (clients.size() + 1)).getBytes(), address, port);
    }

    private void handleInput(String[] tokens) {
        int id = Integer.parseInt(tokens[1]);
        boolean left = tokens[2].equals("1");
        boolean right = tokens[3].equals("1");
        boolean jump = tokens[4].equals("1");

        // Find player and apply input
        for (Player p : game.getPlayers()) {
            if (p.getPlayerID() == id) {
                p.setMovingLeft(left);
                p.setMovingRight(right);
                if (jump) p.jump();
                break;
            }
        }
    }

    public void broadcast(byte[] data) {
        for (ClientInfo c : clients) {
            sendData(data, c.address, c.port);
        }
    }

    public void broadcastStart(int stageIndex) {
        broadcast(("START," + stageIndex).getBytes());
    }

    private void broadcastLobbyState() {
        game.setLobbyPlayerCount(clients.size() + 1);
        broadcast(("LOBBY," + lobbyStageIndex + "," + (clients.size() + 1)).getBytes());
    }

    public void sendData(byte[] data, InetAddress address, int port) {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientInfo {
        InetAddress address;
        int port;
        int playerID;

        ClientInfo(InetAddress address, int port, int playerID) {
            this.address = address;
            this.port = port;
            this.playerID = playerID;
        }
    }
}
