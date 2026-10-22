package proctoring.strategy.grading;

import proctoring.strategy.model.CourseGradingPolicy;

public class GradingStrategySelector {

    public GradingCurveStrategy selectStrategy(CourseGradingPolicy policy, boolean hasProctoringIntegrityFlags) {
        if (hasProctoringIntegrityFlags) {
            return new ProctoringPenaltyGradingStrategy();
        }

        if (policy == null) {
            return new RawPercentageGradingStrategy();
        }

        switch (policy) {
            case BELL_CURVE_NORMALIZED:
                return new BellCurveGradingStrategy();

            case STRICT_INTEGRITY_WEIGHTED:
                return new ProctoringPenaltyGradingStrategy();

            case ABSOLUTE_STANDARD:
            default:
                return new RawPercentageGradingStrategy();
        }
    }
}
