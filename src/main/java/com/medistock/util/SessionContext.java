package com.medistock.util;

import com.medistock.model.Pharmacy;
import com.medistock.model.User;

/**
 * Thread-safe global session context for the currently logged-in user and pharmacy.
 * Owner: Member 1.
 */
public final class SessionContext {

    private static volatile User currentUser;
    private static volatile Pharmacy currentPharmacy;

    private SessionContext() {
    }

    public static synchronized void setSession(User user, Pharmacy pharmacy) {
        currentUser = user;
        currentPharmacy = pharmacy;
    }

    public static synchronized void clearSession() {
        currentUser = null;
        currentPharmacy = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static Pharmacy getCurrentPharmacy() {
        return currentPharmacy;
    }

    public static boolean isAuthenticated() {
        return currentUser != null && currentPharmacy != null;
    }
}
