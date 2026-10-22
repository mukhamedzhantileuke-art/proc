package proctoring.strategy;

import proctoring.strategy.external.SimulatedThreatIntelligenceFeed;
import proctoring.strategy.external.ThreatIntelligenceFeed;
import proctoring.strategy.grading.*;
import proctoring.strategy.model.*;
import proctoring.strategy.risk.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   ASSIGNMENT 5: STRATEGY PATTERN IN ONLINE EXAM PROCTORING                     ");
        System.out.println("================================================================================");

        ThreatIntelligenceFeed threatFeed = new SimulatedThreatIntelligenceFeed();

        // Sample Input Telemetry
        ExamSessionTelemetry suspiciousSession = new ExamSessionTelemetry(
                "STUDENT-8841",
                12.5,   // Gaze away for 12.5 seconds
                2,      // Face disappeared twice
                3,      // 3 audio spikes
                4,      // 4 tab switches
                2,      // 2 copy/paste events
                1,      // 1 unauthorized process (e.g. background cheat software)
                "185.220.101.5" // Known VPN/TOR IP range
        );

        // =========================================================================
        // PART 1: FAMILY 1 - SAME INPUT PROCESSED BY ALL 3 RISK STRATEGIES
        // =========================================================================
        System.out.println("\n>>> [PART 1] RISK SCORING: SAME INPUT ACROSS ALL THREE STRATEGIES");
        System.out.println("Exam Telemetry: " + suspiciousSession);

        RiskScoringStrategy biometricStrategy = new StrictBiometricRiskStrategy();
        RiskScoringStrategy processStrategy = new ProcessKeystrokeRiskStrategy();
        RiskScoringStrategy networkStrategy = new NetworkThreatAdaptiveRiskStrategy(threatFeed);

        System.out.println("\n1. " + biometricStrategy.getStrategyName() +
                " -> Risk Score: " + String.format("%.2f", biometricStrategy.calculateRisk(suspiciousSession)) + " / 100.0");
        System.out.println("2. " + processStrategy.getStrategyName() +
                " -> Risk Score: " + String.format("%.2f", processStrategy.calculateRisk(suspiciousSession)) + " / 100.0");
        System.out.println("3. " + networkStrategy.getStrategyName() +
                " -> Risk Score: " + String.format("%.2f", networkStrategy.calculateRisk(suspiciousSession)) + " / 100.0");

        // =========================================================================
        // PART 2: MANUAL CHOICE VS. AUTOMATIC SELECTOR CHOICE & RUNTIME SWITCHING
        // =========================================================================
        System.out.println("\n>>> [PART 2] RISK CONTEXT: MANUAL VS AUTOMATIC SELECTION & RUNTIME SWITCH");

        // Manual choice
        RiskAssessmentContext riskContext = new RiskAssessmentContext(processStrategy);
        System.out.println("Initial Strategy (Manual): " + riskContext.getStrategy().getStrategyName());
        System.out.println("Evaluated Risk: " + String.format("%.2f", riskContext.assessRisk(suspiciousSession)));

        // Automatic selection via Selector
        RiskStrategySelector riskSelector = new RiskStrategySelector(threatFeed);
        RiskScoringStrategy autoSelected = riskSelector.selectStrategy(ExamDeliveryMode.REMOTE_HIGH_STAKES, 3);
        System.out.println("\nSelector Rule (Mode: REMOTE_HIGH_STAKES, RiskTier: 3) -> Chosen: " + autoSelected.getStrategyName());

        // Runtime switching inside context
        riskContext.setStrategy(autoSelected);
        System.out.println("Context Strategy Swapped At Runtime -> Evaluated Risk: " +
                String.format("%.2f", riskContext.assessRisk(suspiciousSession)));

        // =========================================================================
        // PART 3: FAMILY 2 - GRADING & CURVING STRATEGY DEMONSTRATION
        // =========================================================================
        System.out.println("\n>>> [PART 3] GRADING CURVE: SAME SUBMISSION ACROSS ALL THREE STRATEGIES");

        RawExamSubmission submission = new RawExamSubmission(
                "STUDENT-8841",
                42,     // 42 correct answers
                50,     // out of 50 (84.0% raw)
                2,      // 2 confirmed proctoring anomalies
                35.0,   // cohort average 35.0 (70.0%)
                5.0     // cohort standard deviation 5.0
        );
        System.out.println("Exam Submission: " + submission);

        GradingCurveStrategy rawStrategy = new RawPercentageGradingStrategy();
        GradingCurveStrategy bellStrategy = new BellCurveGradingStrategy();
        GradingCurveStrategy penaltyStrategy = new ProctoringPenaltyGradingStrategy();

        System.out.println("\n1. " + rawStrategy.getStrategyName() +
                " -> Final Grade: " + String.format("%.2f", rawStrategy.computeFinalScore(submission)) + "%");
        System.out.println("2. " + bellStrategy.getStrategyName() +
                " -> Final Grade: " + String.format("%.2f", bellStrategy.computeFinalScore(submission)) + "%");
        System.out.println("3. " + penaltyStrategy.getStrategyName() +
                " -> Final Grade: " + String.format("%.2f", penaltyStrategy.computeFinalScore(submission)) + "%");

        // Automatic selection by GradingStrategySelector
        GradingStrategySelector gradingSelector = new GradingStrategySelector();
        GradingCurveStrategy policyChosen = gradingSelector.selectStrategy(CourseGradingPolicy.BELL_CURVE_NORMALIZED, false);
        GradingContext gradingContext = new GradingContext(policyChosen);
        System.out.println("\nSelector Rule (Policy: BELL_CURVE_NORMALIZED) -> Chosen: " + policyChosen.getStrategyName());
        System.out.println("Computed Final Grade: " + String.format("%.2f", gradingContext.calculateGrade(submission)) + "%");

        // Runtime switch when cheating flags are detected
        GradingCurveStrategy flaggedPolicy = gradingSelector.selectStrategy(CourseGradingPolicy.BELL_CURVE_NORMALIZED, true);
        gradingContext.setStrategy(flaggedPolicy);
        System.out.println("Cheating flags detected! Context Swapped Strategy to: " + flaggedPolicy.getStrategyName());
        System.out.println("New Penalized Final Grade: " + String.format("%.2f", gradingContext.calculateGrade(submission)) + "%");

        System.out.println("\n================================================================================");
        System.out.println("                     DEMONSTRATION COMPLETED SUCCESSFULLY                       ");
        System.out.println("================================================================================");
    }
}
