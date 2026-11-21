package cz.cuni.mff.d3s.autodebugger.model.common.trace;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One hook event of the target method, as the collector received it: either the entry hook or the exit hook of one call.
 *
 * <p>Values are kept in the order they arrived, keyed by slot id. A value is a primitive wrapper, a {@link String},
 * an {@link ObjectSnapshot}, or a {@link ValueMarker} when the value could not be recorded.
 *
 * <p>Only exit events have an outcome. Entry and exit events are not paired here; the reader of the event list does that.
 */
public class RecordedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean isEntryEvent;
    private final LinkedHashMap<Integer, Object> valuesBySlotId;
    private RecordedOutcome outcome;

    /**
     * Creates an event without values.
     *
     * @param isEntryEvent true for an event from the entry hook, false for one from the exit hook
     */
    public RecordedEvent(boolean isEntryEvent) {
        this.isEntryEvent = isEntryEvent;
        this.valuesBySlotId = new LinkedHashMap<>();
    }

    /** True if the event came from the entry hook, false if it came from the exit hook. */
    public boolean isEntryEvent() {
        return isEntryEvent;
    }

    /**
     * Records the value of one slot. If the slot already has a value, the new one replaces it and keeps its position.
     *
     * @param slotId the slot id of the captured argument or field
     * @param value  the value, or a {@link ValueMarker}
     */
    public void putValue(int slotId, Object value) {
        valuesBySlotId.put(slotId, value);
    }

    /** The values in arrival order, keyed by slot id. The map is read-only. */
    public Map<Integer, Object> getValuesBySlotId() {
        return Collections.unmodifiableMap(valuesBySlotId);
    }

    /** The outcome of the call, or {@code null} for an entry event and for an exit event whose outcome did not arrive. */
    public RecordedOutcome getOutcome() {
        return outcome;
    }

    /**
     * Sets the outcome of the call.
     *
     * @throws IllegalStateException if this is an entry event
     */
    public void setOutcome(RecordedOutcome outcome) {
        if (isEntryEvent) {
            throw new IllegalStateException("An entry event has no outcome");
        }
        this.outcome = outcome;
    }

    @Override
    public String toString() {
        return (isEntryEvent ? "entry " : "exit ") + valuesBySlotId + ((outcome != null) ? " " + outcome : "");
    }
}
