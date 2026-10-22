package proctoring.grading;

public class ExamTranscriptRecord {
    private final String transcriptId;
    private final String studentId;
    private final String examCode;
    private GradingStatus status;
    private double finalScore;
    private String letterGrade;
    private String remarks;
    private final long timestamp;

    public ExamTranscriptRecord(String transcriptId, String studentId, String examCode) {
        this(transcriptId, studentId, examCode, GradingStatus.AWAITING_EVALUATION, 0.0, "PENDING", "INITIALIZED", System.currentTimeMillis());
    }

    public ExamTranscriptRecord(String transcriptId, String studentId, String examCode, GradingStatus status, double finalScore, String letterGrade, String remarks, long timestamp) {
        this.transcriptId = transcriptId;
        this.studentId = studentId;
        this.examCode = examCode;
        this.status = status;
        this.finalScore = finalScore;
        this.letterGrade = letterGrade;
        this.remarks = remarks;
        this.timestamp = timestamp;
    }

    public synchronized void certifyGrade(double score, String letterGrade) {
        if (this.status != GradingStatus.AWAITING_EVALUATION) {
            throw new IllegalStateException("Cannot certify grade from status: " + this.status);
        }
        this.status = GradingStatus.CERTIFIED_RECORDED;
        this.finalScore = score;
        this.letterGrade = letterGrade;
        this.remarks = "OFFICIALLY CERTIFIED";
    }

    public synchronized void rejectIntegrity(String reason) {
        this.status = GradingStatus.INTEGRITY_REJECTED;
        this.remarks = reason;
    }

    public String getTranscriptId() {
        return transcriptId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getExamCode() {
        return examCode;
    }

    public GradingStatus getStatus() {
        return status;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public String getRemarks() {
        return remarks;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ExamTranscriptRecord{" +
                "transcriptId='" + transcriptId + '\'' +
                ", studentId='" + studentId + '\'' +
                ", status=" + status +
                ", score=" + finalScore +
                ", grade='" + letterGrade + '\'' +
                ", remarks='" + remarks + '\'' +
                '}';
    }
}
