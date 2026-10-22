package proctoring.strategy.grading;

import proctoring.strategy.model.RawExamSubmission;

public class GradingContext {
    private GradingCurveStrategy strategy;

    public GradingContext(GradingCurveStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(GradingCurveStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Grading strategy cannot be null.");
        }
        this.strategy = strategy;
    }

    public GradingCurveStrategy getStrategy() {
        return strategy;
    }

    public double calculateGrade(RawExamSubmission submission) {
        return strategy.computeFinalScore(submission);
    }
}
