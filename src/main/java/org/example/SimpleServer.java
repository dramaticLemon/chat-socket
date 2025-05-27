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

    public static void main(String[] args) throws  IOException{
        ThreadPoolExecutor clientProcessingPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        try {
            ServerSocket server = new ServerSocket(Config.getPort());
            logger.log(Level.INFO, "Cервер запущен на порту " + Config.getPort());
            while (true) {
                Socket clientConnection = server.accept();
                logger.log(Level.INFO, "Клиент подключен: " + clientConnection.getRemoteSocketAddress());

                UserHandler userHandler = new UserHandler(clientConnection, activeUsers);
                activeUsers.add(userHandler);
                logger.log(Level.INFO, "Активные пользователи: " + activeUsers.size());
                clientProcessingPool.execute(userHandler);
            }
        } catch (IOException e) {
            // залогироваь ошибку запуска сервера
        } finally {
            System.out.println("Закрытие сервера и освобождение ресурсов...");
            clientProcessingPool.shutdown();

            try {
                if (!clientProcessingPool.awaitTermination(30, TimeUnit.SECONDS)); {
                    System.out.println("Пул потоков не завершился вовремя, принудительное завершение...");
                    clientProcessingPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                // залогировать ошибку ожидания завершеня пула потоков
            }
        }

        for (UserHandler user: activeUsers) {
            user.closeConnection();
        }

        System.out.println("Все соединения закрыты. Сервер остановлен.");
    }
}
