package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.websocket.models.SubscribeMessage;
import org.websocket.util.PVcache;

import java.util.Arrays;


public class SubscriptionHandler {

    private final SessionHandler client;
    private final PVcache subCache;
    private final ObjectMapper mapper;


    public SubscriptionHandler(SessionHandler client, PVcache subCache, ObjectMapper mapper) {
        this.client = client;
        this.subCache = subCache;
        this.mapper = mapper;
    }

    public void subscribeCache() throws JsonProcessingException {
        SubscribeMessage message = new SubscribeMessage();
        message.setType("subscribe");
        message.setPvs(Arrays.asList(subCache.getCachedPVs()));
        String json = mapper.writeValueAsString(message);
        client.send(json);
    }

    public void subscribe(String[] pvs) throws JsonProcessingException {

       SubscribeMessage message = new SubscribeMessage();
       message.setType("subscribe");
       message.setPvs(Arrays.asList(pvs));
       String json = mapper.writeValueAsString(message);
       client.send(json);
       subCache.cachePVs(pvs);
    }

    public void unSubscribe(String[] pvs) throws JsonProcessingException {
        SubscribeMessage message = new SubscribeMessage();
        message.setType("clear");
        message.setPvs(Arrays.asList(pvs));
        String json = mapper.writeValueAsString(message);
        client.send(json);
        subCache.uncachePVs(pvs);
    }


}
