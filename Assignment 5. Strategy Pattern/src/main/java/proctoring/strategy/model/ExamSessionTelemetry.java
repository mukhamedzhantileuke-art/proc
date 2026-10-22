package proctoring.strategy.model;

public class ExamSessionTelemetry {
    private final String studentId;
    private final double gazeAversionSeconds;
    private final int faceDisappearanceCount;
    private final int audioDecibelSpikes;
    private final int tabSwitches;
    private final int copyPasteEvents;
    private final int unauthorizedProcessesCount;
    private final String clientIpAddress;

    public ExamSessionTelemetry(String studentId,
                                double gazeAversionSeconds,
                                int faceDisappearanceCount,
                                int audioDecibelSpikes,
                                int tabSwitches,
                                int copyPasteEvents,
                                int unauthorizedProcessesCount,
                                String clientIpAddress) {
        this.studentId = studentId;
        this.gazeAversionSeconds = gazeAversionSeconds;
        this.faceDisappearanceCount = faceDisappearanceCount;
        this.audioDecibelSpikes = audioDecibelSpikes;
        this.tabSwitches = tabSwitches;
        this.copyPasteEvents = copyPasteEvents;
        this.unauthorizedProcessesCount = unauthorizedProcessesCount;
        this.clientIpAddress = clientIpAddress;
    }

    public String getStudentId() {
        return studentId;
    }

    public double getGazeAversionSeconds() {
        return gazeAversionSeconds;
    }

    public int getFaceDisappearanceCount() {
        return faceDisappearanceCount;
    }

    public int getAudioDecibelSpikes() {
        return audioDecibelSpikes;
    }

    public int getTabSwitches() {
        return tabSwitches;
    }

    public int getCopyPasteEvents() {
        return copyPasteEvents;
    }

    public int getUnauthorizedProcessesCount() {
        return unauthorizedProcessesCount;
    }

    public String getClientIpAddress() {
        return clientIpAddress;
    }

    @Override
    public String toString() {
        return "Telemetry{" +
                "student='" + studentId + '\'' +
                ", gazeSec=" + gazeAversionSeconds +
                ", faceLost=" + faceDisappearanceCount +
                ", audioSpikes=" + audioDecibelSpikes +
                ", tabs=" + tabSwitches +
                ", copyPaste=" + copyPasteEvents +
                ", forbiddenApps=" + unauthorizedProcessesCount +
                ", ip='" + clientIpAddress + '\'' +
                '}';
    }
}
