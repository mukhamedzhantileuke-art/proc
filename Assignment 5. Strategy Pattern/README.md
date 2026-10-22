# Assignment 5: Strategy Pattern in Online Exam Proctoring

- **Course:** Software Design Patterns (AITU)
- **Group:** SE-2531
- **Student:** Mukhamedzhan Tileuke
- **Theme:** 🎓 **Variant 12: Exam Proctoring** (Interchangeable Risk Scoring & Grade Curving Strategies)

---

## 1. Project Overview & Architecture
This project applies the **Strategy Design Pattern** (GoF) to solve two core algorithmic operations in an online exam proctoring system:
1. **Family 1 (Risk Assessment Strategy):** Evaluating session telemetry to compute a cheating suspicion score (`0.0` to `100.0`).
2. **Family 2 (Grading & Normalization Strategy):** Converting raw student answers into a normalized course grade (`0.0%` to `100.0%`).

Each family features:
* A strategy interface (`RiskScoringStrategy`, `GradingCurveStrategy`).
* Three interchangeable concrete implementations using distinct mathematical algorithms.
* A clean Context class without conditional logic (`RiskAssessmentContext`, `GradingContext`).
* An automatic Selector class containing environmental decision logic (`RiskStrategySelector`, `GradingStrategySelector`).
* Isolation from external change via an abstracted interface (`ThreatIntelligenceFeed`).

---

## 2. Single Responsibility Principle (SRP) Class Table (Max 25 words, no "if", "and", "or", "but")

| Class / Interface | Responsibility Statement (Strict SRP) |
| :--- | :--- |
| `RiskScoringStrategy` | Declares the behavioral contract for evaluating student exam risk scores. |
| `StrictBiometricRiskStrategy` | Calculates cheating risk scores exclusively from facial orientation tracking data. |
| `ProcessKeystrokeRiskStrategy` | Computes risk scores exclusively from forbidden background processes keyboard events. |
| `NetworkThreatAdaptiveRiskStrategy` | Computes risk scores by combining network telemetry with external intelligence. |
| `RiskAssessmentContext` | Maintains a reference to the active risk algorithm for client execution. |
| `RiskStrategySelector` | Resolves the appropriate risk algorithm based on exam delivery mode. |
| `ThreatIntelligenceFeed` | Declares an abstracted gateway for external network threat data queries. |
| `SimulatedThreatIntelligenceFeed` | Provides simulated proxy detection data for local development. |
| `GradingCurveStrategy` | Declares the behavioral contract for computing final exam percentage grades. |
| `RawPercentageGradingStrategy` | Converts correct answer counts directly to linear percentage scores. |
| `BellCurveGradingStrategy` | Normalizes final scores using statistical cohort z-score distributions. |
| `ProctoringPenaltyGradingStrategy` | Deducts weighted penalty points for verified proctoring anomalies. |
| `GradingContext` | Maintains a reference to the active grade calculation algorithm. |
| `GradingStrategySelector` | Resolves the appropriate grading algorithm based on course policy. |
| `ExamSessionTelemetry` | Encapsulates immutable telemetry captured during a proctored examination. |
| `RawExamSubmission` | Encapsulates immutable student answer counts with cohort statistics. |
| `LegacyMonolithicRiskCalculator` | Historical monolithic class calculating risk via hardcoded switch branches. |

---

## 3. Written Reflection: Why Strategy and NOT State or Template Method?

### Strategy vs. State:
* **Intent Difference:** The **Strategy pattern** is client-driven or context-configured: the algorithm is selected from the outside (either manually or by an external Selector) to solve a specific algorithmic problem. The strategy objects do not know about each other and never trigger transitions to another strategy.
* **Why NOT State:** The **State pattern** represents internal transitions of an object as its lifecycle evolves (e.g. `ExamSession` transitioning from `DRAFT` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `SUBMITTED`). In State, concrete states frequently know about other states and trigger transitions based on internal state changes. In our problem, evaluating a risk score via `StrictBiometric` does not represent a lifecycle phase change of the proctoring engine; it is simply an interchangeable algorithm calculating a formula.

### Strategy vs. Template Method:
* **Intent Difference:** **Template Method** relies on **inheritance** (`extends`): an abstract superclass defines the rigid skeleton of an algorithm in a `final` method and lets subclasses override specific individual steps.
* **Why NOT Template Method:** Template Method locks the algorithm to compile-time inheritance and causes combinatorial subclass explosion if algorithms change along multiple axes. In contrast, **Strategy** uses **object composition** (`has-a`): the context holds an interface reference. This allows algorithms to be swapped dynamically at runtime (`setStrategy()`) while the student is actively taking the exam (e.g., automatically tightening proctoring from standard to biometric monitoring when suspicious activity occurs), which is fundamentally impossible with compile-time Template Method.

---

## 4. Refactoring with Git History (Requirement 4)

### ❌ Before (Monolithic Anti-Pattern with `switch`):
Class: `LegacyMonolithicRiskCalculator.java`
```java
public double calculateRisk(String algorithmType, ExamSessionTelemetry telemetry) {
    switch (algorithmType.toUpperCase()) {
        case "BIOMETRIC":
            // 15 lines of gaze math
            return score;
        case "PROCESS":
            // 15 lines of keystroke math
            return score;
        case "NETWORK":
            // 20 lines of network threat math
            return score;
        default:
            throw new IllegalArgumentException();
    }
}
```
* **Violations:** Violates Open/Closed Principle (adding a new algorithm forces modifying and recompiling this class). Violates Single Responsibility Principle (one class knows all three distinct calculation logics).

### ✅ After (Refactored Strategy Pattern):
* Each algorithm is encapsulated into its own dedicated class implementing `RiskScoringStrategy`.
* The `RiskAssessmentContext` delegates purely through polymorphism:
```java
public double assessRisk(ExamSessionTelemetry telemetry) {
    return strategy.calculateRisk(telemetry);
}
```
* **Benefit:** Adding a new algorithm (e.g., `AudioAcousticRiskStrategy`) requires only creating a new class without modifying existing classes.
* **Equivalence Verification:** `RiskScoringStrategyTest.testRefactoringEquality()` programmatically proves that the refactored Strategy produces identical results to the legacy calculator for all test vectors.

---

## 5. Isolation from Change (Clean Code Ch. 10)
`NetworkThreatAdaptiveRiskStrategy` requires external data (threat reputation of the student's IP address). Following Clean Code Chapter 10, this dependency is hidden behind our own interface: `ThreatIntelligenceFeed`.
* In production: Connected to a real/simulated threat intelligence service (`SimulatedThreatIntelligenceFeed`).
* In unit tests: Replaced with `FakeThreatIntelligenceFeed`, ensuring fast, deterministic, repeatable unit tests without internet dependencies.

---

## 6. How to Run & Verify

### Command Line:
```bash
# Compile and run Demo
javac -d bin -sourcepath src/main/java $(Get-ChildItem -Recurse -Filter *.java src/main/java).FullName
java -cp bin proctoring.strategy.Main
```

### In IntelliJ IDEA:
1. Open this directory (`Assignment 5. Strategy Pattern`) in IntelliJ IDEA.
2. Run [`src/main/java/proctoring/strategy/Main.java`](file:///c:/Users/WW/Documents/antigravity/Courses/02_Software_Design_Patterns/Assignments/Assignment%205.%20Strategy%20Pattern/src/main/java/proctoring/strategy/Main.java) for the live demonstration.
3. Run all tests in `src/test/java` via JUnit 5.
