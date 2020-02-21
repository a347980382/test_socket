package com.*;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Data
@Component
@PropertySource(value = "classpath:socket.properties")
@NoArgsConstructor
public class SocketServer{

    @Value("${socket.port}")
    private int port;

    private boolean started; // 是否启动
    private ServerSocket serverSocket;
    public static ExecutorService executorService = Executors.newCachedThreadPool();

    public void start() {

        // 监听线程池中的socket线程
        ServerPoolListent poolListent = new ServerPoolListent();
        executorService.submit(poolListent);

        try {
            serverSocket = new ServerSocket(port);
            started = true;
            log.info("Socket服务已启动，占用端口： {}", serverSocket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("服务启动异常,异常信息：{}", e);
            System.exit(0);
        }

        try {
            while (started) {
                Socket accept = serverSocket.accept();
                accept.setKeepAlive(true);
                SocketClient client = SocketHandler.register(accept);
                log.info("客户端已连接，其Key值为：{}", client.getKey());
                client.start();

                if (client != null){
                    if (executorService.isShutdown()) {
                        executorService = Executors.newCachedThreadPool();
                    }
                    executorService.submit(client);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            log.info("服务异常关闭，错误信息", e);
            try {
                serverSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
