package proctoring.strategy.external;

public class SimulatedThreatIntelligenceFeed implements ThreatIntelligenceFeed {

    @Override
    public boolean isHighRiskNetwork(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        return ipAddress.startsWith("185.220.") ||
               ipAddress.startsWith("198.51.") ||
               ipAddress.contains(".proxy.") ||
               ipAddress.startsWith("10.99.");
    }

    @Override
    public double getSubnetThreatScore(String ipAddress) {
        if (isHighRiskNetwork(ipAddress)) {
            return 30.0;
        }
        if (ipAddress != null && ipAddress.startsWith("192.168.")) {
            return 5.0;
        }
        return 0.0;
    }
}
