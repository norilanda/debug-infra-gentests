package cz.cuni.mff.d3s.autodebugger.model.common.trace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All hook events of the target method in one run, in the order the collector received them.
 * This is the content of the calls file that the collector writes.
 */
public class RecordedEvents implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ArrayList<RecordedEvent> events = new ArrayList<>();

    /** Appends an event at the end of the list. */
    public void add(RecordedEvent event) {
        events.add(event);
    }

    /** The events in arrival order. The list is read-only. */
    public List<RecordedEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /** The number of events. */
    public int size() {
        return events.size();
    }

    @Override
    public String toString() {
        return "RecordedEvents" + events;
    }
}
