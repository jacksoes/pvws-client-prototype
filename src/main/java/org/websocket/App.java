package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.websocket.models.Message;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

public class App 
{
    public static void main( String[] args ) throws InterruptedException, URISyntaxException, JsonProcessingException {

        URI serverUri = new URI("ws://localhost:8080/pvws/pv");
        CountDownLatch latch = new CountDownLatch(1);  // Wait until connected.
        ObjectMapper mapper = new ObjectMapper(); // library for JSON mapping/parsing.
        SessionHandler client = new SessionHandler(serverUri, latch, mapper); // extends websocket client and handles session.

        client.connect();

        // Wait up to 5 seconds for the connection to open
        if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
            System.out.println("Timeout waiting for WebSocket connection.");
        }

        // turn message into json object because server only accepts json.
        //Message message = new Message("subscribe", new String[]{"pva://jack:calc1", "loc://x(4)"});
        Message message = new Message("subscribe", new String[]{"sim://sine", "loc://x(4)"});
        String json = mapper.writeValueAsString(message);
        System.out.println(json);

        //send json to server
        client.send(json);


        // keep client open while sessionhandler prints message updates
        Thread.sleep(50000);

        client.close();
    }
}
