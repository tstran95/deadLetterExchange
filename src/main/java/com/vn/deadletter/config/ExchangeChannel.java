package com.vn.deadletter.config;

import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.vn.deadletter.constant.Constant;
import com.vn.deadletter.model.Product;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ExchangeChannel {
    public Channel channel;
    private Connection connection;

    public ExchangeChannel(Connection connection) throws IOException {
        this.connection = connection;
        this.channel = connection.createChannel();
    }

    public void declareExchange(BuiltinExchangeType exchangeType, String ...exchangeNames) throws IOException {
        for (String exchangeName: exchangeNames) {
            // exchangeDeclare( exchange, builtinExchangeType, durable)
            channel.exchangeDeclare(exchangeName, exchangeType, true);
        }
    }


    public void declareQueues(String ...queueNames) throws IOException {
        for (String queueName : queueNames) {
            // queueDeclare  - (queueName, durable, exclusive, autoDelete, arguments)
            channel.queueDeclare(queueName, true, false, false, null);
        }
    }

    public void declareQueuesWithTTLAndDLT(String ...queueNames) throws IOException {
        for (String queueName : queueNames) {
            //create the queue main with TTL and dead letter
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("x-dead-letter-exchange", "DeadLetterExchange");
            arguments.put("x-dead-letter-routing-key", "retry-routing");
            arguments.put("x-message-ttl", Constant.RETRY_DELAY);
            channel.queueDeclare(queueName, true, false, false, arguments);
        }
    }

    public void declareQueuesDeadLetter(String ...queueNames) throws IOException {
        for (String queueName : queueNames) {
            // create the dead letter queue
            Map<String, Object> arg = new HashMap<String, Object>();
            arg.put("x-delayed-type", "direct");
            channel.queueDeclare("retryQueue", true, false, false, arg);
        }
    }

    public void declareQueuesWithTTL(String ...queueNames) throws IOException {
        for (String queueName : queueNames) {
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("x-message-ttl", 6_000);
            // queueDeclare  - (queueName, durable, exclusive, autoDelete, arguments)
            channel.queueDeclare(queueName, true, false, false, args);
        }
    }

    public void performQueueBinding(String exchangeName, String queueName, String routingKey) throws IOException {
        // Create bindings - (queue, exchange, routingKey)
        channel.queueBind(queueName, exchangeName, routingKey);
    }

    public void subscribeMessage(String queueName) throws IOException {
        Gson gson = new Gson();
        // basicConsume - ( queue, autoAck, deliverCallback, cancelCallback)
        channel.basicConsume(queueName, true, ((consumerTag, message) -> {
            Product product = gson.fromJson(new String(message.getBody()) , Product.class);
            System.out.println("[" + LocalDateTime.now() + "] [Received] [" + queueName + "]: " + product.toString());
            System.out.println("[Received] [" + queueName + "]: " + consumerTag + " with message: " + product.toString());
        }), System.out::println);
    }

    public void publishMessage(String exchangeName, String message, String routingKey) throws IOException {
        System.out.println("[" + LocalDateTime.now() + "] [Send] [" + exchangeName + "]: " + message);
        // basicPublish - ( exchange, routingKey, basicProperties, body)
        System.out.println("[Send] [" + routingKey + "]: " + message);
        channel.basicPublish(exchangeName, routingKey, null, message.getBytes());
    }

//    public void close() throws IOException, TimeoutException {
//        connection.close();
//        channel.close();
//    }
}
