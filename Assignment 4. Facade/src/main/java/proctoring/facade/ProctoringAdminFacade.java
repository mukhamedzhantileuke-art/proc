package proctoring.facade;

import proctoring.chain.ChainExecutionJournal;
import proctoring.chain.ChainJournalRepository;
import proctoring.chain.ChainStepStatus;
import proctoring.grading.ExamTranscriptRecord;
import proctoring.grading.GradingPersistenceGateway;
import proctoring.grading.GradingService;
import proctoring.intake.IntakePersistenceGateway;
import proctoring.intake.IntakeService;
import proctoring.intake.VerificationRecord;
import proctoring.monitoring.MonitoringPersistenceGateway;
import proctoring.monitoring.MonitoringService;
import proctoring.monitoring.MonitoringSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ProctoringAdminFacade {
    private final IntakeService intakeService;
    private final MonitoringService monitoringService;
    private final GradingService gradingService;
    private final ChainJournalRepository journalRepository;

    public ProctoringAdminFacade(IntakeService intakeService,
                                 MonitoringService monitoringService,
                                 GradingService gradingService,
                                 ChainJournalRepository journalRepository) {
        this.intakeService = intakeService;
        this.monitoringService = monitoringService;
        this.gradingService = gradingService;
        this.journalRepository = journalRepository;
    }

    public List<ChainExecutionJournal> listAllChains() {
        return journalRepository.findAll();
    }

    public String inspectStudentFullState(String studentId, String examCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AUDIT INSPECTION FOR: ").append(studentId).append(" [").append(examCode).append("] ===\n");

        Optional<ChainExecutionJournal> journal = journalRepository.findByStudentAndExam(studentId, examCode);
        sb.append("Chain Journal State: ").append(journal.map(ChainExecutionJournal::toString).orElse("NO_CHAIN_ON_DISK")).append("\n");

        Optional<VerificationRecord> intake = intakeService.getRecord(studentId, examCode);
        sb.append("Subsystem A (Intake): ").append(intake.map(VerificationRecord::toString).orElse("NO_INTAKE_ON_DISK")).append("\n");

        Optional<MonitoringSession> monitor = monitoringService.getSession(studentId, examCode);
        sb.append("Subsystem B (Monitoring): ").append(monitor.map(MonitoringSession::toString).orElse("NO_MONITORING_ON_DISK")).append("\n");

        Optional<ExamTranscriptRecord> grading = gradingService.getRecord(studentId, examCode);
        sb.append("Subsystem C (Grading): ").append(grading.map(ExamTranscriptRecord::toString).orElse("NO_GRADING_ON_DISK")).append("\n");

        return sb.toString();
    }

    public boolean resumeOrRollbackBrokenChain(String studentId, String examCode) {
        Optional<ChainExecutionJournal> optionalJournal = journalRepository.findByStudentAndExam(studentId, examCode);
        if (optionalJournal.isEmpty()) {
            System.out.println("[Admin Facade] No persisted chain journal found for student: " + studentId);
            return false;
        }

        ChainExecutionJournal journal = optionalJournal.get();
        System.out.println("[Admin Facade] Recovering session from durable store. Found status: " + journal.getStatus());

        if (journal.getStatus() == ChainStepStatus.STAGE_A_COMMITTED || journal.getStatus() == ChainStepStatus.COMPENSATING_A) {
            System.out.println("[Admin Facade] Chain stopped after Step A. Rolling back Stage A to restore consistency...");
            journal.transitionTo(ChainStepStatus.COMPENSATING_A, "Admin recovery: unwinding Stage A");
            journalRepository.save(journal);

            intakeService.compensateReservation(studentId, examCode, "Admin recovery rollback after application restart");

            journal.transitionTo(ChainStepStatus.COMPENSATED_A, "Admin recovery complete: Stage A safely compensated");
            journalRepository.save(journal);
            return true;
        } else if (journal.getStatus() == ChainStepStatus.STAGE_B_COMMITTED || journal.getStatus() == ChainStepStatus.COMPENSATING_B_A) {
            System.out.println("[Admin Facade] Chain stopped after Step B. Rolling back Stage B then Stage A...");
            journal.transitionTo(ChainStepStatus.COMPENSATING_B_A, "Admin recovery: two-step rollback (B then A)");
            journalRepository.save(journal);

            monitoringService.compensateSession(studentId, examCode, "Admin recovery rollback after application restart");
            intakeService.compensateReservation(studentId, examCode, "Admin recovery rollback after application restart");

            journal.transitionTo(ChainStepStatus.COMPENSATED_B_A, "Admin recovery complete: Two-step rollback finished");
            journalRepository.save(journal);
            return true;
        } else if (journal.getStatus() == ChainStepStatus.CHAIN_COMPLETED) {
            System.out.println("[Admin Facade] Chain was already fully completed and certified.");
            return true;
        }

        System.out.println("[Admin Facade] Chain was already in terminal compensated state: " + journal.getStatus());
        return false;
    }

    public void purgeAllStorage() {
        Path rootData = Paths.get("data");
        if (Files.exists(rootData)) {
            try {
                Files.walk(rootData)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
                System.out.println("[Admin Facade] Storage wiped clean.");
            } catch (IOException e) {
                System.err.println("[Admin Facade] Failed to cleanly purge storage: " + e.getMessage());
            }
        }
    }
}
