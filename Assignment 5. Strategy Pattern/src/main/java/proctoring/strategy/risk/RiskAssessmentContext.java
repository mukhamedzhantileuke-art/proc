package proctoring.strategy.risk;

import proctoring.strategy.model.ExamSessionTelemetry;

public class RiskAssessmentContext {
    private RiskScoringStrategy strategy;

    public RiskAssessmentContext(RiskScoringStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(RiskScoringStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null.");
        }
        this.strategy = strategy;
    }

    public RiskScoringStrategy getStrategy() {
        return strategy;
    }

    public double assessRisk(ExamSessionTelemetry telemetry) {
        return strategy.calculateRisk(telemetry);
    }
}
