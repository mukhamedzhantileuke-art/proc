package proctoring.intake;

public class VerificationRecord {
    private final String studentId;
    private final String examCode;
    private final String documentNumber;
    private VerificationStatus status;
    private final long createdAt;
    private long updatedAt;

    public VerificationRecord(String studentId, String examCode, String documentNumber) {
        this(studentId, examCode, documentNumber, VerificationStatus.PENDING, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public VerificationRecord(String studentId, String examCode, String documentNumber, VerificationStatus status, long createdAt, long updatedAt) {
        this.studentId = studentId;
        this.examCode = examCode;
        this.documentNumber = documentNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public synchronized void markVerifiedAndReserved() {
        if (this.status != VerificationStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm verification from status: " + this.status);
        }
        this.status = VerificationStatus.VERIFIED_RESERVED;
        this.updatedAt = System.currentTimeMillis();
    }

    public synchronized void revokeReservation(String reason) {
        if (this.status == VerificationStatus.REVOKED) {
            return;
        }
        if (this.status != VerificationStatus.VERIFIED_RESERVED && this.status != VerificationStatus.PENDING) {
            throw new IllegalStateException("Cannot revoke slot from illegal state: " + this.status);
        }
        this.status = VerificationStatus.REVOKED;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getStudentId() {
        return studentId;
    }

    public String getExamCode() {
        return examCode;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "VerificationRecord{" +
                "studentId='" + studentId + '\'' +
                ", examCode='" + examCode + '\'' +
                ", status=" + status +
                '}';
    }
}
