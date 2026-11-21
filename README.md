# MediStock — Pharmacy-to-Pharmacy Surplus & Expiry Stock Exchange

**Stack:** JavaFX 21 • Core Java (Java 21) • OOP • Multithreading • JDBC  
**Evaluation:** Phase 1 (JavaFX Prototype + Multithreading Concurrency Architecture)

---

## 1. Member 1 Deliverables Summary

As **Member 1**, the following components, architecture, and multithreading modules are fully built and integrated in the `MediStock` folder:

### A. JavaFX Screens & Controllers
1. **Authentication Screen (`login-view.fxml` & `LoginController`):**
   - User login with validation and session management.
   - New pharmacy and lead pharmacist registration form.
   - One-click quick demo accounts for rapid evaluation grading (`Apex Care`, `City Health`, `GreenCross`).
2. **Main Dashboard (`dashboard-view.fxml` & `DashboardController`):**
   - Hero welcome banner displaying current licensed pharmacy credentials and municipal branch.
   - 4 Live KPI metric cards: Total Medicine Items, Near-Expiry Batches (<= 30 days), Active Listings, and Live Background Concurrency Threads.
   - Quick action bar to spawn background report workers, toggle background daemon monitor, and open profile.
   - Connected Partner Pharmacies Network table.
   - Live Concurrency & Background Execution Activity stream.
   - Navigation sidebar with access to Member 1 screens and architectural placeholder views for Members 2, 3, and 4.
3. **Pharmacy Facility Profile (`profile-view.fxml` & `ProfileController`):**
   - View and update drug license number, pharmacy physical address, municipal city, phone, and official email.
   - View and update authorized pharmacist profile, designation, and password.
4. **Concurrency & Thread Monitoring Lab (`thread-monitor-view.fxml` & `ThreadMonitorController`):**
   - Live inspection table for all tracked threads: Thread ID, Name, Priority, Daemon flag, Current State, and Alive status.
   - Interactive button to **Stage All 6 Thread States** (`NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED`).
   - Daemon thread controller: Start and stop `ExpiryMonitorThread`.
   - Priority worker launchers: Spawn `ReportWorker` with `MIN_PRIORITY` (1), `NORM_PRIORITY` (5), or `MAX_PRIORITY` (10).
   - Real-time Execution Trace Stream table with message clearing.
5. **Team Integration Placeholders (`placeholder-view.fxml` & `PlaceholderController`):**
   - Module boundaries for Member 2 (Inventory), Member 3 (Marketplace & Requests), and Member 4 (Expiry & Event Queue).

### B. Core Java, OOP & Persistence
- **Models:** `Pharmacy`, `User`.
- **Session:** Thread-safe `SessionContext` for active credentials and branch data.
- **Repository:** `PharmacyRepository` interface and thread-safe `InMemoryPharmacyRepository` with pre-seeded demonstration data.
- **Service:** `PharmacyService` with login, registration, validation, and profile updates.

### C. Multithreading Syllabus Implementation
- **Thread Lifecycle (6 States):** Observed and demonstrable via `ThreadManager.demonstrateAllSixStates()`.
- **Daemon Threads:** `ExpiryMonitorThread` explicitly configured with `setDaemon(true)` for periodic background sweeps.
- **Thread Priorities:** `ReportWorker` with priority manipulation (`Thread.MIN_PRIORITY` to `Thread.MAX_PRIORITY`).
- **Thread Naming & Execution Tracing:** Central `ThreadLogger` recording timestamp, thread name, priority, state, and message.
- **Clean Shutdown:** Application shutdown hook in `MainApp` terminating all background threads gracefully.

---

## 2. Pre-Configured Demo Accounts

| Pharmacy Facility | Drug License # | Username | Password | Role |
| :--- | :--- | :--- | :--- | :--- |
| **Apex Care Pharmacy** | DL-2024-APEX01 | `admin` | `admin123` | Chief Pharmacist |
| **City Health Drugstore** | DL-2024-CITY02 | `citypharm` | `city123` | Inventory Director |
| **GreenCross LifeSciences** | DL-2024-GCR03 | `greencross` | `green123` | Lead Pharmacist |

*(You can also click the quick-login buttons on the Sign In screen to log in instantly).*

---

## 3. How to Run

### Requirements
- **Java 21** or later (e.g., Eclipse Adoptium OpenJDK 21 / 25).
- Maven wrapper is included in the project directory (`mvnw.cmd` / `mvnw`).

### Launch the Application
```powershell
# In PowerShell:
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
cd MediStock
.\mvnw.cmd javafx:run -s settings.xml
```

Or from your IDE (IntelliJ IDEA / Eclipse / VS Code):
- Open `MediStock/pom.xml`.
- Run `com.medistock.AppLauncher` or `com.medistock.MainApp`.

### Run Automated Verification Suite
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
cd MediStock
.\mvnw.cmd test-compile -s settings.xml
& "$env:JAVA_HOME\bin\java.exe" --module-path "target\classes;C:\Users\OMEN\.m2\repository\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6.jar;C:\Users\OMEN\.m2\repository\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar;C:\Users\OMEN\.m2\repository\org\openjfx\javafx-graphics\21.0.6\javafx-graphics-21.0.6.jar;C:\Users\OMEN\.m2\repository\org\openjfx\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar;C:\Users\OMEN\.m2\repository\org\openjfx\javafx-base\21.0.6\javafx-base-21.0.6.jar;C:\Users\OMEN\.m2\repository\org\openjfx\javafx-base\21.0.6\javafx-base-21.0.6-win.jar;C:\Users\OMEN\.m2\repository\org\openjfx\javafx-fxml\21.0.6\javafx-fxml-21.0.6.jar;C:\Users\OMEN\.m2\repository\org\openjfx\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar" -m com.medistock/com.medistock.util.MediStockVerification
```
