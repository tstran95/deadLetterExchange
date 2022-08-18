package com.vn.deadletter.constant;

public class Constant {
    public static final int RETRY_DELAY = 3000;
    public static final String QUEUE_NAME = "mainQueue";
    public static final String DEAD_LETTER_EXCHANGE_NAME = "DeadLetterExchange";
    public static final String DEAD_LETTER_QUEUE_NAME = "retryQueue";
    public static final String DIRECT_EXCHANGE_NAME = "DefaultExchange";
    public static final String ROUTING_KEY_DIRECT_NAME = "main-routing";
    public static final String ROUTING_KEY_DEAD_LETTER_NAME = "retry-routing";
}
