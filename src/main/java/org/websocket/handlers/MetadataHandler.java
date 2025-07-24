package org.websocket.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.websocket.models.PV;
import org.websocket.models.PvMetaData;

import java.util.concurrent.ConcurrentHashMap;





public class MetadataHandler {
    public  final ConcurrentHashMap<String, PvMetaData> pvMetaMap;
    private final ConcurrentHashMap<String, Integer> subscribeAttempts;

    public MetadataHandler(ConcurrentHashMap<String, PvMetaData> pvMetaMap, ConcurrentHashMap<String, Integer> subscribeAttempts){
        this.pvMetaMap = pvMetaMap;
        this.subscribeAttempts = subscribeAttempts;

    }


    public void setData(PvMetaData pv) {
        pvMetaMap.putIfAbsent(pv.getPv(), pv);

    }

    public void refetch(int MAX_SUBSCRIBE_ATTEMPTS, PV pvObj, SessionHandler client) throws JsonProcessingException {
        int currentAttempts = subscribeAttempts.getOrDefault(pvObj.getPv(), 0);
        if (currentAttempts >= MAX_SUBSCRIBE_ATTEMPTS) {
            System.err.println("Max subscribe attempts reached for PV: " + pvObj.getPv());
            client.unSubscribeClient(new String[]{pvObj.getPv()});
            return;
        }

        System.out.println("Missed first message for: " + pvObj.getPv() + ": attempt " + (currentAttempts + 1));
        try {
            subscribeAttempts.put(pvObj.getPv(), currentAttempts + 1);
            client.unSubscribeClient(new String[]{pvObj.getPv()});
            Thread.sleep(100);
            client.subscribeClient(new String[]{pvObj.getPv()});

        }catch(Exception e) {
            System.err.println("Error unsubscribing or resubscribing PV: " + e.getMessage());
        }


    }

}
