package proctoring.intake;

import java.util.Optional;

public class IntakeService {
    private final BiometricValidator validator;
    private final IntakePersistenceGateway gateway;

    public IntakeService(BiometricValidator validator, IntakePersistenceGateway gateway) {
        this.validator = validator;
        this.gateway = gateway;
    }

    public VerificationRecord verifyAndReserveSlot(String studentId, String examCode, String documentNumber) {
        if (validator.isStudentSuspended(studentId)) {
            throw new VerificationException("Student " + studentId + " is suspended from taking exams.");
        }
        if (!validator.validateStudentCredentials(studentId, documentNumber)) {
            throw new VerificationException("Biometric credential check failed for student: " + studentId);
        }

        VerificationRecord record = new VerificationRecord(studentId, examCode, documentNumber);
        record.markVerifiedAndReserved();
        gateway.save(record);

        System.out.println("[Subsystem A - Intake] Verified student " + studentId + " and reserved slot for exam " + examCode);
        return record;
    }

    public void compensateReservation(String studentId, String examCode, String reason) {
        Optional<VerificationRecord> optionalRecord = gateway.findByStudentAndExam(studentId, examCode);
        if (optionalRecord.isPresent()) {
            VerificationRecord record = optionalRecord.get();
            record.revokeReservation(reason);
            gateway.save(record);
            System.out.println("[Subsystem A - Intake] Compensated/Revoked reservation for student: " + studentId + " (" + reason + ")");
        }
    }

    public Optional<VerificationRecord> getRecord(String studentId, String examCode) {
        return gateway.findByStudentAndExam(studentId, examCode);
    }
}
