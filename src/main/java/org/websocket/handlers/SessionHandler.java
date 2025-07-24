package org.websocket.handlers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.epics.vtype.VType;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.websocket.models.PV;


import java.net.URI;

import java.util.concurrent.*;

import org.websocket.models.PvMetaData;
import org.websocket.util.Base64BufferDeserializer;


public class SessionHandler extends WebSocketClient {
    private final ObjectMapper mapper;
    private final CountDownLatch latch;

    private SubscriptionHandler subHandler;
    private HeartbeatHandler heartbeatHandler;
    private ReconnectHandler reconnectHandler;
    private MetadataHandler metadataHandler;
    private VtypeHandler vtypeHandler;

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
            reconnectHandler.resetStatus();
            heartbeatHandler.start();
        } catch (Exception e) {
            System.err.println("Exception in onOpen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(String message) {

        System.out.println("📨👍👍 Received: " + message);

        try {

            JsonNode node = mapper.readTree(message);
            PvMetaData pvMeta = mapper.treeToValue(node, PvMetaData.class);
            if (pvMeta.getVtype() != null)
                metadataHandler.setData(pvMeta); // comment this line out to test missing


            String type = node.get("type").asText();
            switch (type) {
                case "update":
                    PV pvObj = mapper.treeToValue(node, PV.class);

                    if (!metadataHandler.pvMetaMap.containsKey(pvObj.getPv())) {

                        final int MAX_SUBSCRIBE_ATTEMPTS = 5;
                        metadataHandler.refetch(MAX_SUBSCRIBE_ATTEMPTS, pvObj, this);
                        return;

                    }
                    //subscribeAttempts.remove(pvObj.getPv()); // reset retry count if we got the meta data
                    if (node.has("severity"))// if severity changes set it in cached value
                    {
                        String currPV = pvObj.getPv();
                        String currSeverity = pvObj.getSeverity();
                        metadataHandler.pvMetaMap.get(currPV).setSeverity(currSeverity);
                    }
                    //checks for encoded array attribute if present, decode and set arr as pv.value
                    Base64BufferDeserializer.decodeArrValue(node, pvObj);
                    //merges class PV and json node of metadata together
                    JsonNode nodeMerge = mapper.valueToTree(metadataHandler.pvMetaMap.get(pvObj.getPv()));
                    mapper.readerForUpdating(pvObj).readValue(nodeMerge);

                    VType convertedPV = vtypeHandler.processUpdate(pvObj);

                    System.out.println("🧊⛸️🥶: " + pvObj.toString());

                    break;
                default:
                    System.out.println("⚠️ 😤Unknown message type: " + type);

            }
        } catch (Exception e) {
            System.err.println("Error parsing or processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Disconnected. Reason: " + reason);
        heartbeatHandler.stop();
        attemptReconnect();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("🚨 WebSocket Error: " + ex.getMessage());
        heartbeatHandler.stop();
        attemptReconnect();
    }


    public void closeClient() {
        this.close();
    }


    public void setSubscriptionHandler(SubscriptionHandler subHandler) {
        this.subHandler = subHandler;
    }

    public void setHeartbeatHandler(HeartbeatHandler heartbeatHandler) {
        this.heartbeatHandler = heartbeatHandler;
    }

    public void setReconnectHandler(ReconnectHandler reconnectHandler) {
        this.reconnectHandler = reconnectHandler;
    }

    public void setMetadataHandler(MetadataHandler metadataHandler) {
        this.metadataHandler = metadataHandler;
    }

    public void setVtypeHandler(VtypeHandler vtypeHandler) {
        this.vtypeHandler = vtypeHandler;
    }

    public void subscribeClient(String[] pvs) throws JsonProcessingException {
        subHandler.subscribe(pvs);
    }

    public void unSubscribeClient(String[] pvs) throws JsonProcessingException {
        subHandler.unSubscribe(pvs);
    }

    public void attemptReconnect() {
        this.reconnectHandler.attemptReconnect();
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
        heartbeatHandler.setLastPongTime(System.currentTimeMillis());
    }


}
