package org.websocket;


import java.util.Base64;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.PV;


import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import org.java_websocket.framing.PingFrame;
import org.websocket.models.PvMetaData;
import org.websocket.util.Base64BufferDeserializer;
import org.websocket.util.MetaDataCache;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane;


public class SessionHandler extends WebSocketClient {

    public boolean gotFirst = false;
    private final ObjectMapper mapper;
    private final CountDownLatch latch;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<String, Integer> subscribeAttempts = new ConcurrentHashMap<>();
    private final int MAX_SUBSCRIBE_ATTEMPTS = 5;
    private boolean reconnecting = false;

    private SubscriptionHandler subHandler;

    //detect missing heartbeats
    private ScheduledFuture<?> heartbeatCheck;
    private volatile long lastPongTime = System.currentTimeMillis();
    private static final long HEARTBEAT_INTERVAL = 10000;  // 10 seconds
    private static final long HEARTBEAT_TIMEOUT = 15000;   // 15 seconds


    public SessionHandler(URI serverUri, CountDownLatch latch, ObjectMapper mapper) {
        super(serverUri);
        this.latch = latch;
        this.mapper = mapper;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        try {
            System.out.println("Connected to server");
            latch.countDown();
            reconnecting = false;
            // System.out.println("Calling handleHeartbeat...");
            handleHeartbeat();
        } catch (Exception e) {
            System.err.println("Exception in onOpen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //AUTHENTICATION MIGHT BE NEEDED FOR NON-STOMP

    @Override
    public void onMessage(String message) {
        if(!gotFirst){
            gotFirst = true;
            return;
        }

        System.out.println("📨👍👍 Received: " + message);
        try {
            JsonNode node = mapper.readTree(message);

            // each message from server has type, type of update will look something like this: {"type":"update","pv":"sim://sine","ts":"2025-06-30T19:39:50.
            if(node.has("vtype")) // if message has vtype field it is first message with meta-data
            {
                PvMetaData pvMeta = mapper.treeToValue(node, PvMetaData.class);
                MetaDataCache.setData(pvMeta); // comment this line out to test missing  
            }


            // message recieved should always have a type field
            String type = node.get("type").asText();
            switch (type) {
                case "update": //this type means its an updated process variable;
                    PV pvObj = mapper.treeToValue(node, PV.class);

                    //check for encoded array field and if there is set value to decoded array.
                    if (node.has("b64dbl")) {
                        String base64Encoded = node.get("b64dbl").asText();
                        double[] doubles = Base64BufferDeserializer.decodeDoubles(base64Encoded);
                        pvObj.setValue(doubles);
                    }
                    else if (node.has("b64flt")) {
                        String base64Encoded = node.get("b64flt").asText();
                        float[] floats = Base64BufferDeserializer.decodeFloats(base64Encoded);
                        pvObj.setValue(floats);
                    }
                    else if (node.has("b64int")) {
                        String base64Encoded = node.get("b64int").asText();
                        int[] ints = Base64BufferDeserializer.decodeInts(base64Encoded);
                        pvObj.setValue(ints);
                    }
                    else if (node.has("b64srt")) {
                        String base64Encoded = node.get("b64srt").asText();
                        short[] shorts = Base64BufferDeserializer.decodeShorts(base64Encoded);
                        pvObj.setValue(shorts);
                    }
                    else if (node.has("b64byt")) {
                        String base64Encoded = node.get("b64byt").asText();
                        byte[] bytes = Base64.getDecoder().decode(base64Encoded);
                        pvObj.setValue(bytes);
                    }

                    //every PV should have corresponding meta data if its not their resubscirbe and ignore message
                    if(!MetaDataCache.pvMetaMap.containsKey(pvObj.getPv())) {
                        String pv = pvObj.getPv();
                        int currentAttempts = subscribeAttempts.getOrDefault(pv, 0);
                        if (currentAttempts >= MAX_SUBSCRIBE_ATTEMPTS) {
                            System.err.println("Max subscribe attempts reached for PV: " + pv); 
                            return;
                        }
                        
                        System.out.println("Missed first message for: " + pv + ": attempt " + (currentAttempts + 1));
                        try {
                            subscribeAttempts.put(pv, currentAttempts + 1);
                            this.unSubscribeClient(new String[]{pvObj.getPv()});
                            
                            scheduler.schedule(() -> {
                                try {
                                    this.subscribeClient(new String[]{pvObj.getPv()});
                                } catch (JsonProcessingException e) {
                                    System.err.println("Error during scheduled resubscribe: " + e.getMessage());
                               }
                            }, 5, TimeUnit.SECONDS); // retry after 5 seconds 
                            } catch (JsonProcessingException e) {
                                 System.err.println("Error unsubscribing or resubscribing PV: " + e.getMessage());
                                }
                                return;
                            }
                            else // if meta data is not missing continue
                            {
                                subscribeAttempts.remove(pvObj.getPv()); // reset retry count if we got the meta data
                                if(node.has("severity"))// if severity changes set it in cached value
                                {
                                    MetaDataCache.pvMetaMap.get(pvObj.getPv()).setSeverity(node.get("severity").asText());
                                }
                        //merges class together

                        JsonNode nodeMerge = mapper.valueToTree(MetaDataCache.pvMetaMap.get(pvObj.getPv()));
                        mapper.readerForUpdating(pvObj).readValue(nodeMerge);

                        PvProcessor.processUpdate(pvObj);
                        System.out.println("🧊⛸️🥶: " + pvObj.toString());
                    }
                        break;
                    default:
                        System.out.println("⚠️ 😤Unknown message type: " + type);

            }
        } catch (Exception e) {
            System.err.println("Error parsing or processing message: " + e.getMessage());
            e.printStackTrace();}
        }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Disconnected. Reason: " + reason);
        stopHeartbeat();
        attemptReconnect();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("🚨 WebSocket Error: " + ex.getMessage());
        stopHeartbeat();
        attemptReconnect();
    }

    private void attemptReconnect() {
        if (!reconnecting) {
            reconnecting = true;

            Runnable reconnectTask = new Runnable() {
                @Override
                public void run() {
                    System.out.println("Attempting to reconnect...");
                    try {
                        reconnectBlocking();  // Blocks until connected or fails
                        subHandler.subscribeCache();
                        if (isOpen()) {
                            System.out.println("Reconnected");
                            reconnecting = false;
                            subHandler.subscribeCache();
                            return; // stop retrying
                        } else {
                            throw new IllegalStateException("Connection not open after reconnect attempt.");
                        }
                    } catch (InterruptedException e) {
                        System.err.println("Reconnect interrupted: " + e.getMessage());
                        scheduleRetry(this);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        System.err.println("Reconnect failed: " + e.getMessage());
                        scheduleRetry(this);
                    }
                }

                private void scheduleRetry(Runnable task) {
                    System.out.println("Will retry in 10 seconds...");
                    scheduler.schedule(task, 10, TimeUnit.SECONDS);
                }
            };

            scheduler.execute(reconnectTask); // First try immediately
        }
    }

    private void handleHeartbeat() {
        System.out.println(" handleHeartbeat() called");
        lastPongTime = System.currentTimeMillis();
        heartbeatCheck = scheduler.scheduleAtFixedRate(() -> {
            try {
                //System.out.println(" Heartbeat loop running");
                this.sendPing();
                System.out.println("Ping sent");
                scheduler.schedule(() -> {
                    if (System.currentTimeMillis() - lastPongTime > HEARTBEAT_TIMEOUT) {
                        System.out.println("Heartbeat timeout. Reconnecting...");
                        attemptReconnect();
                    }
                }, 3, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("Heartbeat error: " + e.getMessage());
                attemptReconnect();
            }
        }, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void closeClient() {
        scheduler.shutdownNow(); //disables auto reconnect
        this.close();
    }


    public void setSubscriptionHandler(SubscriptionHandler subHandler) {
        this.subHandler = subHandler;
    }

    public void subscribeClient(String[] pvs) throws JsonProcessingException {
        subHandler.subscribe(pvs);
    }

    public void unSubscribeClient(String[] pvs) throws JsonProcessingException {
        subHandler.unSubscribe(pvs);
    }

    @Override
    public void onWebsocketPing(WebSocket conn, Framedata f) {
        System.out.println("Received Ping frame");
        super.onWebsocketPing(conn, f);
    }

    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {
        System.out.println("Received Pong frame"); // you could also comment this out to test the heartbeat timeout just for visual clarity
        super.onWebsocketPong(conn, f);
        lastPongTime = System.currentTimeMillis();  // update last pong time- comment this line out to test heartbeat timeout
    }


    public void sendPingToServer() {
        try {
            PingFrame ping = new PingFrame();
            // Optional: set payload data (must be <= 125 bytes)
            ping.setPayload(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));

            this.sendFrame(ping);  // send the ping frame manually
            System.out.println("Ping frame sent to server");
        } catch (Exception e) {
            System.err.println("Failed to send ping: " + e.getMessage());
        }
    }

    private void stopHeartbeat() {
        if (heartbeatCheck != null && !heartbeatCheck.isCancelled()) {
            heartbeatCheck.cancel(true);
        }
    }


}
