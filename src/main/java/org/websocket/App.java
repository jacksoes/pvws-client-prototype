package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.websocket.models.Message;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class App 
{
    public static void main( String[] args ) throws InterruptedException, URISyntaxException, JsonProcessingException {

        URI serverUri = new URI("ws://localhost:8080/pvws/pv");
        CountDownLatch latch = new CountDownLatch(1);  // Wait until connected.
        ObjectMapper mapper = new ObjectMapper(); // library for JSON mapping/parsing.

/*
        SessionHandler client = new SessionHandler(serverUri, latch, mapper);// extends websocket client and handles session

        PVcache cache = new PVcache();
        SubscriptionHandler subHandler = new SubscriptionHandler(client, cache, mapper);
        client.setSubscriptionHandler(subHandler);

        client.connect();

        // Wait up to 5 seconds for the connection to open
        if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
            System.out.println("Timeout waiting for WebSocket connection.");
        }

        // turn message into json object because server only accepts json.
        //Message message = new Message("subscribe", new String[]{"pva://jack:calc1", "loc://x(4)"});


        String[] PVs = new String[]{"sim://sine", "loc://x(4)"};

        client.subscribeClient(PVs);


            //TEST START
       /* Message subscribeMsg = new Message("subscribe", new String[]{"sim://sine", "sim://cos"});
        String json = mapper.writeValueAsString(subscribeMsg);
        long start = System.nanoTime();
        client.setSubscribeStartTime(start);
        client.send(json);
        Thread.sleep(5000); // wait for messages

        System.out.println("=== Latency Summary ===");

        client.pvLatencies.forEach((pv, list) -> {
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
        client.closeClient();

        client.firstMessageLatencies.forEach((pv) -> {
            System.out.printf("PV %s: ms%n", pv);
        });
        */

        TestLatency2 test = new TestLatency2(serverUri, latch, mapper);
        test.connect();
        Thread.sleep(5000);
        test.testing();
    }


    public void testTime()
    {

    }
}
