package proctoring.strategy.grading;

import proctoring.strategy.model.RawExamSubmission;

public class ProctoringPenaltyGradingStrategy implements GradingCurveStrategy {
    private static final double PENALTY_PER_ANOMALY = 15.0;

    @Override
    public double computeFinalScore(RawExamSubmission submission) {
        if (submission == null) {
            return 0.0;
        }

        double raw = submission.getRawPercentage();
        double penalty = submission.getConfirmedProctoringAnomalies() * PENALTY_PER_ANOMALY;
        double adjusted = raw - penalty;

        return Math.min(100.0, Math.max(0.0, adjusted));
    }

    @Override
    public String getStrategyName() {
        return "Proctoring Integrity Weighted Penalty";
    }
}
