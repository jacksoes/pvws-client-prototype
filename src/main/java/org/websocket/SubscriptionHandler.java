package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.websocket.models.Message;
import org.websocket.util.PVcache;


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

        Message message = new Message("subscribe", subCache.getCachedPVs());
        String json = mapper.writeValueAsString(message);
        client.send(json);
    }

    public void subscribe(String[] pvs) throws JsonProcessingException {

        Message message = new Message("subscribe", pvs);
        String json = mapper.writeValueAsString(message);
        client.send(json);
        subCache.cachePVs(pvs);

    }

    public void unSubscribe(String[] pvs) throws JsonProcessingException {
        Message message = new Message("clear", pvs);
        String json = mapper.writeValueAsString(message);
        client.send(json);
        subCache.uncachePVs(pvs);
    }


}
