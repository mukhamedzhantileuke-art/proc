package proctoring.facade;

public class ProctoringChainException extends RuntimeException {
    public ProctoringChainException(String message) {
        super(message);
    }

    public ProctoringChainException(String message, Throwable cause) {
        super(message, cause);
    }
}
