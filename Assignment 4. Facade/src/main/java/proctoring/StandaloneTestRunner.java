package proctoring;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

/**
 * Autonomous Test Runner without external dependencies.
 * Runs directly with standard 'java' command or IntelliJ Green Play button.
 * Validates all core requirements: F.I.R.S.T., Happy Path, Both Compensation Depths, and Restart/Resume.
 */
public class StandaloneTestRunner {
    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("               RUNNING AUTONOMOUS EXAM PROCTORING TEST SUITE                    ");
        System.out.println("================================================================================");

        Path testDir = Paths.get("data_test_run");
        cleanDirectory(testDir);

        testHappyPath(testDir);
        testCompensationDepth1(testDir);
        testCompensationDepth2(testDir);
        testRestartAndResumeFromDisk(testDir);
        testFailureBeforeCommit(testDir);

        cleanDirectory(testDir);

        System.out.println("\n================================================================================");
        System.out.println("TEST RESULTS: " + passedCount + " PASSED, " + failedCount + " FAILED");
        System.out.println("================================================================================");
        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void testHappyPath(Path testDir) {
        System.out.print("\n[TEST 1] Happy Path (Stages A -> B -> C succeed)... ");
        cleanDirectory(testDir);

        IntakeService intake = new IntakeService(new BiometricValidator(), new IntakePersistenceGateway(testDir.resolve("intake")));
        MonitoringService monitoring = new MonitoringService(new HardwareIntegrityRule(), new MonitoringPersistenceGateway(testDir.resolve("monitoring")));
        GradingService grading = new GradingService(new SubmissionIntegrityEngine(), new GradingPersistenceGateway(testDir.resolve("grading")));
        ChainJournalRepository repo = new ChainJournalRepository(testDir.resolve("chains"));
        ExamProctoringFacade facade = new ExamProctoringFacade(intake, monitoring, grading, repo);

        String answers = "Q1=CORRECT;Q2=CORRECT";
        String checksum = SubmissionIntegrityEngine.computeSha256(answers);

        ExamResultSummary result = facade.conductExamSession(
                "S101", "CS301", "DOC-ID-12345",
                1, true, "chrome.exe",
                answers, checksum
        );

        assertCondition(result.getIntakeStatus() == VerificationStatus.VERIFIED_RESERVED, "Intake must be verified");
        assertCondition(result.getMonitoringStatus() == MonitoringStatus.PASSED_CLEAN, "Monitoring must be passed");
        assertCondition(result.getGradingStatus() == GradingStatus.CERTIFIED_RECORDED, "Grading must be certified");
        assertCondition(result.getScore() == 100.0, "Score must be 100");

        Optional<ChainExecutionJournal> journal = repo.findByStudentAndExam("S101", "CS301");
        assertCondition(journal.isPresent() && journal.get().getStatus() == ChainStepStatus.CHAIN_COMPLETED, "Journal must be CHAIN_COMPLETED");

        System.out.println("PASSED");
        passedCount++;
    }

    private static void testCompensationDepth1(Path testDir) {
        System.out.print("[TEST 2] Compensation Depth 1 (Step B fails, rolls back Step A)... ");
        cleanDirectory(testDir);

        IntakeService intake = new IntakeService(new BiometricValidator(), new IntakePersistenceGateway(testDir.resolve("intake")));
        MonitoringService monitoring = new MonitoringService(new HardwareIntegrityRule(), new MonitoringPersistenceGateway(testDir.resolve("monitoring")));
        GradingService grading = new GradingService(new SubmissionIntegrityEngine(), new GradingPersistenceGateway(testDir.resolve("grading")));
        ChainJournalRepository repo = new ChainJournalRepository(testDir.resolve("chains"));
        ExamProctoringFacade facade = new ExamProctoringFacade(intake, monitoring, grading, repo);

        String answers = "Q1=CORRECT";
        String checksum = SubmissionIntegrityEngine.computeSha256(answers);

        boolean thrown = false;
        try {
            facade.conductExamSession(
                    "S102", "CS301", "DOC-ID-12345",
                    2, true, "chrome.exe", // activeDisplays = 2 triggers B failure
                    answers, checksum
            );
        } catch (ProctoringChainException ex) {
            thrown = true;
        }

        assertCondition(thrown, "ProctoringChainException must be thrown on Stage B failure");

        Optional<VerificationRecord> intakeRecord = intake.getRecord("S102", "CS301");
        assertCondition(intakeRecord.isPresent() && intakeRecord.get().getStatus() == VerificationStatus.REVOKED,
                "Stage A must be compensated to REVOKED");

        Optional<MonitoringSession> monSession = monitoring.getSession("S102", "CS301");
        assertCondition(monSession.isPresent() && monSession.get().getStatus() == MonitoringStatus.FLAGGED_VIOLATION,
                "Stage B must be FLAGGED_VIOLATION");

        Optional<ChainExecutionJournal> journal = repo.findByStudentAndExam("S102", "CS301");
        assertCondition(journal.isPresent() && journal.get().getStatus() == ChainStepStatus.COMPENSATED_A,
                "Journal must be marked COMPENSATED_A");

        System.out.println("PASSED");
        passedCount++;
    }

    private static void testCompensationDepth2(Path testDir) {
        System.out.print("[TEST 3] Compensation Depth 2 (Step C fails, two-step rollback B then A)... ");
        cleanDirectory(testDir);

        IntakeService intake = new IntakeService(new BiometricValidator(), new IntakePersistenceGateway(testDir.resolve("intake")));
        MonitoringService monitoring = new MonitoringService(new HardwareIntegrityRule(), new MonitoringPersistenceGateway(testDir.resolve("monitoring")));
        GradingService grading = new GradingService(new SubmissionIntegrityEngine(), new GradingPersistenceGateway(testDir.resolve("grading")));
        ChainJournalRepository repo = new ChainJournalRepository(testDir.resolve("chains"));
        ExamProctoringFacade facade = new ExamProctoringFacade(intake, monitoring, grading, repo);

        String answers = "Q1=CORRECT";
        String badChecksum = "INVALID_CORRUPT_HASH";

        boolean thrown = false;
        try {
            facade.conductExamSession(
                    "S103", "CS301", "DOC-ID-12345",
                    1, true, "chrome.exe",
                    answers, badChecksum // Bad checksum triggers C failure
            );
        } catch (ProctoringChainException ex) {
            thrown = true;
        }

        assertCondition(thrown, "ProctoringChainException must be thrown on Stage C failure");

        Optional<MonitoringSession> monSession = monitoring.getSession("S103", "CS301");
        assertCondition(monSession.isPresent() && monSession.get().getStatus() == MonitoringStatus.ROLLBACK_INVALIDATED,
                "Stage B must be unwound to ROLLBACK_INVALIDATED");

        Optional<VerificationRecord> intakeRecord = intake.getRecord("S103", "CS301");
        assertCondition(intakeRecord.isPresent() && intakeRecord.get().getStatus() == VerificationStatus.REVOKED,
                "Stage A must be unwound to REVOKED");

        Optional<ChainExecutionJournal> journal = repo.findByStudentAndExam("S103", "CS301");
        assertCondition(journal.isPresent() && journal.get().getStatus() == ChainStepStatus.COMPENSATED_B_A,
                "Journal must be marked COMPENSATED_B_A");

        System.out.println("PASSED");
        passedCount++;
    }

    private static void testRestartAndResumeFromDisk(Path testDir) {
        System.out.print("[TEST 4] Persistence Reload (Application stops mid-chain and recovers from disk)... ");
        cleanDirectory(testDir);

        IntakePersistenceGateway intakeGw = new IntakePersistenceGateway(testDir.resolve("intake"));
        IntakeService intake = new IntakeService(new BiometricValidator(), intakeGw);
        ChainJournalRepository repo = new ChainJournalRepository(testDir.resolve("chains"));

        // Step 1: Simulate crash after Step A
        intake.verifyAndReserveSlot("S104", "CS301", "DOC-ID-9999");
        ChainExecutionJournal journal = new ChainExecutionJournal("S104", "CS301");
        journal.transitionTo(ChainStepStatus.STAGE_A_COMMITTED, "Simulated crash after A commit");
        repo.save(journal);

        // Step 2: Restart application from disk
        IntakeService restartedIntake = new IntakeService(new BiometricValidator(), new IntakePersistenceGateway(testDir.resolve("intake")));
        MonitoringService restartedMon = new MonitoringService(new HardwareIntegrityRule(), new MonitoringPersistenceGateway(testDir.resolve("monitoring")));
        GradingService restartedGrading = new GradingService(new SubmissionIntegrityEngine(), new GradingPersistenceGateway(testDir.resolve("grading")));
        ChainJournalRepository restartedRepo = new ChainJournalRepository(testDir.resolve("chains"));
        ProctoringAdminFacade admin = new ProctoringAdminFacade(restartedIntake, restartedMon, restartedGrading, restartedRepo);

        // Step 3: Run recovery from disk
        boolean recovered = admin.resumeOrRollbackBrokenChain("S104", "CS301");
        assertCondition(recovered, "Admin facade must successfully recover unfinished chain from disk");

        Optional<VerificationRecord> recoveredRecord = restartedIntake.getRecord("S104", "CS301");
        assertCondition(recoveredRecord.isPresent() && recoveredRecord.get().getStatus() == VerificationStatus.REVOKED,
                "Recovered Stage A record on disk must be REVOKED");

        Optional<ChainExecutionJournal> recoveredJournal = restartedRepo.findByStudentAndExam("S104", "CS301");
        assertCondition(recoveredJournal.isPresent() && recoveredJournal.get().getStatus() == ChainStepStatus.COMPENSATED_A,
                "Recovered journal on disk must be COMPENSATED_A");

        System.out.println("PASSED");
        passedCount++;
    }

    private static void testFailureBeforeCommit(Path testDir) {
        System.out.print("[TEST 5] Failure Before Commit (Suspended student rejected without persisting)... ");
        cleanDirectory(testDir);

        IntakeService intake = new IntakeService(new BiometricValidator(), new IntakePersistenceGateway(testDir.resolve("intake")));
        MonitoringService monitoring = new MonitoringService(new HardwareIntegrityRule(), new MonitoringPersistenceGateway(testDir.resolve("monitoring")));
        GradingService grading = new GradingService(new SubmissionIntegrityEngine(), new GradingPersistenceGateway(testDir.resolve("grading")));
        ChainJournalRepository repo = new ChainJournalRepository(testDir.resolve("chains"));
        ExamProctoringFacade facade = new ExamProctoringFacade(intake, monitoring, grading, repo);

        boolean thrown = false;
        try {
            facade.conductExamSession(
                    "S_SUSPEND_01", "CS301", "DOC-ID-12345",
                    1, true, "chrome.exe",
                    "Q1=CORRECT", "fake"
            );
        } catch (ProctoringChainException ex) {
            thrown = true;
        }

        assertCondition(thrown, "Must reject suspended student");
        assertCondition(intake.getRecord("S_SUSPEND_01", "CS301").isEmpty(), "Nothing must be committed in Stage A");

        System.out.println("PASSED");
        passedCount++;
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            failedCount++;
            throw new AssertionError("Assertion Failed: " + message);
        }
    }

    private static void cleanDirectory(Path dir) {
        if (Files.exists(dir)) {
            try {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            } catch (IOException ignored) {
            }
        }
    }
}
