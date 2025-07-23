package org.websocket.handlers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class HeartbeatHandler {

    private final SessionHandler client;
    private ScheduledExecutorService scheduler;

    private ScheduledFuture<?> heartbeatTask;
    private volatile long lastPongTime;

    private final long heartbeatInterval;
    private final long heartbeatTimeout;


    public HeartbeatHandler(SessionHandler client,
                            ScheduledExecutorService scheduler,
                            long heartbeatInterval,
                            long heartbeatTimeout) {
        this.client = client;
        this.scheduler = scheduler;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatTimeout = heartbeatTimeout;
    }



    public void start(SessionHandler client){


        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                //System.out.println(" Heartbeat loop running");
                client.sendPing();
                System.out.println("Ping sent");
                scheduler.schedule(() -> {
                    if (System.currentTimeMillis() - lastPongTime > getHeartbeatTimeout()) {
                        System.out.println("Heartbeat timeout. Reconnecting...");
                        client.attemptReconnect();
                    }
                }, 3, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("Heartbeat error: " + e.getMessage());
                client.attemptReconnect();
            }
        }, 0, heartbeatInterval, TimeUnit.MILLISECONDS);

    }

    public void stop(){

        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(true);
        }


    }

    public void setLastPongTime(Long lastPongTime)
    {
        this.lastPongTime = lastPongTime;
    }


    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }







}
