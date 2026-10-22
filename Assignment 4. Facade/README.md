# Assignment 4: Facade Over a Three-Stage Chain with Persistence & Compensation

- **Course:** Software Design Patterns (AITU)
- **Group:** SE-2531
- **Student:** Mukhamedzhan Tileuke
- **Assigned Domain:** 🎓 **Variant 12: Exam Proctoring**

---

## 1. Project Overview & Subsystem Architecture
This project implements a durable, fault-tolerant orchestration layer using the **Facade Pattern** over three completely decoupled subsystems:

```text
[Client] ---> [ExamProctoringFacade]
                     │
         ┌───────────┼───────────┐
         ▼           ▼           ▼
    Subsystem A  Subsystem B  Subsystem C
    (Intake)    (Monitoring)  (Grading)
         │           │           │
         ▼           ▼           ▼
    [data/intake] [data/mon]  [data/grad]
                     ▲
         [data/chains/ (Durable Journal)]
```

### Subsystems Breakdown:
1. **Subsystem A (`proctoring.intake`):**
   * *Responsibility:* Student identity verification and exam slot reservation.
   * *Business Logic:* `BiometricValidator` (validates document format, checks suspension flags).
   * *Entity & State:* `VerificationRecord` (`PENDING`, `VERIFIED_RESERVED`, `REVOKED`).
   * *Persistence:* `IntakePersistenceGateway` writes/reads directly to `data/intake/`.
2. **Subsystem B (`proctoring.monitoring`):**
   * *Responsibility:* Live video stream integrity and anti-cheat hardware checks.
   * *Business Logic:* `HardwareIntegrityRule` (forbids multi-monitor setups, verifies active webcam feed, detects cheat processes).
   * *Entity & State:* `MonitoringSession` (`INITIALIZED`, `PASSED_CLEAN`, `FLAGGED_VIOLATION`, `ROLLBACK_INVALIDATED`).
   * *Persistence:* `MonitoringPersistenceGateway` writes/reads directly to `data/monitoring/`.
3. **Subsystem C (`proctoring.grading`):**
   * *Responsibility:* Answer packet evaluation and official transcript recording.
   * *Business Logic:* `SubmissionIntegrityEngine` (cryptographic SHA-256 checksum verification, score calculation, letter grade assignment).
   * *Entity & State:* `ExamTranscriptRecord` (`AWAITING_EVALUATION`, `CERTIFIED_RECORDED`, `INTEGRITY_REJECTED`).
   * *Persistence:* `GradingPersistenceGateway` writes/reads directly to `data/grading/`.

### Two Facades:
* **`ExamProctoringFacade`:** Primary client workflow driving the sequential chain $A \rightarrow B \rightarrow C$, handling both compensation depths.
* **`ProctoringAdminFacade`:** Secondary administrative facade for state audit, manual compensation, purging storage, and recovering broken chains after application crash/restart.

---

## 2. Principle of Least Knowledge (Law of Demeter) - Section 3.5

### ❌ BEFORE (Without Facade - High Coupling & "Train Wreck"):
The client had to instantiate, coordinate, and navigate deep object chains across all three unrelated subsystems:
```java
// Client violates Law of Demeter: talks to friends of friends
IntakeService intake = university.getIntakeSystem().getService();
VerificationRecord record = intake.verifyAndReserveSlot(studentId, examCode, doc);
if (record.getStatus() == VerificationStatus.VERIFIED_RESERVED) {
    MonitoringService monitor = university.getSecurityDept().getMonitorService();
    MonitoringSession session = monitor.startAndCompleteMonitoring(studentId, examCode, 1, true, "chrome.exe");
    if (session.getStatus() == MonitoringStatus.PASSED_CLEAN) {
        GradingService grader = university.getAcademicOffice().getGradingEngine();
        ExamTranscriptRecord transcript = grader.evaluateAndCertify(studentId, examCode, answers, checksum);
        // Client manually handles rollbacks if any step fails!
    }
}
```

### ✅ AFTER (With Facade - Clean Encapsulation):
The client talks **only to its immediate friend (`ExamProctoringFacade`)** via a single method:
```java
ExamResultSummary result = proctoringFacade.conductExamSession(
    studentId, examCode, documentNumber,
    activeDisplays, webcamActive, runningProcess,
    answersPayload, expectedChecksum
);
```
Subsystems A, B, and C remain completely decoupled and unaware of each other.

---

## 3. Written Reflection: Facade vs. Adapter - Section 3.10

### Why does this problem call for a Facade rather than an Adapter?
* **Intent of Adapter:** An Adapter exists to resolve an **interface incompatibility between two specific classes**. It acts as a wrapper around an existing class (Adaptee) to make it adhere to a Target interface expected by a client, without altering the underlying behavior or orchestrating multiple steps.
* **Intent of Facade:** A Facade exists to define a **higher-level, simplified interface over an entire complex subsystem or set of subsystems**. In our Exam Proctoring domain, Subsystems A, B, and C are not incompatible interfaces trying to masquerade as one another. Instead, they represent three completely distinct business stages (Intake $\rightarrow$ Monitoring $\rightarrow$ Grading). 
* The core challenge is **orchestrating a multi-stage transaction with durable state and two-tier rollback logic**. An Adapter cannot solve workflow coordination, whereas a Facade encapsulates the sequential dependency, error detection, and compensation dispatch.

---

## 4. Cross-Subsystem Consistency & Compensation Depths - Section 3.4

1. **Compensation Depth 1 (Step B fails after A committed):**
   * Step A commits: Student verified, exam slot locked (`VERIFIED_RESERVED`).
   * Step B fails: Hardware integrity detects multi-monitor anomaly (`FLAGGED_VIOLATION`).
   * *Compensation:* Facade catches error, sets journal to `COMPENSATING_A`, invokes `intakeService.compensateReservation(...)` (marks slot `REVOKED`), and records `COMPENSATED_A`.
2. **Compensation Depth 2 (Step C fails after A and B committed):**
   * Steps A and B commit successfully (`VERIFIED_RESERVED`, `PASSED_CLEAN`).
   * Step C fails: Submission packet has corrupted SHA-256 checksum (`INTEGRITY_REJECTED`).
   * *Two-Step Rollback:* Facade catches error, sets journal to `COMPENSATING_B_A`.
     * Rollback Step 1: Unwinds B $\rightarrow$ marks monitoring session as `ROLLBACK_INVALIDATED`.
     * Rollback Step 2: Unwinds A $\rightarrow$ marks intake slot as `REVOKED`.
     * Journal committed to disk as `COMPENSATED_B_A`.

---

## 5. Answers to the 5 Examiner Sample Questions

### Question 1: Run chain live so step B fails right after A committed, and show persistence store.
* **How to run:** In the CLI, press `2` (or run automated demo).
* **What is demonstrated:** Step A logs success. Step B detects 2 monitors and throws `MonitoringViolationException`. Facade compensates Step A.
* **Proof in store:** In `data/intake/`, `VerificationRecord` shows `status=REVOKED`. In `data/chains/`, journal shows `status=COMPENSATED_A`. Subsystem C has no records on disk.

### Question 2: Stop and restart application in middle of chain, and show resumption from disk store.
* **How to run:** In the CLI, press `4`.
* **What is demonstrated:** A crash is simulated after Stage A writes to disk. In-memory objects are completely deleted (`null`). A brand new application instance starts, reads `data/chains/` from disk, identifies unfinished session, and executes safe rollback/recovery.

### Question 3: Explain using own A, B, C classes why Facade and not Adapter.
* **Answer:** Subsystems A (`IntakeService`), B (`MonitoringService`), and C (`GradingService`) are separate functional stages, not incompatible interfaces. Facade coordinates the workflow and unwinds compensations. Adapter only translates one interface to another.

### Question 4: Write and immediately run one new unit test for a scenario not in test suite.
* **Answer:** Use the fluent DSL in `ExamProctoringFacadeTest`:
```java
@Test
void testForbiddenProcessDetection() {
    assertThrows(ProctoringChainException.class, () ->
        givenExamScenario("S9999", "CS301")
            .withForbiddenProcess("discord.exe")
            .whenExecutingChain()
    );
}
```

### Question 5: Find a spot where Principle of Least Knowledge could be violated and how facade avoids it.
* **Answer:** Without the facade, the client would directly access `intakeService.verifyAndReserveSlot()`, inspect the record, call `monitoringService`, and manually execute rollback calls across different subsystems (see Section 2 above). With `ExamProctoringFacade`, the client has only **one friend** and makes **one call**.

---

## 6. How to Run & Verify

### Command Line:
```bash
# 1. Compile project
javac -d bin -sourcepath src/main/java $(Get-ChildItem -Recurse -Filter *.java src/main/java).FullName

# 2. Run Live Interactive Menu
java -cp bin proctoring.Main

# 3. Run Automated Demonstration (for immediate non-interactive verification)
java -cp bin proctoring.Main --demo
```

### Maven & JaCoCo Coverage Report:
```bash
# Run tests and generate JaCoCo coverage report:
mvn clean test

# View HTML coverage report:
# Report is generated at: target/site/jacoco/index.html
```

### In IntelliJ IDEA:
1. Open this directory (`Assignment 4. Facade`) in IntelliJ IDEA.
2. In the Maven Tool Window (on the right), expand `Lifecycle` -> double-click `test`.
3. JaCoCo will automatically calculate line coverage and generate the report at `target/site/jacoco/index.html`.
4. Alternatively, right-click `src/test/java` in the project tree and select **"Run 'All Tests' with Coverage"** to view live line-by-line coverage directly inside IntelliJ IDEA.

