package org.websocket.handlers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.PV;


import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.*;
import org.java_websocket.framing.PingFrame;
import org.websocket.models.PvMetaData;
import org.websocket.util.Base64BufferDeserializer;
import org.websocket.util.MetaDataCache;


public class SessionHandler extends WebSocketClient {

    private final ObjectMapper mapper;
    private final CountDownLatch latch;
    private boolean reconnecting = false;

    private SubscriptionHandler subHandler;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private HeartbeatHandler heartbeatHandler;

    public int testCount = 0;


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
            //handleHeartbeat();
            heartbeatHandler.start(this);
        } catch (Exception e) {
            System.err.println("Exception in onOpen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(String message) {


        System.out.println("📨👍👍 Received: " + message);

        try {
            JsonNode node = mapper.readTree(message);
            // each message from server has type, type of update will look something like this: {"type":"update","pv":"sim://sine","ts":"2025-06-30T19:39:50.
            if(node.has("vtype") && testCount > 2) // if message has vtype field it is first message with meta-data
            {
                PvMetaData pvMeta = mapper.treeToValue(node, PvMetaData.class);
                MetaDataCache.setData(pvMeta); // comment this line out to test missing
            }
            testCount++;


            // message recieved should always have a type field
            String type = node.get("type").asText();
            switch (type) {
                case "update": //this type means its an updated process variable;
                    PV pvObj = mapper.treeToValue(node, PV.class);
                    // checks for encoded array, if found it decodes and sets it as value of pv.
                    Base64BufferDeserializer.decodeArrValue(node, pvObj);




                    //every PV should have corresponding meta data if its not their resubscirbe and ignore message
                    if(!MetaDataCache.pvMetaMap.containsKey(pvObj.getPv())) {

                        final int MAX_SUBSCRIBE_ATTEMPTS = 5;
                        MetaDataCache.refetch(MAX_SUBSCRIBE_ATTEMPTS, pvObj, this);

                            }
                            else // if meta data is not missing continue
                            {

                                //subscribeAttempts.remove(pvObj.getPv()); // reset retry count if we got the meta data
                                if(node.has("severity"))// if severity changes set it in cached value
                                {
                                    MetaDataCache.pvMetaMap.get(pvObj.getPv()).setSeverity(node.get("severity").asText());
                                }

                        //merges class PV and json node of metadata together
                        JsonNode nodeMerge = mapper.valueToTree(MetaDataCache.pvMetaMap.get(pvObj.getPv()));
                        mapper.readerForUpdating(pvObj).readValue(nodeMerge);

                        VtypeHandler.processUpdate(pvObj);
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
        //stopHeartbeat();
        heartbeatHandler.stop();
        attemptReconnect();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("🚨 WebSocket Error: " + ex.getMessage());
        //stopHeartbeat();
        heartbeatHandler.stop();
        attemptReconnect();
    }

    public void attemptReconnect() {
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
        //lastPongTime = System.currentTimeMillis();
        heartbeatHandler.setLastPongTime(System.currentTimeMillis());
    }

    public void closeClient() {
        scheduler.shutdownNow(); //disables auto reconnect
        this.close();
    }


    public void setSubscriptionHandler(SubscriptionHandler subHandler) {
        this.subHandler = subHandler;
    }

    public void setHeartbeatHandler(HeartbeatHandler heartbeatHandler) {
        this.heartbeatHandler = heartbeatHandler;
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
        //lastPongTime = System.currentTimeMillis();  // update last pong time- comment this line out to test heartbeat timeout

        heartbeatHandler.setLastPongTime(System.currentTimeMillis());
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
       // if (heartbeatCheck != null && !heartbeatCheck.isCancelled()) {
         //   heartbeatCheck.cancel(true);
       // }

    }


}
