package com.vn.deadletter.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.vn.deadletter.constant.Constant;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
@Slf4j
public class ConnectionManager {
    private ConnectionManager() {
        super();
    }

    public static Connection createConnection() throws IOException, TimeoutException {
        log.info("ConnectionManager method createConnection() START");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(Constant.HOST);
        log.info("ConnectionManager method createConnection() END");
        return factory.newConnection();
    }
}
