package proctoring.strategy.external;

public interface ThreatIntelligenceFeed {
    boolean isHighRiskNetwork(String ipAddress);
    double getSubnetThreatScore(String ipAddress);
}
