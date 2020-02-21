package com.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

@Slf4j
public class SocketClient extends Thread{

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private String key;
    private String message;
    private final static int TIME_OUT = 60; // 60秒的心跳
    private long startTime = System.currentTimeMillis();  // 初始化开始时间
    private long endTime = System.currentTimeMillis();   // 初始化结束时间

    private CommandConverter commandConverter;
    private String strCommand = "";
    private byte[] bytResult;

    private int status = 200; // 线程状态 -100 结束线程

    public Socket getSocket() {
        return socket;
    }
    public void setSocket(Socket socket) {
        this.socket = socket;
        commandConverter = new CommandConverter();
    }
    public DataInputStream getInputStream() {
        return inputStream;
    }
    public void setInputStream(DataInputStream inputStream) {
        this.inputStream = inputStream;
    }
    public DataOutputStream getOutputStream() {
        return outputStream;
    }
    public void setOutputStream(DataOutputStream outputStream) {
        this.outputStream = outputStream;
    }
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public long getStartTime() {
        return startTime;
    }
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    public long getEndTime() {
        return endTime;
    }
    public void setEndTime(long endTime) {

        this.endTime = endTime;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public void run() {

        while (true){
            // 接收数据，并处理
            try {
                bytResult = SocketHandler.receiveBytes(this);
                if (bytResult != null) {
                    strCommand = new String(bytResult, "utf-8");
                    String returnContent = commandConverter.verifyCommand(strCommand);
                    if (StringUtils.isEmpty(returnContent)) {
                        continue;
                    }
                    log.info("发送客户端数据：" + returnContent);
                    SocketHandler.sendMessage(this, returnContent);
                }

                this.setStartTime(System.currentTimeMillis());

            } catch (Exception e) {
                SocketHandler.close(this);
                System.out.println("The socket Get IO Buffer error");
                log.error("服务关闭，线程退出。");
                return;
            }

            // 线程状态-100 则停止线程
            if (status == -100) {
                log.info("线程运行结束。");
                return;
            }

            if (SocketHandler.isSocketClosed(this)){
                log.info("客户端已关闭,其Key值为：{}", this.getKey());
                //关闭对应的服务端资源
                SocketHandler.close(this);
                break;
            }
        }
    }

    public boolean redirct() {
        endTime = System.currentTimeMillis();
        if ((endTime - startTime) / 1000 > TIME_OUT) {
            log.info("客户端已关闭,其Key值为：{}", this.getKey());
            // 设置状态-100 结束线程
            status = -100;
            //关闭对应的服务端资源
            SocketHandler.close(this);
            return true;
        }
        return false;
    }
}
