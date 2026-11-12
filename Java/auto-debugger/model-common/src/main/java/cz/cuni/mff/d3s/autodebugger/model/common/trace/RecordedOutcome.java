package cz.cuni.mff.d3s.autodebugger.model.common.trace;

import java.io.Serializable;

/**
 * How one call of the target method ended: it returned, or it threw an exception.
 *
 * @param threw          true if the call threw an exception
 * @param exceptionClass fully qualified class name of the exception, or {@code null} if the call returned
 * @param message        message of the exception, or {@code null} if the call returned or the exception has no message
 */
public record RecordedOutcome(boolean threw, String exceptionClass, String message) implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The outcome of a call that returned normally. */
    public static RecordedOutcome returnedNormally() {
        return new RecordedOutcome(false, null, null);
    }

    /** The outcome of a call that threw an exception of the given class, with the given message (may be {@code null}). */
    public static RecordedOutcome threwException(String exceptionClass, String message) {
        return new RecordedOutcome(true, exceptionClass, message);
    }
}
