package proctoring.monitoring;

public class MonitoringSession {
    private final String sessionId;
    private final String studentId;
    private final String examCode;
    private MonitoringStatus status;
    private String violationSummary;
    private final long startTime;
    private long endTime;

    public MonitoringSession(String sessionId, String studentId, String examCode) {
        this(sessionId, studentId, examCode, MonitoringStatus.INITIALIZED, "NONE", System.currentTimeMillis(), System.currentTimeMillis());
    }

    public MonitoringSession(String sessionId, String studentId, String examCode, MonitoringStatus status, String violationSummary, long startTime, long endTime) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.examCode = examCode;
        this.status = status;
        this.violationSummary = violationSummary;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public synchronized void recordPassedClean() {
        if (this.status != MonitoringStatus.INITIALIZED) {
            throw new IllegalStateException("Cannot pass monitoring session from status: " + this.status);
        }
        this.status = MonitoringStatus.PASSED_CLEAN;
        this.endTime = System.currentTimeMillis();
    }

    public synchronized void recordFlaggedViolation(String violationDetails) {
        if (this.status == MonitoringStatus.ROLLBACK_INVALIDATED) {
            throw new IllegalStateException("Cannot flag already invalidated session");
        }
        this.status = MonitoringStatus.FLAGGED_VIOLATION;
        this.violationSummary = violationDetails;
        this.endTime = System.currentTimeMillis();
    }

    public synchronized void compensateAndInvalidate(String compensationReason) {
        if (this.status == MonitoringStatus.ROLLBACK_INVALIDATED) {
            return;
        }
        this.status = MonitoringStatus.ROLLBACK_INVALIDATED;
        this.violationSummary = "COMPENSATED: " + compensationReason;
        this.endTime = System.currentTimeMillis();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getExamCode() {
        return examCode;
    }

    public MonitoringStatus getStatus() {
        return status;
    }

    public String getViolationSummary() {
        return violationSummary;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return "MonitoringSession{" +
                "sessionId='" + sessionId + '\'' +
                ", studentId='" + studentId + '\'' +
                ", status=" + status +
                ", violation='" + violationSummary + '\'' +
                '}';
    }
}
