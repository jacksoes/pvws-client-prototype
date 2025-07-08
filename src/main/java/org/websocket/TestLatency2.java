package org.websocket;
/*
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.Message;
import org.websocket.models.PV;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

//TRY TO DO WITH SECONDS AND NANOSECONDS INSTEAD OF TS ATTRIBUTE
// FIND EXPLANATION WHY THIS LATENCY IS LOWER THAN STOMP

public class TestLatency2 extends WebSocketClient {
    private final ObjectMapper mapper;
    private final CountDownLatch latch;

    // Tracks all latencies per PV
    public final Map<String, List<Long>> pvLatencies = new ConcurrentHashMap<>();

    // First message latency per PV (optional)
    private final Map<String, Long> firstMessageLatencies = new ConcurrentHashMap<>();

    public TestLatency2(URI serverUri, CountDownLatch latch, ObjectMapper mapper) {
        super(serverUri);
        this.latch = latch;
        this.mapper = mapper;
    }

    public void testing() throws JsonProcessingException, InterruptedException {
        System.out.println("sent subscription");

        Message subscribeMsg = new Message("subscribe", new String[]{"sim://sine"});
        String json = mapper.writeValueAsString(subscribeMsg);
        this.send(json);

        Thread.sleep(50000); // wait for messages

        System.out.println("=== Latency Summary ===");
        this.pvLatencies.forEach((pv, list) -> {
            double avg = list.stream().mapToLong(Long::longValue).average().orElse(0);
            long first = list.get(0);
            long last = list.get(list.size() - 1);

            System.out.printf("PV: %s | Msgs: %d | Avg: %.2f ms | First: %d ms | Last: %d ms%n",
                    pv, list.size(), avg, first, last);
        });

        System.out.println("\n=== First Message Latencies ===");
        firstMessageLatencies.forEach((pv, latency) ->
                System.out.printf("PV: %s | First latency: %d ms%n", pv, latency));

        this.close();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("✅ Connected to server");
        latch.countDown();
    }

    @Override
    public void onMessage(String message) {
        PV pvData;
        try {
            pvData = mapper.readValue(message, PV.class);
        } catch (JsonProcessingException ex) {
            System.err.println("❌ Failed to parse message: " + ex.getMessage());
            return;
        }

        String type = pvData.getType();
        String pv = pvData.getPv();

        if (type != null && (type.equals("update") || type.equals("subscribe"))) {
            String ts = pvData.getTs();  // <-- The new ISO8601 timestamp field as string
            if (ts == null) {
                System.err.println("⚠️ Missing 'ts' field in message for PV: " + pv);
                return;
            }

            try {
                Instant sentTime = Instant.parse(ts);
                long latency = Instant.now().toEpochMilli() - sentTime.toEpochMilli();

                pvLatencies
                        .computeIfAbsent(pv, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(latency);

                firstMessageLatencies.putIfAbsent(pv, latency);

                System.out.printf("📨 [%s] Message #%d after %d ms%n", pv,
                        pvLatencies.get(pv).size(), latency);
            } catch (DateTimeParseException e) {
                System.err.println("❌ Failed to parse 'ts' timestamp: " + ts + " for PV: " + pv);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Disconnected. Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("🚨 WebSocket Error: " + ex.getMessage());
    }


}

*/