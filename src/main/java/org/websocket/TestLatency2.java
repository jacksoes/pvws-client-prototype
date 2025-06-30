package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.Message;
import org.websocket.models.PV;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TestLatency2 extends WebSocketClient {
    ObjectMapper mapper;
    CountDownLatch latch;
    public final ArrayList<String> firstMessageLatencies = new ArrayList<>();
    public final Set<String> receivedPVs = ConcurrentHashMap.newKeySet();


    private volatile long subscribeStartTime;
    public final Map<String, List<Long>> pvLatencies = new ConcurrentHashMap<>();



    public TestLatency2(URI serverUri, CountDownLatch latch, ObjectMapper mapper) {
        super(serverUri);
        this.latch = latch;
        this.mapper = mapper;
    }
    public void setSubscribeStartTime(long time) {
        this.subscribeStartTime = time;
    }

    public void testing() throws JsonProcessingException, InterruptedException {
        //TEST START
        System.out.println("sent subscription");

        Message subscribeMsg = new Message("subscribe", new String[]{"ca://jack:calc1"});
        String json = mapper.writeValueAsString(subscribeMsg);
        long start = System.nanoTime();
        this.setSubscribeStartTime(start);
        this.send(json);
        Thread.sleep(5000); // wait for messages

        System.out.println("=== Latency Summary ===");



        this.pvLatencies.forEach((pv, list) -> {
            double avgMs = list.stream().mapToLong(l -> l).average().orElse(0);
            System.out.printf("PV: %s | Msgs: %d | Avg: %.2f ms | First: %.2f ms | Last: %.2f ms%n",
                    pv, list.size(), avgMs,
                    (double) list.get(0),
                    (double) list.get(list.size() - 1)
            );
        });



        // keep client open while sessionhandler prints message updates
        ///Thread.sleep(50000);
        //TEST END
        this.close();

        this.firstMessageLatencies.forEach((pv) -> {
            System.out.printf("PV %s: ms%n", pv);
        });


    }
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("✅ Connected to server");
        latch.countDown();
    }

    @Override
    public void onMessage(String message) {

        PV pvData = null;
        try {
            pvData = mapper.readValue(message, PV.class);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }

        String type = pvData.getType();
        String pv = pvData.getPv();

        if (type != null && (type.equals("update") || type.equals("subscribe"))) {
            // ✅ Parse timestamp and calculate latency


            //long seconds = Long.parseLong(pvData.getSeconds() + "");
            //Instant sentTime = Instant.ofEpochSecond(seconds);
            //InstsentTime = Instant.parse((pvData.getSeconds() + ""));
            //long latency = Instant.now().toEpochMilli() - sentTime.toEpochMilli();

            long millis = pvData.getSeconds() * 1000L; // Assuming this returns millis
            Instant sentTime = Instant.ofEpochMilli(millis);
            long latency = Instant.now().toEpochMilli() - sentTime.toEpochMilli();

            // ✅ Store latency
            pvLatencies
                    .computeIfAbsent(pv, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(latency);

            System.out.printf("📨 [%s] Message #%d after %.3f ms%n", pv,
                    pvLatencies.get(pv).size(), (double) latency);
        }

    }
        /*
        System.out.println("📨 Received: " + message);
        try {
            JsonNode node = mapper.readTree(message);

            if (node.has("type")) {
                String type = node.get("type").asText();

                switch (type) {
                    case "subscribe":
                    case "update":
                        PV msgObj = mapper.treeToValue(node, PV.class);
                        System.out.println("✅ Parsed Message: " + msgObj.getValue());
                        break;
                    default:
                        System.out.println("⚠️ Unknown message type: " + type);
                }
            } else {
                System.out.println("⚠️ Message without 'type': " + message);
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to parse message: " + e.getMessage());
        }*/
    //}

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Disconnected. Reason: " + reason);
        //attemptReconnect();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("🚨 WebSocket Error: " + ex.getMessage());
        //attemptReconnect();
    }
}
