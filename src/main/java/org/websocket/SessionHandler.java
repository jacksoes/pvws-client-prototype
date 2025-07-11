package org.websocket;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.Message;
import org.websocket.models.PV;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

import org.java_websocket.framing.PingFrame;

//LOOK FOR EXAMPLES OF HEARTBEAT AND PING PONG ON WEBSOCKET AND STOMP WEBSOCKET

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
        handleHeartbeat();
    }
    //AUTHENTICATION MIGHT BE NEEDED FOR NON-STOMP
    @Override
    public void onMessage(String message) {

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
                    case "ping":
                        System.out.println("😀 received ping from server sending pong");
                        try {
                            String json = mapper.writeValueAsString(new Message("pong"));
                            send(json);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "pong":
                        Message pong = mapper.treeToValue(node, Message.class);
                        System.out.println("parse pong: " + pong);
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
        // FOR AUTO RECCONNECT
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

    // HAVE TO SHOW PING ON SERVER AND RECEIVE PONG
    private void handleHeartbeat(){
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
               /* Message message = new Message("ping");
                try {
                    String json = mapper.writeValueAsString(message);
                    send(json);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }*/
                sendPingToServer();
                System.out.println("Ping sent✉️");
            }
        }, 0, 20000);
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
        System.out.println("Received Pong frame");
        super.onWebsocketPong(conn, f);
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


}

//BENEFITS OF PING CLIENT->SERVER:
/*
1. Client will not be dropped if it is idle
2. verifies server is up and responsive
3. can attempt reconnect.
4. verifies connection is healthy

 */