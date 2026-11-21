package cz.cuni.mff.d3s.autodebugger.model.common.trace;

/**
 * Stands in for a value that the collector could not record as a value.
 */
public enum ValueMarker {
    /** The value was {@code null}. */
    NULL,
    /** The value was not serialized, because of its type or because serializing it failed. */
    SKIPPED,
    /** The serialized value was over the size limit and was dropped. */
    TRUNCATED
}
