package com.biz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TestApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(TestApplication.class, args);
		// socket 随着springboot 启动，一起启动
        SocketServer server = context.getBeanFactory().getBean(SocketServer.class);
        server.start();
    }

}
