package org.websocket;

import org.websocket.models.PV;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class VtypeHash {

    //needs vtype, units, description, precision, min, max
    // warn_low, warn_high, alarm_low, alarm_high
    //severity???, readonly

    public static final HashMap<String, HashMap<String, Object>> map = new HashMap<>();

    //public static final String pvName = "", Map<String, Object> messageData


    public static void setData(PV pvObj) {
        map.putIfAbsent(pvObj.getPv(), initialize(pvObj));

    }

    public static HashMap<String, Object> initialize(PV pvObj) {
        HashMap<String, Object> innerMap = new HashMap<>();

        innerMap.put("vtype", null);
        innerMap.put("units", null);
        innerMap.put("description", null);
        innerMap.put("precision", null);
        innerMap.put("min", null);
        innerMap.put("max", null);
        innerMap.put("warn_low", null);
        innerMap.put("warn_high", null);
        innerMap.put("alarm_low", null);
        innerMap.put("alarm_high", null);
        innerMap.put("severity", null);
        innerMap.put("readonly", null);

        for (Field field : pvObj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(pvObj);
                Class<?> type = field.getType();

                if(!innerMap.containsKey(field.getName())) {
                    continue;
                }

                if (value == null) {
                    innerMap.put(field.getName(), null);
                } else if (type == int.class && ((int) value) == 0) {
                    innerMap.put(field.getName(), null);
                } else if (type == double.class && ((double) value) == 0.0) {
                    innerMap.put(field.getName(), null);
                } else if (type == boolean.class && !((boolean) value)) {
                    innerMap.put(field.getName(), null);
                } else {
                    innerMap.put(field.getName(), value);
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return innerMap;

    }



}
