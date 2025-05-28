package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleServer {
    public static List<UserHandler> activeUsers = new CopyOnWriteArrayList<>();
    private static final Logger logger = AppLogger.getLogger();

    public static void main(String[] args) {
        ThreadPoolExecutor clientProcessingPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        try {
            ServerSocket server = new ServerSocket(Config.getPort());
            logger.log(Level.INFO, "Server start on port " + Config.getPort());
            while (true) {
                Socket clientConnection = server.accept();
                logger.log(Level.INFO, "Client connected: " + clientConnection.getRemoteSocketAddress());

                UserHandler userHandler = new UserHandler(clientConnection, activeUsers);
                activeUsers.add(userHandler);
                logger.log(Level.INFO, "Active users: " + activeUsers.size());
                clientProcessingPool.execute(userHandler);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        } finally {
            System.out.println("Close server and freeing up resources...");
            clientProcessingPool.shutdown();

            try {
                if (!clientProcessingPool.awaitTermination(30, TimeUnit.SECONDS)); {
                    System.out.println("Thread pool didn't finish on time, force termination...");
                    clientProcessingPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, e.getMessage());
            }
        }

        for (UserHandler user: activeUsers) {
            user.closeConnection();
        }

        logger.log(Level.INFO, "All connection close. Stopped Server.");
    }
}
