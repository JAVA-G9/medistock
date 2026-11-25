package org.example.medistock.util;

import org.example.medistock.config.DatabaseConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance connection pool and JDBC connection provider for MySQL / TiDB Cloud.
 * Uses a dynamic connection pool with keep-alive validation and non-blocking borrowing.
 * Prevents UI freezes caused by repeated TLS/TCP handshakes over remote cloud connections.
 */
public class DBConnection {

    private static final int MAX_POOL_SIZE = 8;
    private static final BlockingQueue<Connection> pool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
    private static final AtomicInteger createdCount = new AtomicInteger(0);
    private static volatile boolean driverLoaded = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManager.setLoginTimeout(3); // 3 second max login timeout
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] MySQL Driver not found: " + e.getMessage());
        }
    }

    private static boolean isConnectionAlive(Connection c) {
        if (c == null) return false;
        try {
            return !c.isClosed() && c.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Obtains a pooled or freshly created MySQL connection wrapped in a smart proxy.
     * When the caller invokes con.close(), the underlying connection is recycled back into the pool.
     */
    public static Connection connectToMySql() {
        // 1. Try to obtain an idle, valid connection from the pool
        Connection raw = pool.poll();
        while (raw != null) {
            if (isConnectionAlive(raw)) {
                return wrapConnection(raw);
            }
            // Stale connection, close silently and decrement
            try { raw.close(); } catch (Exception ignored) {}
            createdCount.decrementAndGet();
            raw = pool.poll();
        }

        // 2. No idle connection available; create new one if within limit
        Connection newRaw = createRawConnection();
        if (newRaw != null) {
            return wrapConnection(newRaw);
        }

        // 3. Fallback: wait up to 1 second for any returning connection
        try {
            Connection waited = pool.poll(1000, TimeUnit.MILLISECONDS);
            if (isConnectionAlive(waited)) {
                return wrapConnection(waited);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        return null;
    }

    private static Connection createRawConnection() {
        try {
            if (!driverLoaded) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                driverLoaded = true;
            }

            String url = DatabaseConfig.getUrl();
            if (url != null) {
                if (!url.contains("connectTimeout")) {
                    url += (url.contains("?") ? "&" : "?") + "connectTimeout=3000&socketTimeout=4000&autoReconnect=true";
                }
            }

            DriverManager.setLoginTimeout(3);
            Connection con = DriverManager.getConnection(
                    url,
                    DatabaseConfig.getUsername(),
                    DatabaseConfig.getPassword()
            );

            createdCount.incrementAndGet();
            System.out.println("[DBConnection] Established fresh connection to database (total: " + createdCount.get() + ")");
            return con;
        } catch (Throwable e) {
            System.err.println("[DBConnection] Database connection error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Wraps raw java.sql.Connection in a Dynamic Proxy that intercepts close()
     * to return the connection to the pool rather than severing the network socket.
     */
    private static Connection wrapConnection(Connection rawCon) {
        return (Connection) Proxy.newProxyInstance(
                DBConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new InvocationHandler() {
                    private boolean isClosed = false;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();

                        if ("close".equals(methodName)) {
                            if (!isClosed) {
                                isClosed = true;
                                try {
                                    if (!rawCon.isClosed() && rawCon.isValid(1)) {
                                        if (!pool.offer(rawCon)) {
                                            // Pool full, physically close
                                            rawCon.close();
                                            createdCount.decrementAndGet();
                                        }
                                    } else {
                                        rawCon.close();
                                        createdCount.decrementAndGet();
                                    }
                                } catch (Exception e) {
                                    try { rawCon.close(); } catch (Exception ignored) {}
                                    createdCount.decrementAndGet();
                                }
                            }
                            return null;
                        }

                        if ("isClosed".equals(methodName)) {
                            return isClosed || rawCon.isClosed();
                        }

                        if (isClosed) {
                            throw new SQLException("Connection is closed.");
                        }

                        return method.invoke(rawCon, args);
                    }
                }
        );
    }

    /**
     * Closes all pooled connections on application shutdown.
     */
    public static synchronized void shutdownPool() {
        Connection con;
        while ((con = pool.poll()) != null) {
            try {
                con.close();
            } catch (Exception ignored) {}
        }
        createdCount.set(0);
    }
}
