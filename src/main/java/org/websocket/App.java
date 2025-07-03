package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.websocket.models.Message;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

public class App 
{
    public static void main( String[] args ) throws URISyntaxException, InterruptedException, JsonProcessingException {

        URI serverUri = new URI("ws://localhost:8080/pvws/pv");
        CountDownLatch latch = new CountDownLatch(1);  // Wait until connected.
        ObjectMapper mapper = new ObjectMapper();
        //SessionHandler client = getSessionHandler(serverUri, latch);
        SessionHandler client = new SessionHandler(serverUri, latch, mapper);
        client.connect();


        PVcache cache = new PVcache();
        SubscriptionHandler subHandler = new SubscriptionHandler(client, cache, mapper);
        client.setSubscriptionHandler(subHandler);


        // Wait up to 5 seconds for the connection to open
        if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
            System.out.println("Timeout waiting for WebSocket connection.");
        }

        // turn message into json object because server only accepts json.
        //Message message = new Message("subscribe", new String[]{"pva://jack:calc1", "loc://x(4)"});


        //String[] PVs = new String[]{"sim://sine", "loc://x(4)"};
        //client.subscribeClient(PVs);


        //client.close();


        //TestLatency2 test = new TestLatency2(serverUri, latch, mapper);
        //test.connect();
        //test.testing();



    }




}
