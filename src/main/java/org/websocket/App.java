package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.websocket.handlers.HeartbeatHandler;
import org.websocket.handlers.ReconnectHandler;
import org.websocket.handlers.SessionHandler;
import org.websocket.handlers.SubscriptionHandler;
import org.websocket.models.PvMetaData;
import org.websocket.util.MetadataHandler;
import org.websocket.util.PVcache;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.*;


public class App {
    public static void main(String[] args) throws URISyntaxException, InterruptedException, JsonProcessingException {

        URI serverUri = new URI("ws://localhost:8080/pvws/pv");
        SessionHandler client = initializeClient(serverUri);

        String[] PVs = new String[]{"sim://noiseWaveForm", "sim://noise"};
        client.subscribeClient(PVs);

        Thread.sleep(5000000);
        //scheduler.shutdownNow(); //disables auto reconnect and other schedules SCHEDULER SHOULD BE SHUTDOWN WHEN CLIENT IS CLOSEED
        client.close();
    }

    public static SessionHandler initializeClient(URI serverUri) throws URISyntaxException, InterruptedException, JsonProcessingException {
        CountDownLatch latch = new CountDownLatch(1);  // Wait until connected.
        ObjectMapper mapper = new ObjectMapper();
        SessionHandler client = new SessionHandler(serverUri, latch, mapper);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        setHandlers(client, mapper, scheduler);

        client.connect();
        latch.await();

        return client;
    }

    public static void setHandlers(SessionHandler client, ObjectMapper mapper, ScheduledExecutorService scheduler) throws URISyntaxException, InterruptedException, JsonProcessingException {
        HeartbeatHandler heartbeatHandler = initializeHeartbeatHandler(client, scheduler);
        client.setHeartbeatHandler(heartbeatHandler);

        SubscriptionHandler subHandler = initializeSubHandler(client, mapper);
        client.setSubscriptionHandler(subHandler);

        ReconnectHandler reconnectHandler = initializeReconnectHandler(client, scheduler);
        client.setReconnectHandler(reconnectHandler);

        MetadataHandler metadataHandler = initializeMetadataHandler(client);
        client.setMetadataHandler(metadataHandler);


    }


    private static HeartbeatHandler initializeHeartbeatHandler(SessionHandler client, ScheduledExecutorService scheduler) {
        final long HEARTBEAT_INTERVAL = 10000;  // 10 seconds
        final long HEARTBEAT_TIMEOUT = 15000;   // 15 seconds
        return new HeartbeatHandler(client, scheduler, HEARTBEAT_INTERVAL, HEARTBEAT_TIMEOUT);
    }

    private static SubscriptionHandler initializeSubHandler(SessionHandler client, ObjectMapper mapper) throws URISyntaxException, InterruptedException, JsonProcessingException {
        PVcache cache = new PVcache();
        return new SubscriptionHandler(client, cache, mapper);
    }

    private static ReconnectHandler initializeReconnectHandler(SessionHandler client, ScheduledExecutorService scheduler) {
        return new ReconnectHandler(client, scheduler);
    }

    private static MetadataHandler initializeMetadataHandler(SessionHandler client) throws URISyntaxException, InterruptedException, JsonProcessingException {
        ConcurrentHashMap<String, PvMetaData> pvMetaMap = new ConcurrentHashMap<String, PvMetaData>();
        ConcurrentHashMap<String, Integer> subscribeAttempts = new ConcurrentHashMap<String, Integer>();

        return new MetadataHandler(pvMetaMap, subscribeAttempts);
    }

}