package com.medistock.concurrency;

/**
 * JavaBean / Snapshot model representing a thread's inspection state for JavaFX TableView.
 * Owner: Member 1.
 */
public class ThreadInfoSnapshot {

    private final long id;
    private final String name;
    private final int priority;
    private final boolean daemon;
    private final String state;
    private final boolean alive;
    private final String purpose;

    public ThreadInfoSnapshot(long id, String name, int priority, boolean daemon, String state, boolean alive, String purpose) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.daemon = daemon;
        this.state = state;
        this.alive = alive;
        this.purpose = purpose;
    }

    public static ThreadInfoSnapshot of(Thread thread, String purpose) {
        return new ThreadInfoSnapshot(
                thread.threadId(),
                thread.getName(),
                thread.getPriority(),
                thread.isDaemon(),
                thread.getState().name(),
                thread.isAlive(),
                purpose
        );
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public String getState() {
        return state;
    }

    public boolean isAlive() {
        return alive;
    }

    public String getPurpose() {
        return purpose;
    }
}
