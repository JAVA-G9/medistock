package org.example.medistock.concurrency;

import org.example.medistock.model.Notification;
import org.example.medistock.repository.notification.NotificationRepository;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Background worker thread for asynchronous notification dispatch and persistence.
 * Owned by Member 4 (Expiry, Notifications & Reports).
 * Prevents UI freezes when persisting notifications to MySQL (TiDB Cloud) by offloading JDBC execution.
 */
public class NotificationWorker implements Runnable {

    private static NotificationWorker instance;
    private final BlockingQueue<Notification> dispatchQueue = new LinkedBlockingQueue<>(500);
    private final NotificationRepository repository;
    private volatile boolean running = false;
    private Thread workerThread;

    public NotificationWorker() {
        this(NotificationRepository.getInstance());
    }

    public NotificationWorker(NotificationRepository repository) {
        this.repository = repository;
    }

    public static synchronized NotificationWorker getInstance() {
        if (instance == null) {
            instance = new NotificationWorker();
        }
        return instance;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        workerThread = new Thread(this, "NotificationWorker-Thread");
        workerThread.setDaemon(true);
        workerThread.setPriority(Thread.NORM_PRIORITY - 1);
        workerThread.start();
        System.out.printf("[NOTIFICATION-WORKER] Started thread: %s%n", workerThread.getName());
    }

    public synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    public void enqueueNotification(Notification notification) {
        if (notification != null) {
            dispatchQueue.offer(notification);
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Notification n = dispatchQueue.take();
                if (repository != null) {
                    repository.save(n);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("[NOTIFICATION-WORKER] Dispatch error: " + e.getMessage());
            }
        }
    }

    public boolean isRunning() {
        return running && workerThread != null && workerThread.isAlive();
    }

    public int getQueueDepth() {
        return dispatchQueue.size();
    }
}
