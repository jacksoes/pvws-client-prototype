package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.websocket.util.PVcache;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;



// PIPELINE: 1. RECEIVED MESSAGE, 2. MAP TO OBJECT AND META DATA, 3. CREATE VTYPE
// FIND WAY SO FIRST MESSAGE IS ALWAYS THE ONE WITH MET DATA

// potentional solution: THROW AWAY MESSAGES TILL ONE IS GOTTEN WITH METADATA
//1, GET UPDATE 2. CHECK IT HAS META DATA TYPE 3. POPULATE CACHE. 4. IF CACHE IS NOT POPULATED THEN THROW AWAY MESSAGE
public class App {
    public static void main(String[] args) throws URISyntaxException, InterruptedException, JsonProcessingException {

        URI serverUri = new URI("ws://localhost:8080/pvws/pv");
        CountDownLatch latch = new CountDownLatch(1);  // Wait until connected.
        ObjectMapper mapper = new ObjectMapper();
        SessionHandler client = new SessionHandler(serverUri, latch, mapper);
        client.connect();


        PVcache cache = new PVcache();
        SubscriptionHandler subHandler = new SubscriptionHandler(client, cache, mapper);
        client.setSubscriptionHandler(subHandler);


        // Wait up to 5 seconds for the connection to open
        if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
            System.out.println("Timeout waiting for WebSocket connection.");
        }

        //  MAYBE THIS HELPS SO THE FIRST MESSAGE IS NOT MISSED?
        Thread.sleep(500);


        //String[] PVs = new String[]{"sim://sine", "loc://x(4)"};


        String[] PVs = new String[]{"sim://noiseWaveForm"};
        client.subscribeClient(PVs);

        Thread.sleep(5000000);

        client.close();


        //TestLatency2 test = new TestLatency2(serverUri, latch, mapper);
        //test.connect();
        //test.testing();


    }


}


//BRING UP SERVER AND CLIENT

//CREDENTIALS USERNAME AND PASSWORD SHOULD BE SENT ON CONNECT I THINK


//USE MESSAGES ALREADY ON PVWS SERVER MODELING
// VTYPE TO JSON
//