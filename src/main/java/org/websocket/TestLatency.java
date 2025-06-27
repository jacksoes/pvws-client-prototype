package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.Message;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class TestLatency extends WebSocketClient {
    ObjectMapper mapper;
    CountDownLatch latch;
    public final ArrayList<String> firstMessageLatencies = new ArrayList<>();
    public final Set<String> receivedPVs = ConcurrentHashMap.newKeySet();


    private volatile long subscribeStartTime;
    public final Map<String, List<Long>> pvLatencies = new ConcurrentHashMap<>();



    public TestLatency(URI serverUri, CountDownLatch latch, ObjectMapper mapper) {
        super(serverUri);
        this.latch = latch;
        this.mapper = mapper;
    }
    public void setSubscribeStartTime(long time) {
        this.subscribeStartTime = time;
    }

    public void testing() throws JsonProcessingException, InterruptedException {
        //TEST START
        Message subscribeMsg = new Message("subscribe", new String[]{"sim://sine", "sim://cos"});
        String json = mapper.writeValueAsString(subscribeMsg);
        long start = System.nanoTime();
        this.setSubscribeStartTime(start);
        this.send(json);
        Thread.sleep(5000); // wait for messages

        System.out.println("=== Latency Summary ===");

        this.pvLatencies.forEach((pv, list) -> {
            double avgMs = list.stream().mapToLong(l -> l).average().orElse(0) / 1_000_000.0;
            System.out.printf("PV: %s | Msgs: %d | Avg: %.2f ms | First: %.2f ms | Last: %.2f ms%n",
                    pv, list.size(), avgMs,
                    list.get(0) / 1_000_000.0,
                    list.get(list.size() - 1) / 1_000_000.0
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
    }

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
