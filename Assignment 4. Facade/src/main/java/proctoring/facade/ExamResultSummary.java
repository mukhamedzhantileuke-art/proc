package proctoring.facade;

import proctoring.grading.GradingStatus;
import proctoring.intake.VerificationStatus;
import proctoring.monitoring.MonitoringStatus;

public class ExamResultSummary {
    private final String studentId;
    private final String examCode;
    private final VerificationStatus intakeStatus;
    private final MonitoringStatus monitoringStatus;
    private final GradingStatus gradingStatus;
    private final double score;
    private final String letterGrade;

    public ExamResultSummary(String studentId, String examCode, VerificationStatus intakeStatus, MonitoringStatus monitoringStatus, GradingStatus gradingStatus, double score, String letterGrade) {
        this.studentId = studentId;
        this.examCode = examCode;
        this.intakeStatus = intakeStatus;
        this.monitoringStatus = monitoringStatus;
        this.gradingStatus = gradingStatus;
        this.score = score;
        this.letterGrade = letterGrade;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getExamCode() {
        return examCode;
    }

    public VerificationStatus getIntakeStatus() {
        return intakeStatus;
    }

    public MonitoringStatus getMonitoringStatus() {
        return monitoringStatus;
    }

    public GradingStatus getGradingStatus() {
        return gradingStatus;
    }

    public double getScore() {
        return score;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    @Override
    public String toString() {
        return "ExamResultSummary{" +
                "student='" + studentId + '\'' +
                ", exam='" + examCode + '\'' +
                ", intake=" + intakeStatus +
                ", monitor=" + monitoringStatus +
                ", grading=" + gradingStatus +
                ", score=" + score +
                ", grade='" + letterGrade + '\'' +
                '}';
    }
}
