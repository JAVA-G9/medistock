package com.medistock.repository;

import com.medistock.model.Pharmacy;
import com.medistock.model.User;
import com.medistock.repository.memory.InMemoryPharmacyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PharmacyRepositoryTest {

    private PharmacyRepository repository;

    @BeforeEach
    public void setUp() {
        repository = new InMemoryPharmacyRepository();
    }

    @Test
    public void testSeededPharmaciesExist() {
        List<Pharmacy> all = repository.findAll();
        assertTrue(all.size() >= 3, "Should have at least 3 seeded pharmacies");

        Optional<Pharmacy> apex = repository.findById("PHARM-001");
        assertTrue(apex.isPresent(), "Apex Care Pharmacy should exist");
        assertEquals("Apex Care Pharmacy", apex.get().getName());
        assertEquals("DL-2024-APEX01", apex.get().getLicenseNumber());
    }

    @Test
    public void testFindByLicenseNumber() {
        Optional<Pharmacy> found = repository.findByLicenseNumber("DL-2024-CITY02");
        assertTrue(found.isPresent());
        assertEquals("City Health Drugstore", found.get().getName());
    }

    @Test
    public void testSaveAndFindNewPharmacy() {
        Pharmacy p = new Pharmacy(
                null,
                "Sunset Pharmacy",
                "DL-2024-SUN99",
                "99 Coastal Way",
                "Westport",
                "+1 555-999-8888",
                "sunset@pharma.com"
        );

        Pharmacy saved = repository.save(p);
        assertNotNull(saved.getId());
        assertTrue(saved.getId().startsWith("PHARM-"));

        Optional<Pharmacy> retrieved = repository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Sunset Pharmacy", retrieved.get().getName());
    }

    @Test
    public void testFindUserByUsernameAndAuthenticate() {
        Optional<User> adminUser = repository.findUserByUsername("admin");
        assertTrue(adminUser.isPresent());
        assertTrue(adminUser.get().authenticate("admin123"));
        assertFalse(adminUser.get().authenticate("wrongpassword"));
    }
}
