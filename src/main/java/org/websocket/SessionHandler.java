package org.websocket;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.PV;

import java.net.URI;
import java.util.concurrent.*;

public class SessionHandler extends WebSocketClient {
    private final ObjectMapper mapper;
    private final CountDownLatch latch;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean reconnecting = false;

    private SubscriptionHandler subHandler;






    public SessionHandler(URI serverUri, CountDownLatch latch, ObjectMapper mapper) {
        super(serverUri);
        this.latch = latch;
        this.mapper = mapper;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("✅ Connected to server");
        latch.countDown();
        reconnecting = false;
    }

    @Override
    public void onMessage(String message) {
    /*
        try {
            JsonNode node = mapper.readTree(message);
            if (node.has("type") && node.has("pv")) {
                String type = node.get("type").asText();
                String pvName = node.get("pv").asText();

                if (type.equals("update") || type.equals("subscribe")) {
                    long now = System.nanoTime();
                    long latency = now - subscribeStartTime;

                    // Record latency
                    pvLatencies
                            .computeIfAbsent(pvName, k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(latency);

                    System.out.printf("📨 [%s] Message #%d after %.3f ms%n", pvName,
                            pvLatencies.get(pvName).size(), latency / 1_000_000.0);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to parse message: " + e.getMessage());
        }
        */
        System.out.println("📨👍👍 Received: " + message);
        try {
            JsonNode node = mapper.readTree(message);
            // each message from server has type, type of update will look something like this: {"type":"update","pv":"sim://sine","ts":"2025-06-30T19:39:50.
            if (node.has("type")) {
                String type = node.get("type").asText();
                switch (type) {
                    case "update": //this type means its an updated process variable;
                        PV pvObj = mapper.treeToValue(node, PV.class);
                        System.out.println("✅😊 Parsed Message: " + pvObj);
                        break;
                    default:
                        System.out.println("⚠️ 😤Unknown message type: " + type);
                }
            } else {
                System.out.println("⚠️ Message without 'type': " + message);
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to parse message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Disconnected. Reason: " + reason);
        attemptReconnect();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("🚨 WebSocket Error: " + ex.getMessage());
        attemptReconnect();
    }

    private void attemptReconnect() {
        if (!reconnecting) {
            reconnecting = true;
            System.out.println("🔁 😉Attempting to reconnect in 10 seconds...");
            scheduler.schedule(() -> {
                try {
                    this.reconnectBlocking();  // blocks thread while attempting reconnect
                    subHandler.subscribeCache();
                    System.out.println("✅ Reconnected");
                } catch (InterruptedException e) {
                    System.err.println("❌ Reconnect failed: " + e.getMessage());
                    reconnecting = false;
                    attemptReconnect();  // keep retrying
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }, 20, TimeUnit.SECONDS);
        }
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



}