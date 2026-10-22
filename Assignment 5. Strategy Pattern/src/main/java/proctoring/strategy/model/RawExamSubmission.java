package proctoring.strategy.model;

public class RawExamSubmission {
    private final String studentId;
    private final int correctAnswers;
    private final int totalQuestions;
    private final int confirmedProctoringAnomalies;
    private final double cohortAverage;
    private final double cohortStandardDeviation;

    public RawExamSubmission(String studentId,
                             int correctAnswers,
                             int totalQuestions,
                             int confirmedProctoringAnomalies,
                             double cohortAverage,
                             double cohortStandardDeviation) {
        if (totalQuestions <= 0) {
            throw new IllegalArgumentException("Total questions must be greater than zero.");
        }
        this.studentId = studentId;
        this.correctAnswers = Math.max(0, correctAnswers);
        this.totalQuestions = totalQuestions;
        this.confirmedProctoringAnomalies = Math.max(0, confirmedProctoringAnomalies);
        this.cohortAverage = cohortAverage;
        this.cohortStandardDeviation = Math.max(0.1, cohortStandardDeviation);
    }

    public String getStudentId() {
        return studentId;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public int getConfirmedProctoringAnomalies() {
        return confirmedProctoringAnomalies;
    }

    public double getCohortAverage() {
        return cohortAverage;
    }

    public double getCohortStandardDeviation() {
        return cohortStandardDeviation;
    }

    public double getRawPercentage() {
        return (correctAnswers * 100.0) / totalQuestions;
    }

    @Override
    public String toString() {
        return "Submission{" +
                "student='" + studentId + '\'' +
                ", score=" + correctAnswers + "/" + totalQuestions + " (" + String.format("%.1f", getRawPercentage()) + "%)" +
                ", anomalies=" + confirmedProctoringAnomalies +
                ", cohortAvg=" + cohortAverage +
                ", cohortStd=" + cohortStandardDeviation +
                '}';
    }
}
