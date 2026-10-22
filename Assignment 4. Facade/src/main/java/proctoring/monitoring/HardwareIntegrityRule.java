package proctoring.monitoring;

import java.util.Arrays;
import java.util.List;

public class HardwareIntegrityRule {
    private static final List<String> FORBIDDEN_PROCESSES = Arrays.asList(
            "teamviewer", "anydesk", "discord", "cheatengine", "virtualbox"
    );

    public boolean isDisplayConfigurationValid(int activeDisplays) {
        return activeDisplays == 1;
    }

    public boolean isWebcamFeedOperational(boolean webcamActive) {
        return webcamActive;
    }

    public boolean hasSuspiciousForegroundProcess(String runningProcessName) {
        if (runningProcessName == null) {
            return false;
        }
        String normalized = runningProcessName.trim().toLowerCase();
        for (String forbidden : FORBIDDEN_PROCESSES) {
            if (normalized.contains(forbidden)) {
                return true;
            }
        }
        return false;
    }
}
