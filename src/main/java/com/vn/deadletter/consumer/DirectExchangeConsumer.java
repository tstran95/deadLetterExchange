package com.vn.deadletter.consumer;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;
import com.vn.deadletter.config.ConnectionManager;
import com.vn.deadletter.constant.Constant;
import com.vn.deadletter.config.ExchangeChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
@Slf4j
public class DirectExchangeConsumer {
    private ExchangeChannel channel;
    private Connection connection;

    public void start() throws IOException, TimeoutException {
        log.info("DirectExchangeConsumer method start() START");
        // Create connection
        connection = ConnectionManager.createConnection();

        // Create channel
        channel = new ExchangeChannel(connection);

        // Create topic exchange
        channel.declareExchange(BuiltinExchangeType.DIRECT , Constant.DEAD_LETTER_EXCHANGE_NAME);

        // Create queues
        channel.declareQueues(Constant.DEAD_LETTER_QUEUE_NAME);

        // Binding queues
        channel.performQueueBinding(Constant.DEAD_LETTER_EXCHANGE_NAME,
                Constant.DEAD_LETTER_QUEUE_NAME,
                Constant.ROUTING_KEY_DEAD_LETTER_NAME);
        log.info("DirectExchangeConsumer method start() END");
    }

    public void subscribe() throws IOException, TimeoutException {
        log.info("DirectExchangeConsumer method subscribe() START");
        // Subscribe message
        channel.subscribeMessage(Constant.DEAD_LETTER_QUEUE_NAME);
        channel.close();
        connection.close();
        log.info("DirectExchangeConsumer method subscribe() END");
    }
}
