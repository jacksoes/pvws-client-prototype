package org.websocket;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PVcache {

    private final Set<String> cache = new HashSet<>();


    public void cachePVs(String[] pvs) {
        // add all pvs to cache set;
        Collections.addAll(cache, pvs);
    }

    public void uncachePVs(String[] pvs) {

        for (String pv: pvs) {
            if (cache.contains(pv)){
                cache.remove(pv);
                System.out.println("removed PV from cache: " + pv);

            }
        }
    }

    public String[] getCachedPVs() {
        //return cache.toArray(new String[cache.size()]);
        return cache.toArray(new String[0]);  // preferred modern style
    }
}
