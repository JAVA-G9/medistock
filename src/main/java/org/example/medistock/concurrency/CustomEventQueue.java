package org.example.medistock.concurrency;

import org.example.medistock.model.AppEvent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Thread-safe bounded event queue demonstrating guarded blocks, wait(), notifyAll(),
 * and proper monitor ownership.
 * Shared between Member 3 (Producer) and Member 4 (Consumer).
 */
public class CustomEventQueue {

    private static final int DEFAULT_CAPACITY = 50;
    private static CustomEventQueue instance;

    private final Queue<AppEvent> queue = new LinkedList<>();
    private final int capacity;
    private boolean isShutdown = false;

    public CustomEventQueue() {
        this(DEFAULT_CAPACITY);
    }

    public CustomEventQueue(int capacity) {
        this.capacity = capacity;
    }

    public static synchronized CustomEventQueue getInstance() {
        if (instance == null) {
            instance = new CustomEventQueue();
        }
        return instance;
    }

    /**
     * Pushes an event to the queue. If queue is full, blocks until space becomes available.
     * Uses intrinsic monitor lock and guarded while block.
     */
    public synchronized void put(AppEvent event) throws InterruptedException {
        while (queue.size() >= capacity && !isShutdown) {
            wait();
        }
        if (isShutdown) {
            return;
        }
        queue.add(event);
        // Wake up waiting consumers
        notifyAll();
    }

    /**
     * Non-blocking or timed offer with fallback.
     */
    public synchronized boolean offer(AppEvent event) {
        if (queue.size() >= capacity || isShutdown) {
            return false;
        }
        queue.add(event);
        notifyAll();
        return true;
    }

    /**
     * Retrieves and removes an event. Blocks if the queue is empty until an event is pushed or shutdown.
     */
    public synchronized AppEvent take() throws InterruptedException {
        while (queue.isEmpty() && !isShutdown) {
            wait();
        }
        if (queue.isEmpty()) {
            return null; // Return null on shutdown
        }
        AppEvent event = queue.poll();
        // Wake up waiting producers
        notifyAll();
        return event;
    }

    public synchronized AppEvent poll() {
        if (queue.isEmpty()) {
            return null;
        }
        AppEvent event = queue.poll();
        notifyAll();
        return event;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized void shutdown() {
        this.isShutdown = true;
        notifyAll(); // Wake all waiting threads so they can exit gracefully
    }

    public synchronized boolean isShutdown() {
        return isShutdown;
    }

    public int getCapacity() {
        return capacity;
    }
}
