package org.example.medistock.concurrency;

public class StockTest {

    public static void main(String[] args) {

        StockManager manager = new StockManager(10);

        InventoryWorker employee1 =
                new InventoryWorker(manager, 7, "Employee 1");

        InventoryWorker employee2 =
                new InventoryWorker(manager, 5, "Employee 2");

        employee1.start();
        employee2.start();

        try {
            employee1.join();
            employee2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Final stock: " + manager.getAvailableStock());
    }
}