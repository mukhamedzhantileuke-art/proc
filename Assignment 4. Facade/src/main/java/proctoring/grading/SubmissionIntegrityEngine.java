package proctoring.grading;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SubmissionIntegrityEngine {

    public boolean verifySubmissionChecksum(String answersPayload, String expectedChecksum) {
        if (answersPayload == null || answersPayload.trim().isEmpty()) {
            return false;
        }
        if (expectedChecksum == null || expectedChecksum.trim().isEmpty()) {
            return false;
        }
        String computed = computeSha256(answersPayload);
        return computed.equalsIgnoreCase(expectedChecksum);
    }

    public double computeScore(String answersPayload) {
        if (answersPayload == null) {
            return 0.0;
        }
        int validAnswers = 0;
        String[] tokens = answersPayload.split(";");
        for (String token : tokens) {
            if (token.contains("=CORRECT")) {
                validAnswers++;
            }
        }
        return tokens.length > 0 ? (validAnswers * 100.0) / tokens.length : 0.0;
    }

    public String determineLetterGrade(double score) {
        if (score >= 90.0) return "A";
        if (score >= 80.0) return "B";
        if (score >= 70.0) return "C";
        if (score >= 60.0) return "D";
        return "F";
    }

    public static String computeSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
