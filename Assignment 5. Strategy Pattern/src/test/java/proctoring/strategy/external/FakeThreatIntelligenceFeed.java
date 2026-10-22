package proctoring.strategy.external;

public class FakeThreatIntelligenceFeed implements ThreatIntelligenceFeed {
    private boolean highRisk = false;
    private double threatScore = 0.0;

    public void setHighRisk(boolean highRisk) {
        this.highRisk = highRisk;
    }

    public void setThreatScore(double threatScore) {
        this.threatScore = threatScore;
    }

    @Override
    public boolean isHighRiskNetwork(String ipAddress) {
        return highRisk;
    }

    @Override
    public double getSubnetThreatScore(String ipAddress) {
        return threatScore;
    }
}
