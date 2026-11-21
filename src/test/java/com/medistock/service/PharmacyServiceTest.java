package com.medistock.service;

import com.medistock.model.Pharmacy;
import com.medistock.model.User;
import com.medistock.repository.memory.InMemoryPharmacyRepository;
import com.medistock.util.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PharmacyServiceTest {

    private PharmacyService pharmacyService;

    @BeforeEach
    public void setUp() {
        SessionContext.clearSession();
        pharmacyService = new PharmacyService(new InMemoryPharmacyRepository());
    }

    @Test
    public void testSuccessfulLoginSetsSession() throws Exception {
        User user = pharmacyService.login("admin", "admin123");
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
        assertTrue(SessionContext.isAuthenticated());
        assertEquals("PHARM-001", SessionContext.getCurrentPharmacy().getId());
    }

    @Test
    public void testInvalidPasswordThrowsException() {
        assertThrows(PharmacyService.AuthenticationException.class, () -> {
            pharmacyService.login("admin", "wrongpass");
        });
        assertFalse(SessionContext.isAuthenticated());
    }

    @Test
    public void testRegisterNewPharmacyAndUser() throws Exception {
        Pharmacy pharmacy = new Pharmacy(
                null,
                "St. Jude Pharmacy",
                "DL-2024-STJ01",
                "500 Hope Lane",
                "Oakland",
                "+1 555-123-9999",
                "stjude@care.com"
        );

        User user = new User(
                null,
                null,
                "stjude_admin",
                "securePass123",
                "Dr. John Doe",
                "stjude@care.com",
                "Chief Pharmacist"
        );

        User registeredUser = pharmacyService.register(pharmacy, user);
        assertNotNull(registeredUser.getId());
        assertEquals("stjude_admin", registeredUser.getUsername());
        assertTrue(SessionContext.isAuthenticated());
        assertEquals("St. Jude Pharmacy", SessionContext.getCurrentPharmacy().getName());
    }

    @Test
    public void testDuplicateLicenseRejected() {
        Pharmacy pharmacy = new Pharmacy(
                null,
                "Duplicate Pharmacy",
                "DL-2024-APEX01", // Already exists
                "123 Street",
                "City",
                "123",
                "a@b.com"
        );

        User user = new User(
                null, null, "unique_user", "pass123", "Name", "email@b.com", "Role"
        );

        assertThrows(PharmacyService.ValidationException.class, () -> {
            pharmacyService.register(pharmacy, user);
        });
    }
}
