package com.vn.deadletter;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DirectApp {
    public static void main(String[] args) throws IOException, TimeoutException {
        DirectExchangeProducer producer = new DirectExchangeProducer();
        producer.start();

        // Publish some message
        producer.send(Constant.DIRECT_EXCHANGE_NAME , "Hello World!!!!" , Constant.ROUTING_KEY_DIRECT_NAME);

        // Create consumers, queues and binding queues to Direct Exchange
        DirectExchangeConsumer consumer = new DirectExchangeConsumer();
        consumer.start();
        consumer.subscribe();
    }
}
