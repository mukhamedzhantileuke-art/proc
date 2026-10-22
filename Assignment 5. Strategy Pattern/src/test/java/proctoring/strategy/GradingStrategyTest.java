package proctoring.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import proctoring.strategy.grading.*;
import proctoring.strategy.model.CourseGradingPolicy;
import proctoring.strategy.model.RawExamSubmission;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class GradingStrategyTest {

    @Test
    @DisplayName("Raw percentage strategy calculates standard percentage score")
    void testRawPercentageGradingStrategy() {
        GradingCurveStrategy strategy = new RawPercentageGradingStrategy();
        RawExamSubmission submission = new RawExamSubmission("S1", 45, 50, 0, 70.0, 10.0);
        assertEquals(90.0, strategy.computeFinalScore(submission), 0.001);
    }

    @Test
    @DisplayName("Bell curve strategy normalizes score based on cohort mean and standard deviation")
    void testBellCurveGradingStrategy() {
        GradingCurveStrategy strategy = new BellCurveGradingStrategy();
        // Raw percentage = 70.0, which equals cohort mean (70.0). z-score = 0. Curved score = 75.0
        RawExamSubmission averageStudent = new RawExamSubmission("S2", 35, 50, 0, 70.0, 10.0);
        assertEquals(75.0, strategy.computeFinalScore(averageStudent), 0.001);

        // One standard deviation above cohort mean (raw = 80.0, z = +1.0). Curved score = 75.0 + 12.0 = 87.0
        RawExamSubmission highStudent = new RawExamSubmission("S3", 40, 50, 0, 70.0, 10.0);
        assertEquals(87.0, strategy.computeFinalScore(highStudent), 0.001);
    }

    @Test
    @DisplayName("Proctoring penalty strategy deducts 15 points per confirmed anomaly")
    void testProctoringPenaltyGradingStrategy() {
        GradingCurveStrategy strategy = new ProctoringPenaltyGradingStrategy();
        // Raw = 90.0, 2 anomalies * 15 = 30 deduction => Final 60.0
        RawExamSubmission flaggedStudent = new RawExamSubmission("S4", 45, 50, 2, 70.0, 10.0);
        assertEquals(60.0, strategy.computeFinalScore(flaggedStudent), 0.001);
    }

    @Test
    @DisplayName("Selector Rule: Selects penalty strategy when integrity flags are present")
    void testSelectorWithIntegrityFlags() {
        GradingStrategySelector selector = new GradingStrategySelector();
        GradingCurveStrategy strategy = selector.selectStrategy(CourseGradingPolicy.ABSOLUTE_STANDARD, true);
        assertInstanceOf(ProctoringPenaltyGradingStrategy.class, strategy);
    }

    @Test
    @DisplayName("Selector Rule: Selects bell curve strategy when policy is BELL_CURVE_NORMALIZED")
    void testSelectorBellCurve() {
        GradingStrategySelector selector = new GradingStrategySelector();
        GradingCurveStrategy strategy = selector.selectStrategy(CourseGradingPolicy.BELL_CURVE_NORMALIZED, false);
        assertInstanceOf(BellCurveGradingStrategy.class, strategy);
    }

    @Test
    @DisplayName("Selector Rule: Selects raw percentage when policy is ABSOLUTE_STANDARD")
    void testSelectorAbsoluteStandard() {
        GradingStrategySelector selector = new GradingStrategySelector();
        GradingCurveStrategy strategy = selector.selectStrategy(CourseGradingPolicy.ABSOLUTE_STANDARD, false);
        assertInstanceOf(RawPercentageGradingStrategy.class, strategy);
    }

    @Test
    @DisplayName("GradingContext enables dynamic strategy switching at runtime")
    void testGradingContextRuntimeSwitch() {
        GradingCurveStrategy raw = new RawPercentageGradingStrategy();
        GradingCurveStrategy penalty = new ProctoringPenaltyGradingStrategy();

        GradingContext context = new GradingContext(raw);
        assertSame(raw, context.getStrategy());

        context.setStrategy(penalty);
        assertSame(penalty, context.getStrategy());
    }

    // =========================================================================
    // PARAMETERIZED TEST (Section 2 Mandatory Requirement)
    // Runs the exact same check across all strategies of Family 2!
    // =========================================================================
    static Stream<GradingCurveStrategy> provideAllGradingStrategies() {
        return Stream.of(
                new RawPercentageGradingStrategy(),
                new BellCurveGradingStrategy(),
                new ProctoringPenaltyGradingStrategy()
        );
    }

    @ParameterizedTest
    @MethodSource("provideAllGradingStrategies")
    @DisplayName("Parameterized Test: All Grading Strategies produce grades bounded within [0.0, 100.0]")
    void testAllGradingStrategiesBoundedScore(GradingCurveStrategy strategy) {
        // Minimum score (0 correct answers, 10 anomalies)
        RawExamSubmission minSub = new RawExamSubmission("S_MIN", 0, 50, 10, 70.0, 10.0);
        double minGrade = strategy.computeFinalScore(minSub);
        assertTrue(minGrade >= 0.0, "Grade must never be negative for strategy: " + strategy.getStrategyName());

        // Maximum score (50/50 correct answers, 0 anomalies)
        RawExamSubmission maxSub = new RawExamSubmission("S_MAX", 50, 50, 0, 30.0, 10.0);
        double maxGrade = strategy.computeFinalScore(maxSub);
        assertTrue(maxGrade <= 100.0, "Grade must never exceed 100.0 for strategy: " + strategy.getStrategyName());

        // Null safety
        assertEquals(0.0, strategy.computeFinalScore(null));
    }
}
