package org.example;

public class Config {
    private static final int PORT = 5050;
    private static final String SERVER_ADDRESS = "localhost";

    public static int getPort() {
        return PORT;
    }
    public static String getServerAddress() {
        return SERVER_ADDRESS;
    }

}
