package proctoring.chain;

public class ChainExecutionJournal {
    private final String studentId;
    private final String examCode;
    private ChainStepStatus status;
    private String statusMessage;
    private long lastUpdated;

    public ChainExecutionJournal(String studentId, String examCode) {
        this(studentId, examCode, ChainStepStatus.NOT_STARTED, "Chain initialized", System.currentTimeMillis());
    }

    public ChainExecutionJournal(String studentId, String examCode, ChainStepStatus status, String statusMessage, long lastUpdated) {
        this.studentId = studentId;
        this.examCode = examCode;
        this.status = status;
        this.statusMessage = statusMessage;
        this.lastUpdated = lastUpdated;
    }

    public synchronized void transitionTo(ChainStepStatus newStatus, String message) {
        this.status = newStatus;
        this.statusMessage = message;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getStudentId() {
        return studentId;
    }

    public String getExamCode() {
        return examCode;
    }

    public ChainStepStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return "ChainExecutionJournal{" +
                "studentId='" + studentId + '\'' +
                ", examCode='" + examCode + '\'' +
                ", status=" + status +
                ", msg='" + statusMessage + '\'' +
                '}';
    }
}
