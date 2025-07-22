package org.websocket.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.websocket.SessionHandler;
import org.websocket.models.PV;
import org.websocket.models.PvMetaData;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


// HAVE OBJECT CALLED PV META DATA, AND MAP IT WITH THE JSON PARSER
// AND NEW HASH MAP <sTRING, PVMETADATA.CLASS>


//How to make sure first message is not missed?
//  1. unsubscribe and resubscribe
// OR 2. multiple unsubscripe and resubscribe



public class MetaDataCache {
    public static final HashMap<String, PvMetaData> pvMetaMap = new HashMap<>();
    private static final ConcurrentHashMap<String, Integer> subscribeAttempts = new ConcurrentHashMap<>();


    public static void setData(PvMetaData pv) {
        pvMetaMap.putIfAbsent(pv.getPv(), pv);

    }

    public static void refetch(int MAX_SUBSCRIBE_ATTEMPTS, PV pvObj, SessionHandler client) throws JsonProcessingException {
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


    public static String tString(){
        return pvMetaMap.get("sim://noise").toString();

    }
    /*
    public static HashMap<String, Object> initializeMetaFields(PV pvObj) {
        //create hashmap with meta-data fields only sent on first pv update
        HashMap<String, Object> metaFieldsMap = new HashMap<>();
        metaFieldsMap.put("vtype", null);
        metaFieldsMap.put("units", null);
        metaFieldsMap.put("description", null);
        metaFieldsMap.put("precision", null);
        metaFieldsMap.put("min", null);
        metaFieldsMap.put("max", null);
        metaFieldsMap.put("warn_low", null);
        metaFieldsMap.put("warn_high", null);
        metaFieldsMap.put("alarm_low", null);
        metaFieldsMap.put("alarm_high", null);
        metaFieldsMap.put("severity", null);
        metaFieldsMap.put("readonly", null);

        // Loop through the Meta-data fields
        for (Field field : pvObj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(pvObj);
                Class<?> type = field.getType();

                //if not a meta-data field listed above do skip
                if(!metaFieldsMap.containsKey(field.getName())) {
                    continue;
                }



                if (value == null) {
                    metaFieldsMap.put(field.getName(), null);
                } else if (type == int.class && ((int) value) == 0) {
                    metaFieldsMap.put(field.getName(), null);
                } else if (type == double.class && ((double) value) == 0.0) {
                    metaFieldsMap.put(field.getName(), null);
                } else if (type == boolean.class && !((boolean) value)) {
                    metaFieldsMap.put(field.getName(), null);
                } else {
                    metaFieldsMap.put(field.getName(), value);
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return metaFieldsMap;

    }*/



}
