package proctoring.facade;

import proctoring.chain.ChainExecutionJournal;
import proctoring.chain.ChainJournalRepository;
import proctoring.chain.ChainStepStatus;
import proctoring.grading.ExamTranscriptRecord;
import proctoring.grading.GradingService;
import proctoring.intake.IntakeService;
import proctoring.intake.VerificationRecord;
import proctoring.monitoring.MonitoringService;
import proctoring.monitoring.MonitoringSession;

public class ExamProctoringFacade {
    private final IntakeService intakeService;
    private final MonitoringService monitoringService;
    private final GradingService gradingService;
    private final ChainJournalRepository journalRepository;

    public ExamProctoringFacade(IntakeService intakeService,
                                MonitoringService monitoringService,
                                GradingService gradingService,
                                ChainJournalRepository journalRepository) {
        this.intakeService = intakeService;
        this.monitoringService = monitoringService;
        this.gradingService = gradingService;
        this.journalRepository = journalRepository;
    }

    public ExamResultSummary conductExamSession(String studentId,
                                                String examCode,
                                                String documentNumber,
                                                int activeDisplays,
                                                boolean webcamActive,
                                                String runningProcess,
                                                String answersPayload,
                                                String expectedChecksum) {
        if (studentId == null || examCode == null) {
            throw new IllegalArgumentException("Student ID and Exam Code are mandatory.");
        }

        ChainExecutionJournal journal = journalRepository.findByStudentAndExam(studentId, examCode)
                .orElse(new ChainExecutionJournal(studentId, examCode));

        journal.transitionTo(ChainStepStatus.NOT_STARTED, "Starting proctoring chain");
        journalRepository.save(journal);

        // --- STEP A: INTAKE & RESERVATION ---
        VerificationRecord verificationRecord;
        try {
            verificationRecord = intakeService.verifyAndReserveSlot(studentId, examCode, documentNumber);
            journal.transitionTo(ChainStepStatus.STAGE_A_COMMITTED, "Stage A committed: Student verified & slot reserved");
            journalRepository.save(journal);
        } catch (Exception ex) {
            journal.transitionTo(ChainStepStatus.NOT_STARTED, "Stage A rejected: " + ex.getMessage());
            journalRepository.save(journal);
            throw new ProctoringChainException("Chain failed at Stage A: " + ex.getMessage(), ex);
        }

        // --- STEP B: LIVE MONITORING & ANTI-CHEAT ---
        MonitoringSession monitoringSession;
        try {
            monitoringSession = monitoringService.startAndCompleteMonitoring(
                    studentId, examCode, activeDisplays, webcamActive, runningProcess
            );
            journal.transitionTo(ChainStepStatus.STAGE_B_COMMITTED, "Stage B committed: Proctoring session clean");
            journalRepository.save(journal);
        } catch (Exception ex) {
            // COMPENSATION DEPTH 1: B failed after A committed. Roll back A alone.
            System.err.println("[Facade Orchestrator] Stage B failed. Executing Compensation Depth 1 (Unwinding Step A)...");
            journal.transitionTo(ChainStepStatus.COMPENSATING_A, "Compensating Stage A due to: " + ex.getMessage());
            journalRepository.save(journal);

            intakeService.compensateReservation(studentId, examCode, "Stage B violation: " + ex.getMessage());

            journal.transitionTo(ChainStepStatus.COMPENSATED_A, "Stage A compensated. Chain safely unwound.");
            journalRepository.save(journal);

            throw new ProctoringChainException("Chain aborted at Stage B: " + ex.getMessage(), ex);
        }

        // --- STEP C: GRADING & TRANSCRIPT CERTIFICATION ---
        ExamTranscriptRecord transcriptRecord;
        try {
            transcriptRecord = gradingService.evaluateAndCertify(studentId, examCode, answersPayload, expectedChecksum);
            journal.transitionTo(ChainStepStatus.CHAIN_COMPLETED, "Chain completed: All stages A->B->C certified.");
            journalRepository.save(journal);
        } catch (Exception ex) {
            // COMPENSATION DEPTH 2: C failed after A and B committed. Roll back B, then A.
            System.err.println("[Facade Orchestrator] Stage C failed. Executing Compensation Depth 2 (Two-step unwinding: B then A)...");
            journal.transitionTo(ChainStepStatus.COMPENSATING_B_A, "Compensating Stage B then Stage A due to: " + ex.getMessage());
            journalRepository.save(journal);

            // Step 1 of rollback: Compensate B
            monitoringService.compensateSession(studentId, examCode, "Stage C failure: " + ex.getMessage());

            // Step 2 of rollback: Compensate A
            intakeService.compensateReservation(studentId, examCode, "Stage C failure: " + ex.getMessage());

            journal.transitionTo(ChainStepStatus.COMPENSATED_B_A, "Two-step compensation complete (B and A unwound).");
            journalRepository.save(journal);

            throw new ProctoringChainException("Chain aborted at Stage C: " + ex.getMessage(), ex);
        }

        return new ExamResultSummary(
                studentId,
                examCode,
                verificationRecord.getStatus(),
                monitoringSession.getStatus(),
                transcriptRecord.getStatus(),
                transcriptRecord.getFinalScore(),
                transcriptRecord.getLetterGrade()
        );
    }
}
