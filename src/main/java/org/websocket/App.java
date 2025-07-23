package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.websocket.handlers.HeartbeatHandler;
import org.websocket.handlers.SessionHandler;
import org.websocket.handlers.SubscriptionHandler;
import org.websocket.util.PVcache;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;


// PIPELINE: 1. RECEIVED MESSAGE, 2. MAP TO OBJECT AND META DATA, 3. CREATE VTYPE
// FIND WAY SO FIRST MESSAGE IS ALWAYS THE ONE WITH MET DATA

// potentional solution: THROW AWAY MESSAGES TILL ONE IS GOTTEN WITH METADATA
//1, GET UPDATE 2. CHECK IT HAS META DATA TYPE 3. POPULATE CACHE. 4. IF CACHE IS NOT POPULATED THEN THROW AWAY MESSAGE
public class App {
    public static void main(String[] args) throws URISyntaxException, InterruptedException, JsonProcessingException {

        URI serverUri = new URI("ws://localhost:8080/pvws/pv");
        SessionHandler client = initializeClient(serverUri);

        String[] PVs = new String[]{"sim://noiseWaveForm"};
        client.subscribeClient(PVs);

        Thread.sleep(5000000);
        client.close();

    }

    public static SessionHandler initializeClient(URI  serverUri) throws URISyntaxException, InterruptedException, JsonProcessingException {
        CountDownLatch latch = new CountDownLatch(1);  // Wait until connected.
        ObjectMapper mapper = new ObjectMapper();
        SessionHandler client = new SessionHandler(serverUri, latch, mapper);



        setHandlers(client, mapper);

        client.connect();


        latch.await();



        return client;

    }

    public static void setHandlers(SessionHandler client, ObjectMapper mapper) throws URISyntaxException, InterruptedException, JsonProcessingException {
        HeartbeatHandler heartbeatHandler = initializeHeartbeatHandler(client);
        client.setHeartbeatHandler(heartbeatHandler);

        SubscriptionHandler subHandler = initializeSubHandler(client, mapper);
        client.setSubscriptionHandler(subHandler);

    }


    private static HeartbeatHandler initializeHeartbeatHandler(SessionHandler client) {
        final long HEARTBEAT_INTERVAL = 10000;  // 10 seconds
        final long HEARTBEAT_TIMEOUT = 15000;   // 15 seconds
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        return new HeartbeatHandler(client, scheduler, HEARTBEAT_INTERVAL, HEARTBEAT_TIMEOUT);
    }

    private static SubscriptionHandler initializeSubHandler(SessionHandler client, ObjectMapper mapper) throws URISyntaxException, InterruptedException, JsonProcessingException {
        PVcache cache = new PVcache();
        SubscriptionHandler subHandler = new SubscriptionHandler(client, cache, mapper);
        return subHandler;

    }





}


//BRING UP SERVER AND CLIENT

//CREDENTIALS USERNAME AND PASSWORD SHOULD BE SENT ON CONNECT I THINK


//USE MESSAGES ALREADY ON PVWS SERVER MODELING
// VTYPE TO JSON
//