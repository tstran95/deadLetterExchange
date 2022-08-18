package com.vn.deadletter.resource;

import com.google.gson.Gson;
import com.vn.deadletter.constant.Constant;
import com.vn.deadletter.consumer.DirectExchangeConsumer;
import com.vn.deadletter.producer.DirectExchangeProducer;
import com.vn.deadletter.model.Product;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Objects;

@Path("/app")
@Slf4j
public class HelloResource {
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public String publish(Product product) {
        log.info("HelloResource method publish() START with request : {}" , product);
        if (Objects.isNull(product)) {
            log.error("HelloResource method publish() ERROR with message : Request is null");
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
            log.info("HelloResource method publish() ERROR with message : ", e);
            return null;
        }
        log.info("HelloResource method publish() END with response : {}" , prodString);
        return prodString;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public String subscribe() {
        log.info("HelloResource method subscribe() START");
        try {
            // Create consumers, queues and binding queues to Direct Exchange
            DirectExchangeConsumer consumer = new DirectExchangeConsumer();
            consumer.start();
            //Consume message
            consumer.subscribe();
        }catch (Exception e) {
            log.info("HelloResource method subscribe() ERROR with message : ", e);
            e.printStackTrace();
            return null;
        }
        log.info("HelloResource method subscribe() END");
        return "Subscribe done";
    }
    
}