package proctoring.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class MonitoringPersistenceGateway {
    private final Path storageDirectory;

    public MonitoringPersistenceGateway() {
        this(Paths.get("data", "monitoring"));
    }

    public MonitoringPersistenceGateway(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize monitoring storage at: " + storageDirectory, e);
        }
    }

    public synchronized void save(MonitoringSession session) {
        Path filePath = resolveFilePath(session.getStudentId(), session.getExamCode());
        String data = session.getSessionId() + "|" +
                session.getStudentId() + "|" +
                session.getExamCode() + "|" +
                session.getStatus().name() + "|" +
                session.getViolationSummary() + "|" +
                session.getStartTime() + "|" +
                session.getEndTime();
        try {
            Files.writeString(filePath, data);
        } catch (IOException e) {
            throw new RuntimeException("Could not persist monitoring session for: " + session.getStudentId(), e);
        }
    }

    public synchronized Optional<MonitoringSession> findByStudentAndExam(String studentId, String examCode) {
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
            MonitoringSession session = new MonitoringSession(
                    parts[0],
                    parts[1],
                    parts[2],
                    MonitoringStatus.valueOf(parts[3]),
                    parts[4],
                    Long.parseLong(parts[5]),
                    Long.parseLong(parts[6])
            );
            return Optional.of(session);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read monitoring session from disk: " + filePath, e);
        }
    }

    public synchronized void delete(String studentId, String examCode) {
        Path filePath = resolveFilePath(studentId, examCode);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    private Path resolveFilePath(String studentId, String examCode) {
        return storageDirectory.resolve("monitoring_" + studentId + "_" + examCode + ".dat");
    }
}
