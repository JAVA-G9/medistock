package org.example.medistock.repository.notification;

import org.example.medistock.model.Notification;
import org.example.medistock.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repository layer for Notification persistence against MySQL (TiDB Cloud).
 * Supports thread-safe operations, prepared statements, and fallback in-memory caching.
 * Owned by Member 4 (Expiry, Notifications & Reports).
 */
public class NotificationRepository {

    private static NotificationRepository instance;
    private final List<Notification> cache = new CopyOnWriteArrayList<>();
    private final Map<String, Notification> cacheMap = new ConcurrentHashMap<>();
    private volatile long lastFetchTime = 0;
    private static final long CACHE_TTL_MS = 300_000; // 5-minute memory cache for 0ms instant tab switching
    private boolean tablesChecked = false;

    public NotificationRepository() {
        initDatabase();
        // No seed data - all notifications are fetched live from TiDB Cloud
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    /**
     * Initializes database tables if not already present.
     */
    private synchronized void initDatabase() {
        if (tablesChecked) return;
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                try (Statement stmt = con.createStatement()) {
                    String sql = """
                        CREATE TABLE IF NOT EXISTS notification (
                            notification_id VARCHAR(100) PRIMARY KEY,
                            pharmacy_id VARCHAR(50) NOT NULL,
                            title VARCHAR(255) NOT NULL,
                            message TEXT NOT NULL,
                            type VARCHAR(50) NOT NULL,
                            priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
                            is_read BOOLEAN NOT NULL DEFAULT FALSE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            INDEX idx_pharmacy_read (pharmacy_id, is_read)
                        )
                    """;
                    stmt.executeUpdate(sql);
                    System.out.println("[NotificationRepository] Table 'notification' verified/initialized.");
                    tablesChecked = true;
                }
            }
        } catch (Exception e) {
            System.err.println("[NotificationRepository] Failed to initialize table: " + e.getMessage());
        }
    }

    public synchronized void save(Notification n) {
        if (n == null) return;
        cacheMap.put(n.getNotificationId(), n);
        cache.removeIf(item -> item.getNotificationId().equals(n.getNotificationId()));
        cache.add(0, n); // newest first

        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = """
                    INSERT INTO notification (notification_id, pharmacy_id, title, message, type, priority, is_read, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE is_read = VALUES(is_read), priority = VALUES(priority)
                """;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, n.getNotificationId());
                    ps.setString(2, n.getPharmacyId());
                    ps.setString(3, n.getTitle());
                    ps.setString(4, n.getMessage());
                    ps.setString(5, n.getType().name());
                    ps.setString(6, n.getPriority().name());
                    ps.setBoolean(7, n.isRead());
                    ps.setTimestamp(8, Timestamp.valueOf(n.getCreatedAt() != null ? n.getCreatedAt() : LocalDateTime.now()));
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("[NotificationRepository] Error saving notification to DB: " + e.getMessage());
        }
    }

    public List<Notification> findByPharmacyId(String pharmacyId) {
        return findByPharmacyId(pharmacyId, false);
    }

    /**
     * Instantaneous memory-only read without touching network or DB.
     */
    public List<Notification> getCachedNotifications(String pharmacyId) {
        List<Notification> list = new ArrayList<>();
        for (Notification n : cache) {
            if (n.getPharmacyId().equals(pharmacyId) || "ALL".equalsIgnoreCase(n.getPharmacyId())) {
                list.add(n);
            }
        }
        return list;
    }

    public int getCachedUnreadCount(String pharmacyId) {
        int count = 0;
        for (Notification n : cache) {
            if ((n.getPharmacyId().equals(pharmacyId) || "ALL".equalsIgnoreCase(n.getPharmacyId())) && !n.isRead()) {
                count++;
            }
        }
        return count;
    }

    public List<Notification> findByPharmacyId(String pharmacyId, boolean forceRefresh) {
        long now = System.currentTimeMillis();
        if (!forceRefresh && !cache.isEmpty() && (now - lastFetchTime < CACHE_TTL_MS)) {
            List<Notification> list = new ArrayList<>();
            for (Notification n : cache) {
                if (n.getPharmacyId().equals(pharmacyId) || "ALL".equalsIgnoreCase(n.getPharmacyId())) {
                    list.add(n);
                }
            }
            return list;
        }

        List<Notification> list = new ArrayList<>();
        try (Connection con = DBConnection.connectToMySql()) {
            if (con != null) {
                String sql = "SELECT * FROM notification WHERE pharmacy_id = ? OR pharmacy_id = 'ALL' ORDER BY created_at DESC";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, pharmacyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Notification n = mapResultSet(rs);
                            list.add(n);
                            cacheMap.put(n.getNotificationId(), n);
                        }
                    }
                }
                if (!list.isEmpty()) {
                    cache.clear();
                    cache.addAll(list);
                    lastFetchTime = System.currentTimeMillis();
                    return list;
                }
            }
        } catch (Exception e) {
            System.err.println("[NotificationRepository] DB query failed, falling back to cache: " + e.getMessage());
        }

        // Fallback to cache
        for (Notification n : cache) {
            if (n.getPharmacyId().equals(pharmacyId) || "ALL".equalsIgnoreCase(n.getPharmacyId())) {
                list.add(n);
            }
        }
        return list;
    }

    public List<Notification> findUnreadByPharmacyId(String pharmacyId) {
        List<Notification> all = findByPharmacyId(pharmacyId, false);
        List<Notification> unread = new ArrayList<>();
        for (Notification n : all) {
            if (!n.isRead()) {
                unread.add(n);
            }
        }
        return unread;
    }

    public void markAsRead(String notificationId) {
        Notification n = cacheMap.get(notificationId);
        if (n != null) {
            n.setRead(true);
        }
        for (Notification item : cache) {
            if (item.getNotificationId().equals(notificationId)) {
                item.setRead(true);
            }
        }

        // Run DB update once asynchronously in background so UI thread never freezes
        new Thread(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = "UPDATE notification SET is_read = TRUE WHERE notification_id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, notificationId);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                System.err.println("[NotificationRepository] Failed to mark as read in DB: " + e.getMessage());
            }
        }, "NotifMarkRead-Worker").start();
    }

    public void markAllAsRead(String pharmacyId) {
        for (Notification n : cache) {
            if (n.getPharmacyId().equals(pharmacyId) || "ALL".equalsIgnoreCase(n.getPharmacyId())) {
                n.setRead(true);
            }
        }

        // Run DB update once asynchronously in background
        new Thread(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = "UPDATE notification SET is_read = TRUE WHERE pharmacy_id = ? OR pharmacy_id = 'ALL'";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, pharmacyId);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                System.err.println("[NotificationRepository] Failed to mark all as read: " + e.getMessage());
            }
        }, "NotifMarkAllRead-Worker").start();
    }

    public void delete(String notificationId) {
        cacheMap.remove(notificationId);
        cache.removeIf(n -> n.getNotificationId().equals(notificationId));

        // Run DB delete once asynchronously in background
        new Thread(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = "DELETE FROM notification WHERE notification_id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, notificationId);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                System.err.println("[NotificationRepository] Failed to delete notification: " + e.getMessage());
            }
        }, "NotifDelete-Worker").start();
    }

    public void clearAll(String pharmacyId) {
        cache.removeIf(n -> n.getPharmacyId().equals(pharmacyId) || "ALL".equalsIgnoreCase(n.getPharmacyId()));

        // Run DB clear once asynchronously in background
        new Thread(() -> {
            try (Connection con = DBConnection.connectToMySql()) {
                if (con != null) {
                    String sql = "DELETE FROM notification WHERE pharmacy_id = ? OR pharmacy_id = 'ALL'";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, pharmacyId);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                System.err.println("[NotificationRepository] Failed to clear notifications: " + e.getMessage());
            }
        }, "NotifClearAll-Worker").start();
    }

    private Notification mapResultSet(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getString("notification_id"));
        n.setPharmacyId(rs.getString("pharmacy_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setType(Notification.NotificationType.valueOf(rs.getString("type")));
        n.setPriority(Notification.Priority.valueOf(rs.getString("priority")));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_at");
        n.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        return n;
    }
}
