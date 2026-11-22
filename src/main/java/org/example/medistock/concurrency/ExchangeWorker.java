package org.example.medistock.concurrency;

import org.example.medistock.model.StockTransfer;
import org.example.medistock.service.exchange.ExchangeService;

import java.util.List;

/**
 * Background worker thread owned by Member 3 (Stock Exchange & Requests).
 * Folder: concurrency/
 * Demonstrates thread lifecycle states (RUNNABLE, TIMED_WAITING, TERMINATED),
 * thread naming, execution tracing, and background transfer maintenance.
 */
public class ExchangeWorker implements Runnable {

    private final ExchangeService exchangeService;
    private final long intervalMillis;
    private volatile boolean running = false;
    private Thread workerThread;

    public ExchangeWorker(ExchangeService exchangeService, long intervalMillis) {
        this.exchangeService = exchangeService;
        this.intervalMillis = intervalMillis;
    }

    /**
     * Starts the worker on a named daemon background thread.
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        workerThread = new Thread(this, "ExchangeWorker-Thread");
        workerThread.setDaemon(true); // Daemon thread: does not block JVM exit
        workerThread.setPriority(Thread.NORM_PRIORITY);
        System.out.printf("[EXCHANGE-WORKER] Thread state before start: %s%n", workerThread.getState());
        workerThread.start();
        System.out.printf("[EXCHANGE-WORKER] Thread started: %s (Priority: %d, Daemon: %b)%n",
                workerThread.getName(), workerThread.getPriority(), workerThread.isDaemon());
    }

    /**
     * Stops the worker safely using the volatile running flag.
     */
    public synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    @Override
    public void run() {
        System.out.printf("[EXCHANGE-WORKER] [%s] Entered RUNNABLE state.%n", Thread.currentThread().getName());
        while (running) {
            try {
                processBackgroundTasks();
                // Enters TIMED_WAITING state
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                System.out.printf("[EXCHANGE-WORKER] [%s] Interrupted during sleep. Exiting loop.%n",
                        Thread.currentThread().getName());
                break;
            } catch (Exception e) {
                System.err.printf("[EXCHANGE-WORKER] Error in execution: %s%n", e.getMessage());
            }
        }
        System.out.printf("[EXCHANGE-WORKER] [%s] Terminating. Final state: %s%n",
                Thread.currentThread().getName(), Thread.currentThread().getState());
    }

    private void processBackgroundTasks() {
        // Automatically progress any INITIATED transfers to DISPATCHED
        List<StockTransfer> allTransfers = exchangeService.getAllTransfers();
        for (StockTransfer transfer : allTransfers) {
            if (transfer.getStatus() == StockTransfer.TransferStatus.INITIATED) {
                System.out.printf("[EXCHANGE-WORKER] Automatically dispatching transfer %s (%s)...%n",
                        transfer.getTransferId(), transfer.getMedicineName());
                exchangeService.updateTransferStatus(transfer.getTransferId(), StockTransfer.TransferStatus.DISPATCHED);
            }
        }
    }

    public Thread.State getThreadState() {
        return workerThread != null ? workerThread.getState() : Thread.State.NEW;
    }

    public boolean isRunning() {
        return running;
    }
}
