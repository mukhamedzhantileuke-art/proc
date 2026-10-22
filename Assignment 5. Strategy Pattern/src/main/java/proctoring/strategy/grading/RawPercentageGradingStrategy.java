package proctoring.strategy.grading;

import proctoring.strategy.model.RawExamSubmission;

public class RawPercentageGradingStrategy implements GradingCurveStrategy {

    @Override
    public double computeFinalScore(RawExamSubmission submission) {
        if (submission == null) {
            return 0.0;
        }
        return Math.min(100.0, Math.max(0.0, submission.getRawPercentage()));
    }

    @Override
    public String getStrategyName() {
        return "Absolute Raw Percentage";
    }
}
