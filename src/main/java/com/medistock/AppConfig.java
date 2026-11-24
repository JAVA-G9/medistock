package com.medistock;

/**
 * Shared application configuration. Owner: Member 1.
 */
public final class AppConfig {

    public static final String APP_NAME = "MediStock";
    public static final String APP_TAGLINE = "Pharmacy-to-Pharmacy Surplus Stock & Expiry Exchange";
    public static final String VERSION = "1.0.0-SNAPSHOT";

    public static final double WINDOW_WIDTH = 1180;
    public static final double WINDOW_HEIGHT = 740;
    public static final double MIN_WIDTH = 980;
    public static final double MIN_HEIGHT = 640;

    // Resource paths (absolute, inside src/main/resources)
    public static final String VIEW_PATH = "/com/medistock/view/";
    public static final String LOGIN_VIEW = VIEW_PATH + "login-view.fxml";
    public static final String DASHBOARD_VIEW = VIEW_PATH + "dashboard-view.fxml";
    public static final String PROFILE_VIEW = VIEW_PATH + "profile-view.fxml";
    public static final String THREAD_MONITOR_VIEW = VIEW_PATH + "thread-monitor-view.fxml";
    public static final String PLACEHOLDER_VIEW = VIEW_PATH + "placeholder-view.fxml";
    public static final String INVENTORY_VIEW = "/org/example/medistock/view/InventoryV/InventoryView.fxml";
    public static final String MARKETPLACE_VIEW = "/org/example/medistock/view/exchange/marketplace-view.fxml";
    public static final String REQUESTS_VIEW = "/org/example/medistock/view/exchange/requests-view.fxml";
    public static final String EXPIRY_VIEW = "/org/example/medistock/view/expiry/expiry-view.fxml";
    public static final String NOTIFICATIONS_VIEW = "/org/example/medistock/view/notifications/notifications-view.fxml";
    public static final String REPORTS_VIEW = "/org/example/medistock/view/reports/reports-view.fxml";

    public static final String MAIN_CSS = "/com/medistock/css/main.css";

    private AppConfig() {
    }
}
