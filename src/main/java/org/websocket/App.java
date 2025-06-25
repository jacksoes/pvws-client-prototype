package org.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.Message;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

public class App 
{
    public static void main( String[] args ) throws InterruptedException, URISyntaxException, JsonProcessingException {




        //ObjectMapper mapper = new ObjectMapper();
        //Message message = new Message("subscribe", new String[] {"sim://sine", "loc://x(4)"});

        ObjectMapper mapper = new ObjectMapper();
        Message message = new Message("subscribe", new String[]{"sim://sine", "loc://x(4)"});
        //Message message = new Message("subscribe", new String[]{"pva://jack:calc1", "loc://x(4)"});

        String json = mapper.writeValueAsString(message);
        System.out.println(json);


// Serialize to JSON
        //String json = mapper.writeValueAsString(message);

// Send as a WebSocket message


        // Replace with your WebSocket server URL
        URI serverUri = new URI("ws://localhost:8080/pvws/pv");

        //URI serverUri = new URI("ws://localhost:8080/pvws/websocket");

        CountDownLatch latch = new CountDownLatch(1);  // Wait until connected

        WebSocketClient client = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("Connected to server");
                send(json);
                latch.countDown();  // Signal connection complete
            }

            @Override
            public void onMessage(String message) {
                System.out.println("Received: " + message);
                try {
                    Message msgObj = mapper.readValue(message, Message.class);
                    System.out.println("Parsed message: " + msgObj);
                    // Or, for more detailed printing:
                    // System.out.println("Type: " + msgObj.getType());
                    // System.out.println("PVs: " + Arrays.toString(msgObj.getPvs()));
                } catch (Exception e) {
                    System.err.println("Failed to parse message: " + e.getMessage());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("Disconnected. Reason: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("Error: " + ex.getMessage());
                latch.countDown();  // Don't hang if error occurs
            }
        };

        client.connect();

        // Wait up to 5 seconds for the connection to open
        if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
            System.out.println("Timeout waiting for WebSocket connection.");
        }

        Thread.sleep(50000);  // Let the client run for a bit

        client.close();
    }
}
