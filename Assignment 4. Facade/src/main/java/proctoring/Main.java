package proctoring;

import proctoring.chain.ChainJournalRepository;
import proctoring.facade.ExamProctoringFacade;
import proctoring.facade.ExamResultSummary;
import proctoring.facade.ProctoringAdminFacade;
import proctoring.facade.ProctoringChainException;
import proctoring.grading.GradingPersistenceGateway;
import proctoring.grading.GradingService;
import proctoring.grading.SubmissionIntegrityEngine;
import proctoring.intake.BiometricValidator;
import proctoring.intake.IntakePersistenceGateway;
import proctoring.intake.IntakeService;
import proctoring.monitoring.HardwareIntegrityRule;
import proctoring.monitoring.MonitoringPersistenceGateway;
import proctoring.monitoring.MonitoringService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Initialize subsystems and persistence gateways
        IntakePersistenceGateway intakeGateway = new IntakePersistenceGateway();
        IntakeService intakeService = new IntakeService(new BiometricValidator(), intakeGateway);

        MonitoringPersistenceGateway monitoringGateway = new MonitoringPersistenceGateway();
        MonitoringService monitoringService = new MonitoringService(new HardwareIntegrityRule(), monitoringGateway);

        GradingPersistenceGateway gradingGateway = new GradingPersistenceGateway();
        GradingService gradingService = new GradingService(new SubmissionIntegrityEngine(), gradingGateway);

        ChainJournalRepository journalRepository = new ChainJournalRepository();

        // Initialize Facades
        ExamProctoringFacade clientFacade = new ExamProctoringFacade(intakeService, monitoringService, gradingService, journalRepository);
        ProctoringAdminFacade adminFacade = new ProctoringAdminFacade(intakeService, monitoringService, gradingService, journalRepository);

        System.out.println("================================================================================");
        System.out.println("   ASSIGNMENT 4: EXAM PROCTORING SYSTEM (FACADE OVER THREE-STAGE CHAIN)         ");
        System.out.println("================================================================================");

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        // If non-interactive environment or CLI, run comprehensive showcase by default
        if (args.length > 0 && args[0].equalsIgnoreCase("--demo")) {
            runAutomatedDemonstration(clientFacade, adminFacade);
            return;
        }

        while (running) {
            printMenu();
            System.out.print("Select an option [1-7]: ");
            String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "1";

            switch (input) {
                case "1":
                    runHappyPath(clientFacade, adminFacade);
                    break;
                case "2":
                    runCompensationDepth1(clientFacade, adminFacade);
                    break;
                case "3":
                    runCompensationDepth2(clientFacade, adminFacade);
                    break;
                case "4":
                    simulateCrashAndRecovery(intakeService, monitoringService, gradingService, journalRepository, adminFacade);
                    break;
                case "5":
                    System.out.println("\n" + adminFacade.inspectStudentFullState("S1001", "CS301"));
                    break;
                case "6":
                    adminFacade.purgeAllStorage();
                    break;
                case "7":
                    runAutomatedDemonstration(clientFacade, adminFacade);
                    running = false;
                    break;
                default:
                    System.out.println("Exiting application.");
                    running = false;
                    break;
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n----------------- LIVE PROCTORING MENU -----------------");
        System.out.println("1. Run Happy Path (Stages A -> B -> C succeed)");
        System.out.println("2. Trigger Failure at Stage B (Compensation Depth 1: rolls back A)");
        System.out.println("3. Trigger Failure at Stage C (Compensation Depth 2: rolls back B then A)");
        System.out.println("4. Simulate Application Crash & Resume From Disk Store");
        System.out.println("5. Inspect Persisted Store for Student S1001");
        System.out.println("6. Purge All Persisted Data");
        System.out.println("7. Run Full Automated Defense Showcase & Exit");
        System.out.println("---------------------------------------------------------");
    }

    public static void runHappyPath(ExamProctoringFacade clientFacade, ProctoringAdminFacade adminFacade) {
        System.out.println("\n>>> [1] EXECUTING HAPPY PATH (Normal A -> B -> C)");
        String answers = "Q1=CORRECT;Q2=CORRECT;Q3=CORRECT";
        String checksum = SubmissionIntegrityEngine.computeSha256(answers);

        try {
            ExamResultSummary result = clientFacade.conductExamSession(
                    "S1001", "CS301", "DOC-PASSPORT-5521",
                    1, true, "chrome.exe",
                    answers, checksum
            );
            System.out.println("SUCCESS: " + result);
        } catch (ProctoringChainException ex) {
            System.err.println("Unexpected failure: " + ex.getMessage());
        }

        System.out.println("\nPersisted State Verification:");
        System.out.println(adminFacade.inspectStudentFullState("S1001", "CS301"));
    }

    public static void runCompensationDepth1(ExamProctoringFacade clientFacade, ProctoringAdminFacade adminFacade) {
        System.out.println("\n>>> [2] EXECUTING COMPENSATION DEPTH 1 (Step B Fails after A committed)");
        System.out.println("Simulating multi-display cheat attempt (activeDisplays = 2)...");
        String answers = "Q1=CORRECT;Q2=CORRECT";
        String checksum = SubmissionIntegrityEngine.computeSha256(answers);

        try {
            clientFacade.conductExamSession(
                    "S1002", "CS301", "DOC-ID-8841",
                    2, true, "chrome.exe", // activeDisplays = 2 causes Stage B failure
                    answers, checksum
            );
        } catch (ProctoringChainException ex) {
            System.out.println("Expected Chain Exception Caught: " + ex.getMessage());
        }

        System.out.println("\nPersisted State Verification (Notice Stage A is REVOKED):");
        System.out.println(adminFacade.inspectStudentFullState("S1002", "CS301"));
    }

    public static void runCompensationDepth2(ExamProctoringFacade clientFacade, ProctoringAdminFacade adminFacade) {
        System.out.println("\n>>> [3] EXECUTING COMPENSATION DEPTH 2 (Step C Fails after A and B committed)");
        System.out.println("Simulating corrupt answers packet checksum mismatch in Stage C...");
        String answers = "Q1=CORRECT;Q2=CORRECT";
        String invalidChecksum = "0000000000000000000000000000000000000000000000000000000000000000";

        try {
            clientFacade.conductExamSession(
                    "S1003", "CS301", "DOC-ID-9912",
                    1, true, "chrome.exe",
                    answers, invalidChecksum // Causes Stage C checksum failure
            );
        } catch (ProctoringChainException ex) {
            System.out.println("Expected Chain Exception Caught: " + ex.getMessage());
        }

        System.out.println("\nPersisted State Verification (Notice B is INVALIDATED and A is REVOKED):");
        System.out.println(adminFacade.inspectStudentFullState("S1003", "CS301"));
    }

    public static void simulateCrashAndRecovery(IntakeService intakeService,
                                               MonitoringService monitoringService,
                                               GradingService gradingService,
                                               ChainJournalRepository journalRepository,
                                               ProctoringAdminFacade adminFacade) {
        System.out.println("\n>>> [4] SIMULATING APPLICATION CRASH & RESTART RESUMPTION");
        String studentId = "S1004";
        String examCode = "CS301";

        // Simulate application committing Step A, then instantly terminating (crashing)
        System.out.println("Process committing Step A directly to disk...");
        intakeService.verifyAndReserveSlot(studentId, examCode, "DOC-NATIONAL-4412");
        proctoring.chain.ChainExecutionJournal journal = new proctoring.chain.ChainExecutionJournal(studentId, examCode);
        journal.transitionTo(proctoring.chain.ChainStepStatus.STAGE_A_COMMITTED, "Simulated crash right after Stage A");
        journalRepository.save(journal);

        System.out.println("CRASH! Application killed in memory. Only files on disk survive.");
        System.out.println("Restarting application fresh from disk storage...");

        // Recreate brand new facade instances pointing to the same disk storage
        ProctoringAdminFacade freshRestartedAdmin = new ProctoringAdminFacade(
                new IntakeService(new BiometricValidator(), new IntakePersistenceGateway()),
                new MonitoringService(new HardwareIntegrityRule(), new MonitoringPersistenceGateway()),
                new GradingService(new SubmissionIntegrityEngine(), new GradingPersistenceGateway()),
                new ChainJournalRepository()
        );

        System.out.println("Running recovery on restarted application:");
        freshRestartedAdmin.resumeOrRollbackBrokenChain(studentId, examCode);

        System.out.println("\nPersisted State After Recovery from Disk:");
        System.out.println(freshRestartedAdmin.inspectStudentFullState(studentId, examCode));
    }

    private static void runAutomatedDemonstration(ExamProctoringFacade clientFacade, ProctoringAdminFacade adminFacade) {
        System.out.println("\n=== RUNNING FULL AUTOMATED DEFENSE DEMONSTRATION ===");
        runHappyPath(clientFacade, adminFacade);
        runCompensationDepth1(clientFacade, adminFacade);
        runCompensationDepth2(clientFacade, adminFacade);
        System.out.println("\n=== AUTOMATED SHOWCASE COMPLETED ===");
    }
}
