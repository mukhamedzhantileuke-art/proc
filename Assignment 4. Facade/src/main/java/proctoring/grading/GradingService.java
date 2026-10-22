package proctoring.grading;

import java.util.Optional;
import java.util.UUID;

public class GradingService {
    private final SubmissionIntegrityEngine integrityEngine;
    private final GradingPersistenceGateway gateway;

    public GradingService(SubmissionIntegrityEngine integrityEngine, GradingPersistenceGateway gateway) {
        this.integrityEngine = integrityEngine;
        this.gateway = gateway;
    }

    public ExamTranscriptRecord evaluateAndCertify(String studentId, String examCode, String answersPayload, String expectedChecksum) {
        String transcriptId = "TRX-" + UUID.randomUUID().toString().substring(0, 8);
        ExamTranscriptRecord record = new ExamTranscriptRecord(transcriptId, studentId, examCode);

        if (!integrityEngine.verifySubmissionChecksum(answersPayload, expectedChecksum)) {
            record.rejectIntegrity("Cryptographic checksum mismatch. Submission packet corrupted or tampered.");
            gateway.save(record);
            throw new GradingIntegrityException("Grading failed: Submission cryptographic checksum invalid.");
        }

        double score = integrityEngine.computeScore(answersPayload);
        String letterGrade = integrityEngine.determineLetterGrade(score);

        record.certifyGrade(score, letterGrade);
        gateway.save(record);

        System.out.println("[Subsystem C - Grading] Transcript " + transcriptId + " certified with grade " + letterGrade + " (" + score + "%) for: " + studentId);
        return record;
    }

    public void compensateTranscript(String studentId, String examCode, String reason) {
        Optional<ExamTranscriptRecord> optionalRecord = gateway.findByStudentAndExam(studentId, examCode);
        if (optionalRecord.isPresent()) {
            ExamTranscriptRecord record = optionalRecord.get();
            record.rejectIntegrity("COMPENSATED_ROLLBACK: " + reason);
            gateway.save(record);
            System.out.println("[Subsystem C - Grading] Compensated/Revoked transcript for student: " + studentId + " (" + reason + ")");
        }
    }

    public Optional<ExamTranscriptRecord> getRecord(String studentId, String examCode) {
        return gateway.findByStudentAndExam(studentId, examCode);
    }
}
