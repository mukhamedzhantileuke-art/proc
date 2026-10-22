package proctoring.strategy.grading;

import proctoring.strategy.model.RawExamSubmission;

public interface GradingCurveStrategy {
    double computeFinalScore(RawExamSubmission submission);
    String getStrategyName();
}
