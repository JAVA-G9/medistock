package com.medistock.repository.memory;

import com.medistock.model.Pharmacy;
import com.medistock.model.User;
import com.medistock.repository.PharmacyRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of PharmacyRepository for Evaluation 1.
 * Owner: Member 1.
 */
public class InMemoryPharmacyRepository implements PharmacyRepository {

    private final Map<String, Pharmacy> pharmacies = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public InMemoryPharmacyRepository() {
        seedInitialData();
    }

    private void seedInitialData() {
        // Pre-populate with realistic licensed pharmacies
        Pharmacy p1 = new Pharmacy(
                "PHARM-001",
                "Apex Care Pharmacy",
                "DL-2024-APEX01",
                "124 Healthcare Boulevard, Suite A",
                "Metro City",
                "+1 (555) 234-5678",
                "contact@apexcare.com",
                142, 18, 5
        );

        Pharmacy p2 = new Pharmacy(
                "PHARM-002",
                "City Health Drugstore",
                "DL-2024-CITY02",
                "45 Commercial Square, Downtown",
                "Riverdale",
                "+1 (555) 345-6789",
                "orders@cityhealth.com",
                98, 12, 3
        );

        Pharmacy p3 = new Pharmacy(
                "PHARM-003",
                "GreenCross LifeSciences",
                "DL-2024-GCR03",
                "78 Botanical Way, West End",
                "Green Valley",
                "+1 (555) 456-7890",
                "support@greencrosspharma.com",
                210, 24, 8
        );

        pharmacies.put(p1.getId(), p1);
        pharmacies.put(p2.getId(), p2);
        pharmacies.put(p3.getId(), p3);

        // Pre-populate default user credentials
        User u1 = new User(
                "USER-001",
                p1.getId(),
                "admin",
                "admin123",
                "Dr. Marcus Vance",
                "marcus.vance@apexcare.com",
                "Chief Pharmacist"
        );

        User u2 = new User(
                "USER-002",
                p2.getId(),
                "citypharm",
                "city123",
                "Sarah Jenkins, PharmD",
                "sarah.j@cityhealth.com",
                "Inventory Director"
        );

        User u3 = new User(
                "USER-003",
                p3.getId(),
                "greencross",
                "green123",
                "Rajesh Patel",
                "rajesh.p@greencrosspharma.com",
                "Lead Pharmacist"
        );

        users.put(u1.getId(), u1);
        users.put(u2.getId(), u2);
        users.put(u3.getId(), u3);
    }

    @Override
    public Optional<Pharmacy> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(pharmacies.get(id));
    }

    @Override
    public Optional<Pharmacy> findByLicenseNumber(String licenseNumber) {
        if (licenseNumber == null) return Optional.empty();
        return pharmacies.values().stream()
                .filter(p -> p.getLicenseNumber() != null && p.getLicenseNumber().equalsIgnoreCase(licenseNumber.trim()))
                .findFirst();
    }

    @Override
    public List<Pharmacy> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(pharmacies.values()));
    }

    @Override
    public Pharmacy save(Pharmacy pharmacy) {
        if (pharmacy == null) throw new IllegalArgumentException("Pharmacy cannot be null");
        if (pharmacy.getId() == null || pharmacy.getId().isBlank()) {
            pharmacy.setId("PHARM-" + String.format("%03d", pharmacies.size() + 1));
        }
        pharmacies.put(pharmacy.getId(), pharmacy);
        return pharmacy;
    }

    @Override
    public void update(Pharmacy pharmacy) {
        if (pharmacy != null && pharmacy.getId() != null) {
            pharmacies.put(pharmacy.getId(), pharmacy);
        }
    }

    @Override
    public boolean deleteById(String id) {
        return id != null && pharmacies.remove(id) != null;
    }

    @Override
    public Optional<User> findUserById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        if (username == null) return Optional.empty();
        return users.values().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }

    @Override
    public List<User> findUsersByPharmacyId(String pharmacyId) {
        if (pharmacyId == null) return Collections.emptyList();
        List<User> result = new ArrayList<>();
        for (User u : users.values()) {
            if (pharmacyId.equals(u.getPharmacyId())) {
                result.add(u);
            }
        }
        return result;
    }

    @Override
    public User saveUser(User user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (user.getId() == null || user.getId().isBlank()) {
            user.setId("USER-" + String.format("%03d", users.size() + 1));
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public void updateUser(User user) {
        if (user != null && user.getId() != null) {
            users.put(user.getId(), user);
        }
    }
}
