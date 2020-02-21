package com.*;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * socket 线程池
 */
@Component
public class SocketPool{

    private final static Map<String, SocketClient> ONLINE_SOCKET_MAP = new ConcurrentHashMap<>();
    public static Map<String, SocketClient> getOnlineSocketMap() {
        return ONLINE_SOCKET_MAP;
    }

    public static void add(SocketClient clientSocket){
        if (clientSocket != null && !clientSocket.getKey().isEmpty())
            ONLINE_SOCKET_MAP.put(clientSocket.getKey(), clientSocket);
    }

    public static void remove(String key){
        if (!key.isEmpty()) {
            ONLINE_SOCKET_MAP.remove(key);
        }
    }
}
