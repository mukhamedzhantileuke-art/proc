package proctoring.intake;

public class BiometricValidator {

    public boolean validateStudentCredentials(String studentId, String documentNumber) {
        if (studentId == null || !studentId.matches("^S[0-9]{4,8}$")) {
            return false;
        }
        if (documentNumber == null || documentNumber.trim().length() < 5) {
            return false;
        }
        return !studentId.endsWith("999");
    }

    public boolean isStudentSuspended(String studentId) {
        return studentId != null && studentId.contains("SUSPEND");
    }
}
