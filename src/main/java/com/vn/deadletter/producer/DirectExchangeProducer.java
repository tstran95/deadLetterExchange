package com.vn.deadletter.producer;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;
import com.vn.deadletter.config.ConnectionManager;
import com.vn.deadletter.constant.Constant;
import com.vn.deadletter.config.ExchangeChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class DirectExchangeProducer {
    private ExchangeChannel channel;
    private Connection connection;

    public void start() throws IOException, TimeoutException {
        log.info("DirectExchangeProducer method start() START");
        // Create connection
        connection = ConnectionManager.createConnection();

        // Create channel
        channel = new ExchangeChannel(connection);

        // Create topic exchange
        channel.declareExchange(BuiltinExchangeType.DIRECT , Constant.DIRECT_EXCHANGE_NAME , Constant.DEAD_LETTER_EXCHANGE_NAME);
        // Create queues
        channel.declareQueuesWithTTLAndDLT(Constant.QUEUE_NAME);
        channel.declareQueues(Constant.DEAD_LETTER_QUEUE_NAME);

        // Binding queues
        channel.performQueueBinding(Constant.DIRECT_EXCHANGE_NAME,
                                    Constant.QUEUE_NAME,
                                    Constant.ROUTING_KEY_DIRECT_NAME);
        channel.performQueueBinding(Constant.DEAD_LETTER_EXCHANGE_NAME,
                                    Constant.DEAD_LETTER_QUEUE_NAME,
                                    Constant.ROUTING_KEY_DEAD_LETTER_NAME);
        log.info("DirectExchangeProducer method start() END");
    }

    public void send(String exchangeName, String message, String messageKey) throws IOException, TimeoutException {
        log.info("DirectExchangeProducer method send() START");
        // Send message
        channel.publishMessage(exchangeName, message, messageKey);
        channel.close();
        connection.close();
        log.info("DirectExchangeProducer method send() END");
    }
}
