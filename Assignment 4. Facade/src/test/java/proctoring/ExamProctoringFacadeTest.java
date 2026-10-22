package proctoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import proctoring.chain.ChainExecutionJournal;
import proctoring.chain.ChainJournalRepository;
import proctoring.chain.ChainStepStatus;
import proctoring.facade.ExamProctoringFacade;
import proctoring.facade.ExamResultSummary;
import proctoring.facade.ProctoringAdminFacade;
import proctoring.facade.ProctoringChainException;
import proctoring.grading.*;
import proctoring.intake.*;
import proctoring.monitoring.*;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ExamProctoringFacadeTest {

    @TempDir
    Path tempStorage;

    private IntakeService intakeService;
    private MonitoringService monitoringService;
    private GradingService gradingService;
    private ChainJournalRepository journalRepository;

    private ExamProctoringFacade clientFacade;
    private ProctoringAdminFacade adminFacade;

    @BeforeEach
    void setUp() {
        IntakePersistenceGateway intakeGateway = new IntakePersistenceGateway(tempStorage.resolve("intake"));
        intakeService = new IntakeService(new BiometricValidator(), intakeGateway);

        MonitoringPersistenceGateway monitoringGateway = new MonitoringPersistenceGateway(tempStorage.resolve("monitoring"));
        monitoringService = new MonitoringService(new HardwareIntegrityRule(), monitoringGateway);

        GradingPersistenceGateway gradingGateway = new GradingPersistenceGateway(tempStorage.resolve("grading"));
        gradingService = new GradingService(new SubmissionIntegrityEngine(), gradingGateway);

        journalRepository = new ChainJournalRepository(tempStorage.resolve("chains"));

        clientFacade = new ExamProctoringFacade(intakeService, monitoringService, gradingService, journalRepository);
        adminFacade = new ProctoringAdminFacade(intakeService, monitoringService, gradingService, journalRepository);
    }

    @AfterEach
    void tearDown() {
        adminFacade.purgeAllStorage();
    }

    // =========================================================================
    // DOMAIN-SPECIFIC TESTING DSL BUILDER (Section 3.7)
    // Makes test scenarios read as fluent English sentences in the domain!
    // =========================================================================
    private ExamScenarioBuilder givenExamScenario(String studentId, String examCode) {
        return new ExamScenarioBuilder(studentId, examCode);
    }

    private class ExamScenarioBuilder {
        private final String studentId;
        private final String examCode;
        private String document = "DOC-ID-VALID-101";
        private int activeDisplays = 1;
        private boolean webcamActive = true;
        private String runningProcess = "chrome.exe";
        private String answers = "Q1=CORRECT;Q2=CORRECT";
        private String checksum = SubmissionIntegrityEngine.computeSha256("Q1=CORRECT;Q2=CORRECT");

        public ExamScenarioBuilder(String studentId, String examCode) {
            this.studentId = studentId;
            this.examCode = examCode;
        }

        public ExamScenarioBuilder withSuspiciousDisplays(int count) {
            this.activeDisplays = count;
            return this;
        }

        public ExamScenarioBuilder withCorruptedChecksum(String fakeChecksum) {
            this.checksum = fakeChecksum;
            return this;
        }

        public ExamScenarioBuilder withForbiddenProcess(String processName) {
            this.runningProcess = processName;
            return this;
        }

        public ExamResultSummary whenExecutingChain() {
            return clientFacade.conductExamSession(
                    studentId, examCode, document,
                    activeDisplays, webcamActive, runningProcess,
                    answers, checksum
            );
        }

        public ProctoringChainException whenExpectingFailure() {
            return assertThrows(ProctoringChainException.class, this::whenExecutingChain);
        }
    }

    // =========================================================================
    // TEST CASES
    // =========================================================================

    @Test
    @DisplayName("Happy Path: Stages A -> B -> C succeed and store is marked CHAIN_COMPLETED")
    void testHappyPathExecution() {
        ExamResultSummary result = givenExamScenario("S2001", "CS301").whenExecutingChain();

        assertNotNull(result);
        assertEquals(VerificationStatus.VERIFIED_RESERVED, result.getIntakeStatus());
        assertEquals(MonitoringStatus.PASSED_CLEAN, result.getMonitoringStatus());
        assertEquals(GradingStatus.CERTIFIED_RECORDED, result.getGradingStatus());
        assertEquals(100.0, result.getScore());
        assertEquals("A", result.getLetterGrade());

        // Verify durable storage on disk
        Optional<ChainExecutionJournal> journal = journalRepository.findByStudentAndExam("S2001", "CS301");
        assertTrue(journal.isPresent());
        assertEquals(ChainStepStatus.CHAIN_COMPLETED, journal.get().getStatus());
    }

    @Test
    @DisplayName("Compensation Depth 1: Step B fails after Step A committed -> Step A is unwound and REVOKED")
    void testCompensationDepth1_BFailsAfterACommitted() {
        ProctoringChainException exception = givenExamScenario("S2002", "CS301")
                .withSuspiciousDisplays(2) // Triggers failure in Stage B
                .whenExpectingFailure();

        assertTrue(exception.getMessage().contains("Stage B"));

        // Verify on-disk compensation
        Optional<VerificationRecord> intakeRecord = intakeService.getRecord("S2002", "CS301");
        assertTrue(intakeRecord.isPresent());
        assertEquals(VerificationStatus.REVOKED, intakeRecord.get().getStatus(), "Step A must be compensated and revoked");

        Optional<MonitoringSession> session = monitoringService.getSession("S2002", "CS301");
        assertTrue(session.isPresent());
        assertEquals(MonitoringStatus.FLAGGED_VIOLATION, session.get().getStatus());

        Optional<ExamTranscriptRecord> gradingRecord = gradingService.getRecord("S2002", "CS301");
        assertTrue(gradingRecord.isEmpty(), "Step C should never have been invoked");

        Optional<ChainExecutionJournal> journal = journalRepository.findByStudentAndExam("S2002", "CS301");
        assertTrue(journal.isPresent());
        assertEquals(ChainStepStatus.COMPENSATED_A, journal.get().getStatus());
    }

    @Test
    @DisplayName("Compensation Depth 2: Step C fails after A and B committed -> Two-step rollback unwinds B then A")
    void testCompensationDepth2_CFailsAfterAAndBCommitted() {
        ProctoringChainException exception = givenExamScenario("S2003", "CS301")
                .withCorruptedChecksum("CORRUPT_CHECKSUM_HASH") // Triggers failure in Stage C
                .whenExpectingFailure();

        assertTrue(exception.getMessage().contains("Stage C"));

        // Verify Two-step rollback in durable store:
        // 1. Subsystem B compensated
        Optional<MonitoringSession> session = monitoringService.getSession("S2003", "CS301");
        assertTrue(session.isPresent());
        assertEquals(MonitoringStatus.ROLLBACK_INVALIDATED, session.get().getStatus(), "Step B must be rolled back to INVALIDATED");

        // 2. Subsystem A compensated
        Optional<VerificationRecord> intakeRecord = intakeService.getRecord("S2003", "CS301");
        assertTrue(intakeRecord.isPresent());
        assertEquals(VerificationStatus.REVOKED, intakeRecord.get().getStatus(), "Step A must be rolled back to REVOKED");

        // 3. Subsystem C rejected
        Optional<ExamTranscriptRecord> gradingRecord = gradingService.getRecord("S2003", "CS301");
        assertTrue(gradingRecord.isPresent());
        assertEquals(GradingStatus.INTEGRITY_REJECTED, gradingRecord.get().getStatus());

        // 4. Chain Journal marked COMPENSATED_B_A
        Optional<ChainExecutionJournal> journal = journalRepository.findByStudentAndExam("S2003", "CS301");
        assertTrue(journal.isPresent());
        assertEquals(ChainStepStatus.COMPENSATED_B_A, journal.get().getStatus());
    }

    @Test
    @DisplayName("Persistence Reload Test: Application stops mid-chain, restarts, and recovers state from disk")
    void testPersistenceReloadAndRestartRecovery() {
        String studentId = "S2004";
        String examCode = "CS301";

        // Step 1: Simulate application committing Step A and then instantly crashing
        intakeService.verifyAndReserveSlot(studentId, examCode, "DOC-12345");
        ChainExecutionJournal journal = new ChainExecutionJournal(studentId, examCode);
        journal.transitionTo(ChainStepStatus.STAGE_A_COMMITTED, "Simulated crash after Stage A commit");
        journalRepository.save(journal);

        // Step 2: Simulate application shutdown - dereference all in-memory services
        intakeService = null;
        monitoringService = null;
        gradingService = null;
        journalRepository = null;
        clientFacade = null;
        adminFacade = null;

        // Step 3: Brand new application instance starts, reading strictly from disk
        IntakePersistenceGateway freshIntakeGateway = new IntakePersistenceGateway(tempStorage.resolve("intake"));
        IntakeService freshIntakeService = new IntakeService(new BiometricValidator(), freshIntakeGateway);

        MonitoringPersistenceGateway freshMonitoringGateway = new MonitoringPersistenceGateway(tempStorage.resolve("monitoring"));
        MonitoringService freshMonitoringService = new MonitoringService(new HardwareIntegrityRule(), freshMonitoringGateway);

        GradingPersistenceGateway freshGradingGateway = new GradingPersistenceGateway(tempStorage.resolve("grading"));
        GradingService freshGradingService = new GradingService(new SubmissionIntegrityEngine(), freshGradingGateway);

        ChainJournalRepository freshJournalRepo = new ChainJournalRepository(tempStorage.resolve("chains"));
        ProctoringAdminFacade freshAdminFacade = new ProctoringAdminFacade(freshIntakeService, freshMonitoringService, freshGradingService, freshJournalRepo);

        // Step 4: Verify recovery from disk
        boolean recovered = freshAdminFacade.resumeOrRollbackBrokenChain(studentId, examCode);
        assertTrue(recovered);

        // Check that Stage A on disk is now cleanly compensated
        Optional<VerificationRecord> record = freshIntakeService.getRecord(studentId, examCode);
        assertTrue(record.isPresent());
        assertEquals(VerificationStatus.REVOKED, record.get().getStatus());

        Optional<ChainExecutionJournal> recoveredJournal = freshJournalRepo.findByStudentAndExam(studentId, examCode);
        assertTrue(recoveredJournal.isPresent());
        assertEquals(ChainStepStatus.COMPENSATED_A, recoveredJournal.get().getStatus());
    }

    @Test
    @DisplayName("Failure Before Commit: Suspended student is rejected before any state is committed to disk")
    void testFailureBeforeCommit_SuspendedStudent() {
        assertThrows(ProctoringChainException.class, () ->
                givenExamScenario("S_SUSPENDED_99", "CS301").whenExecutingChain()
        );

        // Verify nothing was committed
        assertTrue(intakeService.getRecord("S_SUSPENDED_99", "CS301").isEmpty());
        assertTrue(monitoringService.getSession("S_SUSPENDED_99", "CS301").isEmpty());
        assertTrue(gradingService.getRecord("S_SUSPENDED_99", "CS301").isEmpty());
    }
}
