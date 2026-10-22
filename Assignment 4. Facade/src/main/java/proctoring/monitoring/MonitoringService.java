package proctoring.monitoring;

import java.util.Optional;
import java.util.UUID;

public class MonitoringService {
    private final HardwareIntegrityRule integrityRule;
    private final MonitoringPersistenceGateway gateway;

    public MonitoringService(HardwareIntegrityRule integrityRule, MonitoringPersistenceGateway gateway) {
        this.integrityRule = integrityRule;
        this.gateway = gateway;
    }

    public MonitoringSession startAndCompleteMonitoring(String studentId, String examCode, int activeDisplays, boolean webcamActive, String runningProcess) {
        String sessionId = "MON-" + UUID.randomUUID().toString().substring(0, 8);
        MonitoringSession session = new MonitoringSession(sessionId, studentId, examCode);

        if (!integrityRule.isWebcamFeedOperational(webcamActive)) {
            session.recordFlaggedViolation("Webcam feed was disabled or obstructed.");
            gateway.save(session);
            throw new MonitoringViolationException("Monitoring failed: Webcam feed is not operational.");
        }

        if (!integrityRule.isDisplayConfigurationValid(activeDisplays)) {
            session.recordFlaggedViolation("Multi-monitor anomaly detected (" + activeDisplays + " active displays).");
            gateway.save(session);
            throw new MonitoringViolationException("Monitoring failed: Unauthorized multi-monitor setup detected.");
        }

        if (integrityRule.hasSuspiciousForegroundProcess(runningProcess)) {
            session.recordFlaggedViolation("Prohibited software process detected: " + runningProcess);
            gateway.save(session);
            throw new MonitoringViolationException("Monitoring failed: Forbidden software running: " + runningProcess);
        }

        session.recordPassedClean();
        gateway.save(session);

        System.out.println("[Subsystem B - Monitoring] Session " + sessionId + " verified clean for student: " + studentId);
        return session;
    }

    public void compensateSession(String studentId, String examCode, String reason) {
        Optional<MonitoringSession> optionalSession = gateway.findByStudentAndExam(studentId, examCode);
        if (optionalSession.isPresent()) {
            MonitoringSession session = optionalSession.get();
            session.compensateAndInvalidate(reason);
            gateway.save(session);
            System.out.println("[Subsystem B - Monitoring] Compensated/Invalidated session for student: " + studentId + " (" + reason + ")");
        }
    }

    public Optional<MonitoringSession> getSession(String studentId, String examCode) {
        return gateway.findByStudentAndExam(studentId, examCode);
    }
}
