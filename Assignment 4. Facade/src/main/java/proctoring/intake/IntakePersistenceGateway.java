package proctoring.intake;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class IntakePersistenceGateway {
    private final Path storageDirectory;

    public IntakePersistenceGateway() {
        this(Paths.get("data", "intake"));
    }

    public IntakePersistenceGateway(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize intake storage at: " + storageDirectory, e);
        }
    }

    public synchronized void save(VerificationRecord record) {
        Path filePath = resolveFilePath(record.getStudentId(), record.getExamCode());
        String data = record.getStudentId() + "|" +
                record.getExamCode() + "|" +
                record.getDocumentNumber() + "|" +
                record.getStatus().name() + "|" +
                record.getCreatedAt() + "|" +
                record.getUpdatedAt();
        try {
            Files.writeString(filePath, data);
        } catch (IOException e) {
            throw new RuntimeException("Could not persist intake record for: " + record.getStudentId(), e);
        }
    }

    public synchronized Optional<VerificationRecord> findByStudentAndExam(String studentId, String examCode) {
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
            VerificationRecord record = new VerificationRecord(
                    parts[0],
                    parts[1],
                    parts[2],
                    VerificationStatus.valueOf(parts[3]),
                    Long.parseLong(parts[4]),
                    Long.parseLong(parts[5])
            );
            return Optional.of(record);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read intake record from disk: " + filePath, e);
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
        return storageDirectory.resolve("intake_" + studentId + "_" + examCode + ".dat");
    }
}
