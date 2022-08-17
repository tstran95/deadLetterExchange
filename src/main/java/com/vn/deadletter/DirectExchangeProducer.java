package com.vn.deadletter;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DirectExchangeProducer {
    private ExchangeChannel channel;

    private Connection connection;

    public void start() throws IOException, TimeoutException {
        // Create connection
        connection = ConnectionManager.createConnection();

        // Create channel
        channel = new ExchangeChannel(connection);

        // Create topic exchange
        channel.declareExchange(BuiltinExchangeType.DIRECT , Constant.DIRECT_EXCHANGE_NAME , Constant.DEAD_LETTER_EXCHANGE_NAME);
        // Create queues
        channel.declareQueuesWithTTLAndDLT(Constant.QUEUE_NAME);
        channel.declareQueuesDeadLetter(Constant.DEAD_LETTER_QUEUE_NAME);

        // Binding queues
        channel.performQueueBinding(Constant.DIRECT_EXCHANGE_NAME,
                                    Constant.QUEUE_NAME,
                                    Constant.ROUTING_KEY_DIRECT_NAME);
        channel.performQueueBinding(Constant.DEAD_LETTER_EXCHANGE_NAME,
                                    Constant.DEAD_LETTER_QUEUE_NAME,
                                    Constant.ROUTING_KEY_DEAD_LETTER_NAME);
    }

    public void send(String exchangeName, String message, String messageKey) throws IOException, TimeoutException {
        // Send message
        channel.publishMessage(exchangeName, message, messageKey);
    }

    public void close() throws IOException, TimeoutException {
        connection.close();
    }
}
