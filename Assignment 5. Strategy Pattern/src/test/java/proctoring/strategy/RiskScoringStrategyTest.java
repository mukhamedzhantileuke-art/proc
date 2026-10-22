package proctoring.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import proctoring.strategy.external.FakeThreatIntelligenceFeed;
import proctoring.strategy.legacy.LegacyMonolithicRiskCalculator;
import proctoring.strategy.model.ExamDeliveryMode;
import proctoring.strategy.model.ExamSessionTelemetry;
import proctoring.strategy.risk.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RiskScoringStrategyTest {

    private FakeThreatIntelligenceFeed fakeFeed;

    @BeforeEach
    void setUp() {
        fakeFeed = new FakeThreatIntelligenceFeed();
    }

    @Test
    @DisplayName("Biometric strategy correctly calculates gaze and audio penalties")
    void testStrictBiometricRiskStrategy() {
        RiskScoringStrategy strategy = new StrictBiometricRiskStrategy();
        ExamSessionTelemetry telemetry = new ExamSessionTelemetry(
                "S1", 10.0, 1, 2, 0, 0, 0, "127.0.0.1"
        );
        // Gaze: 10 * 2.5 = 25; Face: 1 * 25 = 25; Audio: 2 * 8 = 16 => Total 66.0
        assertEquals(66.0, strategy.calculateRisk(telemetry), 0.001);
    }

    @Test
    @DisplayName("Process monitoring strategy correctly computes application anomalies")
    void testProcessKeystrokeRiskStrategy() {
        RiskScoringStrategy strategy = new ProcessKeystrokeRiskStrategy();
        ExamSessionTelemetry telemetry = new ExamSessionTelemetry(
                "S2", 0.0, 0, 0, 3, 2, 1, "127.0.0.1"
        );
        // Apps: 1 * 45 = 45; CopyPaste: 2 * 15 = 30; Tabs: 3 * 10 = 30 => Total 105.0 clamped to 100.0
        assertEquals(100.0, strategy.calculateRisk(telemetry), 0.001);
    }

    @Test
    @DisplayName("Network adaptive strategy incorporates external threat feed data")
    void testNetworkThreatAdaptiveStrategyWithFakeFeed() {
        fakeFeed.setHighRisk(true);
        fakeFeed.setThreatScore(30.0);

        RiskScoringStrategy strategy = new NetworkThreatAdaptiveRiskStrategy(fakeFeed);
        ExamSessionTelemetry telemetry = new ExamSessionTelemetry(
                "S3", 0.0, 0, 0, 2, 0, 0, "198.51.100.1"
        );
        // Subnet: 30; HighRisk: 25; Tabs: 2 * 8 = 16 => Total 71.0
        assertEquals(71.0, strategy.calculateRisk(telemetry), 0.001);
    }

    @Test
    @DisplayName("Selector Rule 1: High stakes exam with high risk student selects NetworkThreatAdaptive")
    void testSelectorHighStakesHighRisk() {
        RiskStrategySelector selector = new RiskStrategySelector(fakeFeed);
        RiskScoringStrategy strategy = selector.selectStrategy(ExamDeliveryMode.REMOTE_HIGH_STAKES, 3);
        assertInstanceOf(NetworkThreatAdaptiveRiskStrategy.class, strategy);
    }

    @Test
    @DisplayName("Selector Rule 2: High stakes exam with normal student selects StrictBiometric")
    void testSelectorHighStakesNormal() {
        RiskStrategySelector selector = new RiskStrategySelector(fakeFeed);
        RiskScoringStrategy strategy = selector.selectStrategy(ExamDeliveryMode.REMOTE_HIGH_STAKES, 1);
        assertInstanceOf(StrictBiometricRiskStrategy.class, strategy);
    }

    @Test
    @DisplayName("Selector Rule 3: Campus lab exam selects ProcessKeystroke")
    void testSelectorCampusLab() {
        RiskStrategySelector selector = new RiskStrategySelector(fakeFeed);
        RiskScoringStrategy strategy = selector.selectStrategy(ExamDeliveryMode.CAMPUS_LAB_MONITORED, 1);
        assertInstanceOf(ProcessKeystrokeRiskStrategy.class, strategy);
    }

    @Test
    @DisplayName("Selector Rule 4: Remote unsupervised exam selects NetworkThreatAdaptive")
    void testSelectorRemoteUnsupervised() {
        RiskStrategySelector selector = new RiskStrategySelector(fakeFeed);
        RiskScoringStrategy strategy = selector.selectStrategy(ExamDeliveryMode.REMOTE_UNSUPERVISED, 1);
        assertInstanceOf(NetworkThreatAdaptiveRiskStrategy.class, strategy);
    }

    @Test
    @DisplayName("Context allows runtime strategy switching without altering client code")
    void testContextRuntimeSwitch() {
        RiskScoringStrategy bio = new StrictBiometricRiskStrategy();
        RiskScoringStrategy proc = new ProcessKeystrokeRiskStrategy();

        RiskAssessmentContext context = new RiskAssessmentContext(bio);
        assertSame(bio, context.getStrategy());

        context.setStrategy(proc);
        assertSame(proc, context.getStrategy());
    }

    @Test
    @DisplayName("Refactoring Verification: Legacy monolithic calculator matches Strategy pattern output")
    void testRefactoringEquality() {
        fakeFeed.setHighRisk(false);
        fakeFeed.setThreatScore(10.0);

        LegacyMonolithicRiskCalculator legacy = new LegacyMonolithicRiskCalculator(fakeFeed);

        RiskScoringStrategy bioStrategy = new StrictBiometricRiskStrategy();
        RiskScoringStrategy procStrategy = new ProcessKeystrokeRiskStrategy();
        RiskScoringStrategy netStrategy = new NetworkThreatAdaptiveRiskStrategy(fakeFeed);

        ExamSessionTelemetry sample = new ExamSessionTelemetry("STEST", 4.0, 1, 1, 2, 1, 0, "192.168.1.10");

        assertEquals(legacy.calculateRisk("BIOMETRIC", sample), bioStrategy.calculateRisk(sample), 0.001);
        assertEquals(legacy.calculateRisk("PROCESS", sample), procStrategy.calculateRisk(sample), 0.001);
        assertEquals(legacy.calculateRisk("NETWORK", sample), netStrategy.calculateRisk(sample), 0.001);
    }

    // =========================================================================
    // PARAMETERIZED TEST (Section 2 Mandatory Requirement)
    // Runs the exact same check across all strategies of Family 1!
    // =========================================================================
    static Stream<RiskScoringStrategy> provideAllRiskStrategies() {
        FakeThreatIntelligenceFeed feed = new FakeThreatIntelligenceFeed();
        feed.setHighRisk(true);
        feed.setThreatScore(25.0);

        return Stream.of(
                new StrictBiometricRiskStrategy(),
                new ProcessKeystrokeRiskStrategy(),
                new NetworkThreatAdaptiveRiskStrategy(feed)
        );
    }

    @ParameterizedTest
    @MethodSource("provideAllRiskStrategies")
    @DisplayName("Parameterized Test: All Risk Strategies enforce [0.0, 100.0] score bounds on extreme inputs")
    void testAllRiskStrategiesBoundedScore(RiskScoringStrategy strategy) {
        // Zero anomaly input -> must be 0.0
        ExamSessionTelemetry clean = new ExamSessionTelemetry("S0", 0.0, 0, 0, 0, 0, 0, "127.0.0.1");
        double minScore = strategy.calculateRisk(clean);
        assertTrue(minScore >= 0.0, "Score must never be negative for strategy: " + strategy.getStrategyName());

        // Extreme excessive anomaly input -> must be clamped to 100.0
        ExamSessionTelemetry extreme = new ExamSessionTelemetry("S99", 500.0, 50, 100, 80, 40, 20, "185.220.101.5");
        double maxScore = strategy.calculateRisk(extreme);
        assertTrue(maxScore <= 100.0, "Score must never exceed 100.0 for strategy: " + strategy.getStrategyName());
        assertEquals(100.0, maxScore, 0.001);

        // Null input safety
        assertEquals(0.0, strategy.calculateRisk(null));
    }
}
