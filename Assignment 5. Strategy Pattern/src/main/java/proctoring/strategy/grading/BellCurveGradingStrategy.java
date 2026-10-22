package proctoring.strategy.grading;

import proctoring.strategy.model.RawExamSubmission;

public class BellCurveGradingStrategy implements GradingCurveStrategy {

    @Override
    public double computeFinalScore(RawExamSubmission submission) {
        if (submission == null) {
            return 0.0;
        }

        double raw = submission.getRawPercentage();
        double zScore = (raw - submission.getCohortAverage()) / submission.getCohortStandardDeviation();
        double normalized = 75.0 + (zScore * 12.0);

        return Math.min(100.0, Math.max(0.0, normalized));
    }

    @Override
    public String getStrategyName() {
        return "Statistical Bell Curve Normalization";
    }
}
