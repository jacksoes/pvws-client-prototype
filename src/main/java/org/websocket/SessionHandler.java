package org.websocket;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.epics.util.stats.Range;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.Message;
import org.websocket.models.PV;

import org.epics.vtype.*;

//import org.epics.vtype.VDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.Display;


import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

import org.java_websocket.framing.PingFrame;

//import org.epics.vtype.*;
import org.epics.util.text.NumberFormats;



//LOOK FOR EXAMPLES OF HEARTBEAT AND PING PONG ON WEBSOCKET AND STOMP WEBSOCKET

public class SessionHandler extends WebSocketClient {
    private final ObjectMapper mapper;
    private final CountDownLatch latch;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
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
            handleHeartbeat();  } 
        catch (Exception e) {
        System.err.println("Exception in onOpen: " + e.getMessage());
        e.printStackTrace(); }
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


                        /*
                        String nameKey = pvObj.getPv();
                        String vtypeValue = pvObj.getVtype();
                        if(!VtypeHash.map.containsKey(nameKey))
                            VtypeHash.map.put(nameKey, vtypeValue);





                        String name = pvObj.getPv();
                        // at this point pv should always be in map with vtype
                        System.out.println(VtypeHash.map.get(name));


                        String vtype = VtypeHash.map.get(name);
                         */

                        //if (vtype.equals("VDouble")) {
                            Alarm alarm = Alarm.of(
                                    AlarmSeverity.valueOf(pvObj.getSeverity()),
                                    AlarmStatus.NONE,
                                    pvObj.getDescription()
                            );

                            Instant instant = Instant.ofEpochSecond(pvObj.getSeconds(), pvObj.getNanos());
                            Time time = Time.of(instant);

                            NumberFormat format = NumberFormats.precisionFormat(pvObj.getPrecision());

                            // TO DO: ALARM PARAMETERS ARE CURRENTLY INCORRECT
                            Display display = Display.of(Range.of(pvObj.getAlarm_low(), pvObj.getAlarm_high()), Range.of(pvObj.getWarn_low(), pvObj.getWarn_high()), Range.of(pvObj.getAlarm_low(), pvObj.getAlarm_high()), Range.of(pvObj.getMin(), pvObj.getMax()), pvObj.getUnits(), format);
                            //Parameters:
                        //displayRange - the display range
                        //warningRange - the warning range
                        //alarmRange - the alarm range
                        //controlRange - the control range
                        //units - the units
                        //numberFormat - the preferred number format


                 //           VDouble value = VDouble.of((Double) pvObj.getValue(), alarm, time, display);

                            Object Vvalue = VType.toVType(pvObj.getValue(), alarm, time, display);
                            //pvObj.setValue(value);

                        //}



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
        stopHeartbeat();
        attemptReconnect();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("🚨 WebSocket Error: " + ex.getMessage());
        stopHeartbeat();
        attemptReconnect();
    }
        // FOR AUTO RECCONNECT
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

    // HAVE TO SHOW PING ON SERVER AND RECEIVE PONG
    private void handleHeartbeat(){
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

//BENEFITS OF PING CLIENT->SERVER:
/*
1. Client will not be dropped if it is idle
2. verifies server is up and responsive
3. can attempt reconnect.
4. verifies connection is healthy

 */