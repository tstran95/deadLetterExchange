package com.vn.deadletter;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DirectExchangeConsumer {
    private ExchangeChannel channel;

    public void start() throws IOException, TimeoutException {
        // Create connection
        Connection connection = ConnectionManager.createConnection();

        // Create channel
        channel = new ExchangeChannel(connection);

        // Create topic exchange
        channel.declareExchange(BuiltinExchangeType.DIRECT , Constant.DEAD_LETTER_EXCHANGE_NAME);

        // Create queues
        channel.declareQueuesDeadLetter(Constant.DEAD_LETTER_QUEUE_NAME);

        // Binding queues
        channel.performQueueBinding(Constant.DEAD_LETTER_EXCHANGE_NAME,
                Constant.DEAD_LETTER_QUEUE_NAME,
                Constant.ROUTING_KEY_DEAD_LETTER_NAME);
    }

    public void subscribe() throws IOException {
        // Subscribe message
        channel.subscribeMessage(Constant.DEAD_LETTER_QUEUE_NAME);
    }

//    public void close() throws IOException, TimeoutException {
//        channel.close();
//    }
}
