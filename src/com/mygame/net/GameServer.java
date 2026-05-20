package com.mygame.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.mygame.GamePanel;
import com.mygame.entity.Player;

public class GameServer extends Thread {

    private DatagramSocket socket;
    private GamePanel game;
    private List<ClientInfo> clients = new ArrayList<>();
    private int port = 9876;
    private volatile boolean running = true;
    private int lobbyStageIndex = 0;
    private final ConcurrentHashMap<Integer, InputSnapshot> latestInputs = new ConcurrentHashMap<>();

    private static final class InputSnapshot {
        volatile boolean left;
        volatile boolean right;
        volatile boolean jump;
    }

    public GameServer(GamePanel game) {
        this.game = game;
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Port " + port + " unavailable, attempting ephemeral port.");
            try {
                this.socket = new DatagramSocket(0);
                this.port = this.socket.getLocalPort();
                System.out.println("Server bound to ephemeral port " + this.port);
            } catch (SocketException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean isBound() {
        return socket != null && !socket.isClosed();
    }

    public void run() {
        if (socket == null) {
            System.err.println("Server socket not available; aborting server thread.");
            return;
        }
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
            parsePacket(packet.getData(), packet.getLength(), packet.getAddress(), packet.getPort());
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

    private void parsePacket(byte[] data, int length, InetAddress address, int port) {
        String message = new String(data, 0, length, StandardCharsets.UTF_8).trim();
        String[] tokens = message.split(",");
        if (tokens.length < 1) return;
        String type = tokens[0];

        if (type.equals("JOIN")) {
            handleJoin(address, port);
        } else if (type.equals("INP")) {
            handleInput(tokens);
        } else if (type.equals("CHAT")) {
            handleChat(tokens, message);
        }
    }

    private void handleJoin(InetAddress address, int port) {
        if (!game.isAllowLocalJoin() && isSameMachine(address)) {
            sendData("DENIED,Cannot join your own game from this computer.".getBytes(), address, port);
            return;
        }

        for (ClientInfo existing : clients) {
            if (existing.address.equals(address) && existing.port == port) {
                sendData(("JOINED," + existing.playerID).getBytes(), address, port);
                sendLobbyState(address, port);
                return;
            }
        }

        if (clients.size() >= 3) return; // Host is 0, max 3 additional clients (total 4)

        int playerID = clients.size() + 1; // Clients get ID 1, 2, 3 (display: Player 2–4)
        clients.add(new ClientInfo(address, port, playerID));
        
        // Add player to the server's authoritative game world (Host's screen)
        var stage = game.getStageManager().getCurrentStage();
        Player newPlayer = new Player(playerID,
            stage.getSpawnXForPlayer(playerID),
            stage.getSpawnYForPlayer(playerID));
        
        // Synchronize list modification
        synchronized(game.getPlayers()) {
            game.getPlayers().add(newPlayer);
        }
        game.setLobbyPlayerCount(game.getPlayers().size());
        
        System.out.println("Client joined: " + address.getHostAddress() + ":" + port + " as Player " + playerID);
        
        sendData(("JOINED," + playerID).getBytes(), address, port);
        broadcastLobbyState();
    }

    private void handleInput(String[] tokens) {
        int id = Integer.parseInt(tokens[1]);
        InputSnapshot snap = latestInputs.computeIfAbsent(id, k -> new InputSnapshot());
        snap.left = tokens[2].equals("1");
        snap.right = tokens[3].equals("1");
        if (tokens[4].equals("1")) {
            snap.jump = true;
        }
    }

    private void handleChat(String[] tokens, String fullMessage) {
        if (tokens.length < 3) return;
        
        int senderId = Integer.parseInt(tokens[1]);
        // Extract message while preserving commas
        String msg = fullMessage.substring(fullMessage.indexOf(',', fullMessage.indexOf(',') + 1) + 1);
        
        System.out.println("Chat received from Player " + (senderId + 1) + ": " + msg);

        // Broadcast the entire CHAT message to all clients
        String broadcastData = "CHAT," + senderId + "," + msg;
        broadcast(broadcastData.getBytes(StandardCharsets.UTF_8));
        
        // Notify local GamePanel (Host)
        game.onChatMessageReceived(senderId, msg);
    }

    /** Apply latest client inputs on the game thread before physics (host is id 0, keyboard only). */
    public void applyNetworkInputs() {
        synchronized (game.getPlayers()) {
            for (Player p : game.getPlayers()) {
                int id = p.getPlayerID();
                if (id == 0 || p.isWaitingAtExit()) {
                    continue;
                }
                InputSnapshot snap = latestInputs.get(id);
                if (snap != null) {
                    p.setMovingLeft(snap.left);
                    p.setMovingRight(snap.right);
                    if (snap.jump) {
                        p.jump();
                        snap.jump = false;
                    }
                } else {
                    p.setMovingLeft(false);
                    p.setMovingRight(false);
                }
            }
        }
    }

    public void broadcast(byte[] data) {
        for (ClientInfo c : clients) {
            sendData(data, c.address, c.port);
        }
    }

    public void broadcastStart(int stageIndex) {
        broadcast(("START," + stageIndex).getBytes(StandardCharsets.UTF_8));
    }

    public void broadcastReset() {
        broadcast(("RESET").getBytes(StandardCharsets.UTF_8));
    }

    private byte[] buildLobbyPacket() {
        synchronized (game.getPlayers()) {
            game.setLobbyPlayerCount(game.getPlayers().size());
            StringBuilder msg = new StringBuilder();
            msg.append("LOBBY,").append(lobbyStageIndex).append(",").append(game.getPlayers().size());
            for (Player p : game.getPlayers()) {
                msg.append(",").append(p.getPlayerID());
            }
            return msg.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    private void sendLobbyState(InetAddress address, int port) {
        sendData(buildLobbyPacket(), address, port);
    }

    private void broadcastLobbyState() {
        byte[] packet = buildLobbyPacket();
        broadcast(packet);
    }

    private boolean isSameMachine(InetAddress clientAddr) {
        if (clientAddr.isLoopbackAddress()) {
            return true;
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.equals(clientAddr)) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return false;
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
