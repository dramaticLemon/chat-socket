package org.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

class UserHandler implements Runnable{
    private final Socket connectSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private final String userName;
    private final List<UserHandler> connectedUsers;
    private static final Logger logger = AppLogger.getLogger();

    /**
     * Constructor UserHandler
     *
     * @param connectionSocket socket connect client and  server app
     * @param connectedUsers safeThread users list, with connect to server
     */
    public UserHandler(Socket connectionSocket, List<UserHandler> connectedUsers) {
        this.connectSocket = connectionSocket;
        this.connectedUsers = connectedUsers;
        try {
            this.dis = new DataInputStream(connectionSocket.getInputStream());
            this.dos = new DataOutputStream(connectSocket.getOutputStream());
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
            closeConnection();
        }
        this.userName = "User_" + UUID.randomUUID();
    }

    // send message all connected users
    private void broadcastChatMessage(String message) {
        for (UserHandler user : connectedUsers) {
            if (user != this) {
                user.sendMessage(message);
            }
        }
    }

    private void broadcastServerMessage(String message) {
        for (UserHandler user : connectedUsers) {
            if (user != this) {
                user.sendMessage(message);
            }
        }
    }

    /**
     * Main logic client processing, to execute in separate thread.
     * Read message from client and send other connected client.
     */
    @Override
    public void run() {
        System.out.println(userName + " (" + connectSocket.getRemoteSocketAddress() + ") start processing.");
        try {
            while (true) {
                String clientMessage = dis.readUTF();
                System.out.println("Message from " + userName + ": " + clientMessage);

                if ("exit".equalsIgnoreCase(clientMessage.trim())) {
                    logger.log(Level.INFO,userName + " request exit.");
                    break;
                } else {
                    broadcastChatMessage(userName + ": " + clientMessage);
                }
            }
        } catch (EOFException e) {
            System.out.println(userName + " (" + connectSocket.getRemoteSocketAddress() + ") switch off (EOF).");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error reading from client" + userName + " (" + connectSocket.getRemoteSocketAddress() + "): " + e.getMessage());
        } finally {
            closeConnection();
            connectedUsers.remove(this);
            logger.log(Level.INFO, userName + " (" + connectSocket.getRemoteSocketAddress() + ") delete form the list active users. All: " + connectedUsers.size());
            broadcastServerMessage(userName + " leave chat.");
        }
    }

    /**
     * Send message to concrete user
     *
     * @param message
     */
    public void sendMessage(String message) {
        try {
            if(dos != null) {
                dos.writeUTF(message);
                dos.flush();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
            closeConnection();
        }
    }

    public void closeConnection() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (connectSocket != null && !connectSocket.isClosed()) connectSocket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING,"Connection error " + e.getMessage());
        }
    }
}
