
package org.example.medistock.repository.notification;

import org.example.medistock.model.Notification;
import org.example.medistock.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repository layer for Notification persistence against MySQL (TiDB Cloud).
 *
 * Supports:
 * - Saving and retrieving notifications
 * - Marking notifications as read
 * - Deleting notifications
 * - Thread-safe in-memory caching
 * - Cache fallback when database operations fail
 *
 * Owned by Member 4 (Expiry, Notifications & Reports).
 */
public class NotificationRepository {

    private static NotificationRepository instance;

    private final List<Notification> cache = new CopyOnWriteArrayList<>();
    private final Map<String, Notification> cacheMap = new ConcurrentHashMap<>();

    private volatile boolean tablesChecked = false;

    private NotificationRepository() {
        initDatabase();
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }

        return instance;
    }

    /**
     * Creates the notification table if it does not already exist.
     */
    private synchronized void initDatabase() {

        if (tablesChecked) {
            return;
        }

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

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                System.err.println("[NotificationRepository] Database connection unavailable.");
                return;
            }

            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(sql);
                tablesChecked = true;

                System.out.println("[NotificationRepository] Notification table verified.");
            }

        } catch (SQLException e) {
            System.err.println("[NotificationRepository] Table initialization failed: "
                    + e.getMessage());
        }
    }

    /**
     * Adds or updates a notification in the cache.
     */
    private void updateCache(Notification notification) {

        if (notification == null || notification.getNotificationId() == null) {
            return;
        }

        String id = notification.getNotificationId();

        cacheMap.put(id, notification);

        cache.removeIf(existing ->
                id.equals(existing.getNotificationId()));

        cache.add(0, notification);
    }

    /**
     * Removes a notification from the cache.
     */
    private void removeFromCache(String notificationId) {

        if (notificationId == null) {
            return;
        }

        cacheMap.remove(notificationId);

        cache.removeIf(notification ->
                notificationId.equals(notification.getNotificationId()));
    }

    /**
     * Saves a notification to the database and cache.
     */
    public synchronized void save(Notification n) {

        if (n == null || n.getNotificationId() == null) {
            return;
        }

        updateCache(n);

        String sql = """
                INSERT INTO notification
                (notification_id, pharmacy_id, title, message,
                 type, priority, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    pharmacy_id = VALUES(pharmacy_id),
                    title = VALUES(title),
                    message = VALUES(message),
                    type = VALUES(type),
                    priority = VALUES(priority),
                    is_read = VALUES(is_read),
                    created_at = VALUES(created_at)
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                System.err.println("[NotificationRepository] Notification saved in cache only.");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, n.getNotificationId());
                ps.setString(2, n.getPharmacyId());
                ps.setString(3, n.getTitle());
                ps.setString(4, n.getMessage());
                ps.setString(5, n.getType().name());
                ps.setString(6, n.getPriority().name());
                ps.setBoolean(7, n.isRead());

                LocalDateTime createdAt = n.getCreatedAt() != null
                        ? n.getCreatedAt()
                        : LocalDateTime.now();

                ps.setTimestamp(8, Timestamp.valueOf(createdAt));

                ps.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("[NotificationRepository] Error saving notification: "
                    + e.getMessage());
        }
    }

    /**
     * Finds all notifications belonging to a pharmacy,
     * including notifications addressed to ALL pharmacies.
     */
    public List<Notification> findByPharmacyId(String pharmacyId) {

        List<Notification> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM notification
                WHERE pharmacy_id = ? OR pharmacy_id = 'ALL'
                ORDER BY created_at DESC
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                return getCachedNotifications(pharmacyId, false);
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, pharmacyId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        Notification n = mapResultSet(rs);
                        list.add(n);
                        updateCache(n);
                    }
                }
            }

            return list;

        } catch (SQLException e) {
            System.err.println("[NotificationRepository] Database query failed: "
                    + e.getMessage());

            return getCachedNotifications(pharmacyId, false);
        }
    }

    /**
     * Finds unread notifications belonging to a pharmacy,
     * including unread notifications addressed to ALL pharmacies.
     */
    public List<Notification> findUnreadByPharmacyId(String pharmacyId) {

        List<Notification> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM notification
                WHERE (pharmacy_id = ? OR pharmacy_id = 'ALL')
                AND is_read = FALSE
                ORDER BY created_at DESC
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                return getCachedNotifications(pharmacyId, true);
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, pharmacyId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        Notification n = mapResultSet(rs);
                        list.add(n);
                        updateCache(n);
                    }
                }
            }

            return list;

        } catch (SQLException e) {
            System.err.println("[NotificationRepository] Unread query failed: "
                    + e.getMessage());

            return getCachedNotifications(pharmacyId, true);
        }
    }

    /**
     * Retrieves matching notifications from the in-memory cache.
     */
    private List<Notification> getCachedNotifications(
            String pharmacyId,
            boolean unreadOnly) {

        List<Notification> list = new ArrayList<>();

        for (Notification n : cache) {

            boolean belongsToPharmacy =
                    pharmacyId.equals(n.getPharmacyId())
                            || "ALL".equalsIgnoreCase(n.getPharmacyId());

            boolean isUnread = !n.isRead();

            if (belongsToPharmacy && (!unreadOnly || isUnread)) {
                list.add(n);
            }
        }

        return list;
    }

    /**
     * Marks one pharmacy-specific notification as read.
     */
    public void markAsRead(String notificationId) {

        if (notificationId == null) {
            return;
        }

        Notification n = cacheMap.get(notificationId);

        if (n != null && !"ALL".equalsIgnoreCase(n.getPharmacyId())) {
            n.setRead(true);
        }

        String sql = """
                UPDATE notification
                SET is_read = TRUE
                WHERE notification_id = ?
                AND pharmacy_id <> 'ALL'
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, notificationId);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("[NotificationRepository] Failed to mark as read: "
                    + e.getMessage());
        }
    }

    /**
     * Marks all notifications belonging to one pharmacy as read.
     * Shared ALL notifications are not modified.
     */
    public void markAllAsRead(String pharmacyId) {

        for (Notification n : cache) {

            if (pharmacyId.equals(n.getPharmacyId())) {
                n.setRead(true);
            }
        }

        String sql = """
                UPDATE notification
                SET is_read = TRUE
                WHERE pharmacy_id = ?
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, pharmacyId);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("[NotificationRepository] Failed to mark all as read: "
                    + e.getMessage());
        }
    }

    /**
     * Deletes one notification.
     */
    public void delete(String notificationId) {

        if (notificationId == null) {
            return;
        }

        String sql = """
                DELETE FROM notification
                WHERE notification_id = ?
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                removeFromCache(notificationId);
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, notificationId);
                ps.executeUpdate();

                removeFromCache(notificationId);
            }

        } catch (SQLException e) {
            System.err.println("[NotificationRepository] Failed to delete notification: "
                    + e.getMessage());
        }
    }

    /**
     * Clears notifications belonging to one pharmacy only.
     * Notifications addressed to ALL pharmacies are preserved.
     */
    public void clearAll(String pharmacyId) {

        String sql = """
                DELETE FROM notification
                WHERE pharmacy_id = ?
                """;

        try (Connection con = DBConnection.connectToMySql()) {

            if (con == null) {
                cache.removeIf(n -> pharmacyId.equals(n.getPharmacyId()));
                cacheMap.entrySet().removeIf(entry ->
                        pharmacyId.equals(entry.getValue().getPharmacyId()));
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, pharmacyId);
                ps.executeUpdate();

                cache.removeIf(n -> pharmacyId.equals(n.getPharmacyId()));

                cacheMap.entrySet().removeIf(entry ->
                        pharmacyId.equals(entry.getValue().getPharmacyId()));
            }

        } catch (SQLException e) {
            System.err.println("[NotificationRepository] Failed to clear notifications: "
                    + e.getMessage());
        }
    }

    /**
     * Converts a database result row into a Notification object.
     */
    private Notification mapResultSet(ResultSet rs) throws SQLException {

        Notification n = new Notification();

        n.setNotificationId(rs.getString("notification_id"));
        n.setPharmacyId(rs.getString("pharmacy_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));

        n.setType(
                Notification.NotificationType.valueOf(
                        rs.getString("type").toUpperCase()
                )
        );

        n.setPriority(
                Notification.Priority.valueOf(
                        rs.getString("priority").toUpperCase()
                )
        );

        n.setRead(rs.getBoolean("is_read"));

        Timestamp timestamp = rs.getTimestamp("created_at");

        n.setCreatedAt(
                timestamp != null
                        ? timestamp.toLocalDateTime()
                        : LocalDateTime.now()
        );

        return n;
    }
}
