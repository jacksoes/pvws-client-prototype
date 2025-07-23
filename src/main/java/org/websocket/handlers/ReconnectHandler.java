package org.websocket.handlers;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReconnectHandler {
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;
    private final SessionHandler client;

    public ReconnectHandler(SessionHandler client, ScheduledExecutorService scheduler) {
        this.client = client;
        this.scheduler = scheduler;
    }

    public void attemptReconnect() {
        if (reconnecting.compareAndSet(false, true)) {
            Runnable reconnectTask = new Runnable() {
                @Override
                public void run() {
                    System.out.println("Attempting to reconnect...");
                    try {
                        boolean success = client.reconnectBlocking();  // Blocks until connected or fails
                        if (client.isOpen()) {
                            System.out.println("Reconnected successfully.");
                            reconnecting.set(false);
                        } else {
                            throw new IllegalStateException("Connection not open after reconnect attempt.");
                        }
                    } catch (InterruptedException e) {
                        System.err.println("Reconnect interrupted: " + e.getMessage());
                        scheduleRetry(this);
                    } catch (Exception e) {
                        System.err.println("Reconnect failed: " + e.getMessage());
                        scheduleRetry(this);
                    }
                }

                private void scheduleRetry(Runnable task) {
                    System.out.println("Will retry in 10 seconds...");
                    scheduler.schedule(task, 10, TimeUnit.SECONDS);
                }
            };

            scheduler.execute(reconnectTask); // Try immediately
        }
    }

    public void resetStatus() {
        reconnecting.set(false);
    }

    public boolean isReconnecting() {
        return reconnecting.get();
    }
}