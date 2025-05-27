package org.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import java.util.logging.Logger;
import java.util.logging.Level;

public class SimpleClient {
    private static final Logger logger = AppLogger.getLogger();

    public static void main(String[] args) {
        try {
            DataInputStream in;
            DataOutputStream out;
            Scanner scanner;
            try (Socket client = new Socket(Config.getServerAddress(), Config.getPort())) {
                in = new DataInputStream(client.getInputStream());
                out = new DataOutputStream(client.getOutputStream());
                scanner = new Scanner(System.in);
                logger.log(Level.INFO, "Connect to server: " + client.getRemoteSocketAddress());
            }
            Thread readThread = new Thread(() -> {
                    try {
                        while (true) {
                            String serverMessage = in.readUTF();
                            System.out.println(serverMessage);
                        }
                    } catch (IOException e) {
                        logger.log(Level.INFO, "Server close connection.");
                        System.out.println();
                    }
            });
            System.out.println("Entre message for send (or 'exit'): ");
            while (true) {
                String messageToSend = scanner.nextLine();
                if (messageToSend.equalsIgnoreCase("exit")) {
                    System.out.println("Exit chat...");
                    break;
                }
                out.writeUTF(messageToSend);
                out.flush();
            }
            readThread.join(5000);
            if (readThread.isAlive()) {
                logger.log(Level.WARNING, "Read Thread not completed, forced closed");
                readThread.interrupt();

            }
        } catch (InterruptedException | IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        } finally {
            logger.log(Level.INFO, "Client finished work" );
        }
    }
}

