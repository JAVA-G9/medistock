package org.example.medistock.concurrency;

public class StockManager {

    private int availableStock;

    public StockManager(int availableStock) {
        this.availableStock = availableStock;
    }

    // Intentionally unsafe: demonstrates a race condition
    public synchronized void issueStock(int quantity, String employee) {

        System.out.println(employee + " is checking stock...");

        int currentStock = availableStock;

        System.out.println(employee + " sees stock: " + currentStock);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (currentStock >= quantity) {
            availableStock = currentStock - quantity;

            System.out.println(employee + " issued " + quantity + " units.");
            System.out.println("Remaining stock: " + availableStock);
        } else {
            System.out.println(employee + ": Insufficient stock!");
        }
    }

    public int getAvailableStock() {
        return availableStock;
    }
}