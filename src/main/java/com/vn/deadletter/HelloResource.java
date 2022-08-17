package com.vn.deadletter;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/app")
public class HelloResource {
    @GET
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public String publish() {
        String message = "Hello World";
        try {
            DirectExchangeProducer producer = new DirectExchangeProducer();
            producer.start();

            // Publish some message
            producer.send(Constant.DIRECT_EXCHANGE_NAME , "Hello World!!!!" , Constant.ROUTING_KEY_DIRECT_NAME);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    @POST
    public String subscribe() {
        try {
            // Create consumers, queues and binding queues to Direct Exchange
            DirectExchangeConsumer consumer = new DirectExchangeConsumer();
            consumer.start();
            consumer.subscribe();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return "Subscribe done";
    }
    
}