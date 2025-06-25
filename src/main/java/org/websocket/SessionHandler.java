package org.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.Message;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

public class SessionHandler extends WebSocketClient {
    private final ObjectMapper mapper;
    private final CountDownLatch latch;

    public SessionHandler(URI serverUri, CountDownLatch latch, ObjectMapper mapper) {
        super(serverUri);
        this.latch = latch;
        this.mapper = mapper;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to server");
        latch.countDown();  // Signal connection complete
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        try {
            JsonNode node = mapper.readTree(message);

            if (node.has("type")) {
                String type = node.get("type").asText();

                switch (type) {
                    case "subscribe":
                    case "update":
                        Message msgObj = mapper.treeToValue(node, Message.class);
                        System.out.println("Parsed Message: " + msgObj);
                        break;

                    // Add other cases for different message types
                    default:
                        System.out.println("Unknown message type: " + type);
                }
            } else {
                System.out.println("Message without 'type': " + message);
            }

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
        latch.countDown();  // Prevent blocking
    }


}