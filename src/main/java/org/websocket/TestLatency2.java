package org.websocket;

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

/*public class TestLatency2 extends WebSocketClient {
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

        Message subscribeMsg = new Message("subscribe", new String[]{"sim://sine"});
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

            /*long millis = pvData.getSeconds() * 1000L; // Assuming this returns millis
            Instant sentTime = Instant.ofEpochMilli(millis);
            long latency = Instant.now().toEpochMilli() - sentTime.toEpochMilli();
            */
            /*long seconds = pvData.getSeconds();
            int nanos = pvData.getNanos();

            Instant sentTime = Instant.ofEpochSecond(seconds, nanos);
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
    /*
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
*/

        /*public class TestLatency2 extends WebSocketClient {
            private final ObjectMapper mapper;
            private final CountDownLatch latch;

            // Tracks all latencies per PV
            public final Map<String, List<Long>> pvLatencies = new ConcurrentHashMap<>();

            // Optional: first message latencies per PV
            private final Map<String, Long> firstMessageLatencies = new ConcurrentHashMap<>();

            public TestLatency2(URI serverUri, CountDownLatch latch, ObjectMapper mapper) {
                super(serverUri);
                this.latch = latch;
                this.mapper = mapper;
            }

            public void testing() throws JsonProcessingException, InterruptedException {
                // Send subscription
                System.out.println("sent subscription");

                Message subscribeMsg = new Message("subscribe", new String[]{"sim://sine"});
                String json = mapper.writeValueAsString(subscribeMsg);
                this.send(json);

                // Wait for messages to be received
                Thread.sleep(5000);

                // Output summary
                System.out.println("=== Latency Summary ===");
                this.pvLatencies.forEach((pv, list) -> {
                    double avg = list.stream().mapToLong(Long::longValue).average().orElse(0);
                    long first = list.get(0);
                    long last = list.get(list.size() - 1);

                    System.out.printf("PV: %s | Msgs: %d | Avg: %.2f ms | First: %d ms | Last: %d ms%n",
                            pv, list.size(), avg, first, last);
                });

                // Optionally show first message latency separately
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
                    long seconds = pvData.getSeconds();
                    int nanos = pvData.getNanos();

                    // Convert to Instant and calculate latency using current wall time
                    Instant sentTime = Instant.ofEpochSecond(seconds, nanos);
                    long latency = Instant.now().toEpochMilli() - sentTime.toEpochMilli();

                    pvLatencies
                            .computeIfAbsent(pv, k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(latency);

                    if (!firstMessageLatencies.containsKey(pv)) {
                        firstMessageLatencies.put(pv, latency);
                    }

                    System.out.printf("📨 [%s] Message #%d after %.3f ms%n", pv,
                            pvLatencies.get(pv).size(), (double) latency);
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
        }*/

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

        Thread.sleep(5000); // wait for messages

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

