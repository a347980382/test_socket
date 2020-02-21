package com.*;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * socket 服务端监听连接池
 */
@Slf4j
public class ServerPoolListent extends Thread {

    @Override
    public void run() {
        while (true) {
            Map<String, SocketClient> socketMap = SocketPool.getOnlineSocketMap();

            // 每隔5秒 重复监听是否有客户与服务端建立连接
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 线程关闭之后，还需要监听 是否有新的客户端连接
            while (true) {
                if (socketMap.size() == 0 && !SocketServer.executorService.isShutdown()) {
                    SocketServer.executorService.shutdown();
                    log.info("初始化线程池，完成。");
                } else {
                    for(Map.Entry<String, SocketClient> entry : socketMap.entrySet()) {
                        SocketClient client = entry.getValue();
                        client.redirct();
                    }
                    if (SocketServer.executorService.isShutdown()) {
                        break;
                    }
                }
            }
        }
    }
}
