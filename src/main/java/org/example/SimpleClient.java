package org.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import java.util.logging.Logger;
import java.util.logging.Level;

public class SimpleClient implements AutoCloseable{
    private final Socket client;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final Scanner scanner = new Scanner(System.in);
    private static final Logger logger = AppLogger.getLogger();
    private volatile boolean running = true;
    private final Thread readThread;

    public static void main (String[] args) {
        try (SimpleClient client = new SimpleClient(Config.getServerAddress(), Config.getPort())) {
            client.startCommunication();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Client application error:" + e.getMessage());
        }
    }

    private void readMessageFromServer() {
        try {
            while (true) {
                String serverMessage = input.readUTF();
                System.out.println(serverMessage);
            }
        } catch (IOException e) {
            if (running) {
                logger.log(Level.INFO, "Server closed connection or an error occurred during read: " + e.getMessage());
                System.out.println("\nDisconnect from server");
            } else {
                logger.log(Level.INFO, "Read thread stopped as client is shutting down.");
            }
        } finally {
            running = false;
        }
    }

    public SimpleClient (String address, int port) throws IOException{
        try {
            client = new Socket(address, port);
            logger.log(Level.INFO, "Connect to server: " + client.getRemoteSocketAddress());
            input = new DataInputStream(client.getInputStream());
            output = new DataOutputStream(client.getOutputStream());

            readThread = new Thread(this::readMessageFromServer);
            readThread.start();
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, "Unknown host: " + address, ex);
            throw ex;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error connecting to server: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    public void startCommunication() {
        System.out.println("Enter message to send (or 'exit'): ");
        while (running) {
            String messageToSend = scanner.nextLine();
            if (messageToSend.equalsIgnoreCase("exit")) {
                System.out.println("Exit chat...");
                running = false;
                break;
            }
            try {
                output.writeUTF(messageToSend);
                output.flush();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error sending message: " + e.getMessage());
                System.out.println("Failed to send message. Disconnecting.");
                running = false;
                break;
                }
        }


        // try correct finish work
        if (readThread != null && readThread.isAlive()) {
            try {
                readThread.join(1000);
                if (readThread.isAlive()) {
                    logger.log(Level.WARNING, "Read thread did not terminate gracefully, interrupting.");
                    readThread.interrupt();
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Interrupted while waiting for read thread to join.", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close(){
        running = false;

        if (output != null) {
            try { output.close(); } catch (IOException e) { logger.log(Level.WARNING, "Error closing output stream: " + e.getMessage(), e); }
        }
        if (input != null) {
            try { input.close(); } catch (IOException e) { logger.log(Level.WARNING, "Error closing input stream: " + e.getMessage(), e); }
        }
        if (client != null && !client.isClosed()) {
            try { client.close(); } catch (IOException e) { logger.log(Level.WARNING, "Error closing client socket: " + e.getMessage(), e); }
        }
        scanner.close();
        logger.log(Level.INFO, "Client finished work and resources closed.");
    }
}


