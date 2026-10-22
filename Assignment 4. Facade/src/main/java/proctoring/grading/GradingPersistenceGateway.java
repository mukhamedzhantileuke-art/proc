package proctoring.grading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class GradingPersistenceGateway {
    private final Path storageDirectory;

    public GradingPersistenceGateway() {
        this(Paths.get("data", "grading"));
    }

    public GradingPersistenceGateway(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize grading storage at: " + storageDirectory, e);
        }
    }

    public synchronized void save(ExamTranscriptRecord record) {
        Path filePath = resolveFilePath(record.getStudentId(), record.getExamCode());
        String data = record.getTranscriptId() + "|" +
                record.getStudentId() + "|" +
                record.getExamCode() + "|" +
                record.getStatus().name() + "|" +
                record.getFinalScore() + "|" +
                record.getLetterGrade() + "|" +
                record.getRemarks() + "|" +
                record.getTimestamp();
        try {
            Files.writeString(filePath, data);
        } catch (IOException e) {
            throw new RuntimeException("Could not persist transcript record for: " + record.getStudentId(), e);
        }
    }

    public synchronized Optional<ExamTranscriptRecord> findByStudentAndExam(String studentId, String examCode) {
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
            ExamTranscriptRecord record = new ExamTranscriptRecord(
                    parts[0],
                    parts[1],
                    parts[2],
                    GradingStatus.valueOf(parts[3]),
                    Double.parseDouble(parts[4]),
                    parts[5],
                    parts[6],
                    Long.parseLong(parts[7])
            );
            return Optional.of(record);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read transcript record from disk: " + filePath, e);
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
        return storageDirectory.resolve("grading_" + studentId + "_" + examCode + ".dat");
    }
}
