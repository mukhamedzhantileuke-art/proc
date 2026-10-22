package proctoring.chain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ChainJournalRepository {
    private final Path storageDirectory;

    public ChainJournalRepository() {
        this(Paths.get("data", "chains"));
    }

    public ChainJournalRepository(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize chain journal storage at: " + storageDirectory, e);
        }
    }

    public synchronized void save(ChainExecutionJournal journal) {
        Path filePath = resolveFilePath(journal.getStudentId(), journal.getExamCode());
        String data = journal.getStudentId() + "|" +
                journal.getExamCode() + "|" +
                journal.getStatus().name() + "|" +
                journal.getStatusMessage().replace("|", "/") + "|" +
                journal.getLastUpdated();
        try {
            Files.writeString(filePath, data);
        } catch (IOException e) {
            throw new RuntimeException("Could not persist chain journal for: " + journal.getStudentId(), e);
        }
    }

    public synchronized Optional<ChainExecutionJournal> findByStudentAndExam(String studentId, String examCode) {
        Path filePath = resolveFilePath(studentId, examCode);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(filePath).trim();
            if (content.isEmpty()) {
                return Optional.empty();
            }
            String[] parts = content.split("\\|");
            ChainExecutionJournal journal = new ChainExecutionJournal(
                    parts[0],
                    parts[1],
                    ChainStepStatus.valueOf(parts[2]),
                    parts[3],
                    Long.parseLong(parts[4])
            );
            return Optional.of(journal);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read chain journal from disk: " + filePath, e);
        }
    }

    public synchronized List<ChainExecutionJournal> findAll() {
        List<ChainExecutionJournal> list = new ArrayList<>();
        if (!Files.exists(storageDirectory)) {
            return list;
        }
        try (Stream<Path> stream = Files.list(storageDirectory)) {
            stream.filter(p -> p.toString().endsWith(".dat")).forEach(path -> {
                try {
                    String content = Files.readString(path).trim();
                    if (!content.isEmpty()) {
                        String[] parts = content.split("\\|");
                        list.add(new ChainExecutionJournal(
                                parts[0],
                                parts[1],
                                ChainStepStatus.valueOf(parts[2]),
                                parts[3],
                                Long.parseLong(parts[4])
                        ));
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (IOException ignored) {
        }
        return list;
    }

    public synchronized void delete(String studentId, String examCode) {
        Path filePath = resolveFilePath(studentId, examCode);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    private Path resolveFilePath(String studentId, String examCode) {
        return storageDirectory.resolve("chain_" + studentId + "_" + examCode + ".dat");
    }
}
