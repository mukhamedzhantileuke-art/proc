package proctoring.chain;

public enum ChainStepStatus {
    NOT_STARTED,
    STAGE_A_COMMITTED,
    STAGE_B_COMMITTED,
    CHAIN_COMPLETED,
    COMPENSATING_A,
    COMPENSATED_A,
    COMPENSATING_B_A,
    COMPENSATED_B_A
}
