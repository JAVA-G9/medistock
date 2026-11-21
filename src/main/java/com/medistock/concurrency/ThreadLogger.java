package com.medistock.concurrency;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe logging mechanism for thread execution tracing and observation.
 * Owner: Member 1.
 */
public final class ThreadLogger {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX_LOGS = 300;

    private static final ObservableList<LogEntry> observableLogs = FXCollections.observableArrayList();
    private static final List<LogEntry> history = new ArrayList<>();

    private ThreadLogger() {
    }

    public static class LogEntry {
        private final String timestamp;
        private final String threadName;
        private final int priority;
        private final String state;
        private final String message;

        public LogEntry(String timestamp, String threadName, int priority, String state, String message) {
            this.timestamp = timestamp;
            this.threadName = threadName;
            this.priority = priority;
            this.state = state;
            this.message = message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getThreadName() {
            return threadName;
        }

        public int getPriority() {
            return priority;
        }

        public String getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("[%s] [%s | Pri=%d | State=%s] %s",
                    timestamp, threadName, priority, state, message);
        }
    }

    /**
     * Log a message capturing the calling thread's name, priority, and state.
     */
    public static void log(String message) {
        Thread current = Thread.currentThread();
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        LogEntry entry = new LogEntry(
                timestamp,
                current.getName(),
                current.getPriority(),
                current.getState().name(),
                message
        );

        synchronized (history) {
            history.add(entry);
            if (history.size() > MAX_LOGS) {
                history.remove(0);
            }
        }

        // Safely update JavaFX observable list if toolkit is available
        try {
            Platform.runLater(() -> appendObservable(entry));
        } catch (Throwable t) {
            // In headless test environments or before toolkit startup, fallback to stdout
            System.out.println(entry);
        }
    }

    private static void appendObservable(LogEntry entry) {
        observableLogs.add(entry);
        if (observableLogs.size() > MAX_LOGS) {
            observableLogs.remove(0);
        }
    }

    public static ObservableList<LogEntry> getObservableLogs() {
        return observableLogs;
    }

    public static void clear() {
        synchronized (history) {
            history.clear();
        }
        try {
            Platform.runLater(observableLogs::clear);
        } catch (Throwable ignored) {
            observableLogs.clear();
        }
    }
}
