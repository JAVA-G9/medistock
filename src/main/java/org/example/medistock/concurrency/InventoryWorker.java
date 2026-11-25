package org.example.medistock.concurrency;

public class InventoryWorker extends Thread {

    private final StockManager stockManager;
    private final int quantity;
    private final String employeeName;

    public InventoryWorker(
            StockManager stockManager,
            int quantity,
            String employeeName) {

        this.stockManager = stockManager;
        this.quantity = quantity;
        this.employeeName = employeeName;
    }

    @Override
    public void run() {
        stockManager.issueStock(quantity, employeeName);
    }
}