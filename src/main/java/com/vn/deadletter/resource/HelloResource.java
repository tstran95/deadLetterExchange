package com.vn.deadletter.resource;

import com.google.gson.Gson;
import com.vn.deadletter.constant.Constant;
import com.vn.deadletter.consumer.DirectExchangeConsumer;
import com.vn.deadletter.producer.DirectExchangeProducer;
import com.vn.deadletter.model.Product;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Objects;

@Path("/app")
public class HelloResource {
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public String publish(Product product) {
        if (Objects.isNull(product)) {
            return "Please fill all input";
        }
        Gson gson = new Gson();
        String prodString = gson.toJson(product);
        try {
            DirectExchangeProducer producer = new DirectExchangeProducer();
            producer.start();

            // Publish some message
            producer.send(Constant.DIRECT_EXCHANGE_NAME , prodString , Constant.ROUTING_KEY_DIRECT_NAME);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return prodString;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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