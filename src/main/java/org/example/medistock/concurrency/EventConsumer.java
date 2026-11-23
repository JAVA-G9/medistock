package org.example.medistock.concurrency;

import org.example.medistock.model.AppEvent;
import org.example.medistock.service.notifications.NotificationService;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * EventConsumer owned by Member 4 (Expiry, Notifications & Reports).
 * Multithreading syllabus role: Consumer in Producer-Consumer architecture using CustomEventQueue.
 * Consumes AppEvents pushed by Member 3 (Producer), processes them asynchronously,
 * and generates live notifications and audit logs without blocking the JavaFX UI thread.
 */
public class EventConsumer implements Runnable {

    private static EventConsumer instance;

    private final CustomEventQueue eventQueue;
    private final NotificationService notificationService;
    private volatile boolean running = false;
    private Thread consumerThread;

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private volatile AppEvent lastEvent;
    private final AtomicLong lastProcessedTimestamp = new AtomicLong(0);

    public EventConsumer() {
        this(CustomEventQueue.getInstance(), NotificationService.getInstance());
    }

    public EventConsumer(CustomEventQueue eventQueue, NotificationService notificationService) {
        this.eventQueue = eventQueue;
        this.notificationService = notificationService;
    }

    public static synchronized EventConsumer getInstance() {
        if (instance == null) {
            instance = new EventConsumer();
        }
        return instance;
    }

    /**
     * Starts the consumer thread as a daemon thread.
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        consumerThread = new Thread(this, "EventConsumer-Thread");
        consumerThread.setDaemon(true); // Daemon thread: automatically terminates on JVM exit
        consumerThread.setPriority(Thread.NORM_PRIORITY);
        consumerThread.start();
        System.out.printf("[EVENT-CONSUMER] Started thread: %s (Daemon: %b, Priority: %d)%n",
                consumerThread.getName(), consumerThread.isDaemon(), consumerThread.getPriority());
    }

    /**
     * Gracefully stops the consumer worker.
     */
    public synchronized void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }

    @Override
    public void run() {
        System.out.printf("[EVENT-CONSUMER] [%s] Entered RUNNABLE state. Waiting for events...%n",
                Thread.currentThread().getName());

        while (running) {
            try {
                // Blocks here using CustomEventQueue.take() with wait() until an event is produced
                AppEvent event = eventQueue.take();

                if (event == null) {
                    if (!running || eventQueue.isShutdown()) {
                        break;
                    }
                    continue;
                }

                // Process the event
                processEvent(event);

            } catch (InterruptedException e) {
                System.out.printf("[EVENT-CONSUMER] [%s] Received interruption signal.%n",
                        Thread.currentThread().getName());
                break;
            } catch (Exception e) {
                System.err.printf("[EVENT-CONSUMER] Error processing event: %s%n", e.getMessage());
            }
        }

        System.out.printf("[EVENT-CONSUMER] [%s] Terminated gracefully. Total processed: %d%n",
                Thread.currentThread().getName(), processedCount.get());
    }

    private void processEvent(AppEvent event) {
        System.out.printf("[EVENT-CONSUMER] [%s] Consumed event: %s (Ref: %s) -> Dispatching notification%n",
                Thread.currentThread().getName(), event.getEventType(), event.getReferenceId());

        if (notificationService != null) {
            notificationService.processAppEvent(event);
        }

        this.lastEvent = event;
        this.lastProcessedTimestamp.set(System.currentTimeMillis());
        this.processedCount.incrementAndGet();
    }

    public int getProcessedCount() {
        return processedCount.get();
    }

    public AppEvent getLastEvent() {
        return lastEvent;
    }

    public long getLastProcessedTimestamp() {
        return lastProcessedTimestamp.get();
    }

    public boolean isRunning() {
        return running && consumerThread != null && consumerThread.isAlive();
    }

    public Thread.State getThreadState() {
        return consumerThread != null ? consumerThread.getState() : Thread.State.NEW;
    }
}
