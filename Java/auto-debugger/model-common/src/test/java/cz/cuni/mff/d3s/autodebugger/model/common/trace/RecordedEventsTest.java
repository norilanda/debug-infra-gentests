package cz.cuni.mff.d3s.autodebugger.model.common.trace;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the hook event classes: {@link RecordedEvents}, {@link RecordedEvent}, {@link RecordedOutcome},
 * {@link ValueMarker}.
 */
class RecordedEventsTest {

    @Test
    void givenEventsWithValuesMarkersAndOutcomes_whenSerializedAndDeserialized_thenEverythingPreservedInOrder() throws Exception {
        // Given: one call that returned and one that threw
        ObjectSnapshot person = new ObjectSnapshot("com.example.Person");
        person.putField("name", "Ada");

        RecordedEvent firstEntry = new RecordedEvent(true);
        firstEntry.putValue(3, 7);
        firstEntry.putValue(1, "text");
        firstEntry.putValue(2, person);
        RecordedEvent firstExit = new RecordedEvent(false);
        firstExit.putValue(3, 7);
        firstExit.putValue(1, "text");
        firstExit.putValue(2, person);
        firstExit.setOutcome(RecordedOutcome.returnedNormally());

        RecordedEvent secondEntry = new RecordedEvent(true);
        secondEntry.putValue(3, 0);
        secondEntry.putValue(1, ValueMarker.NULL);
        secondEntry.putValue(2, ValueMarker.SKIPPED);
        RecordedEvent secondExit = new RecordedEvent(false);
        secondExit.putValue(3, 0);
        secondExit.putValue(1, ValueMarker.NULL);
        secondExit.putValue(2, ValueMarker.TRUNCATED);
        secondExit.setOutcome(RecordedOutcome.threwException("java.lang.ArithmeticException", "/ by zero"));

        RecordedEvents original = new RecordedEvents();
        original.add(firstEntry);
        original.add(firstExit);
        original.add(secondEntry);
        original.add(secondExit);

        // When
        RecordedEvents deserialized = serializeAndDeserialize(original);

        // Then
        List<RecordedEvent> events = deserialized.getEvents();
        assertEquals(4, events.size());
        assertEquals(List.of(true, false, true, false), events.stream().map(RecordedEvent::isEntryEvent).toList());

        assertEquals(List.of(3, 1, 2), List.copyOf(events.get(0).getValuesBySlotId().keySet()));
        assertEquals(7, events.get(0).getValuesBySlotId().get(3));
        assertEquals("text", events.get(0).getValuesBySlotId().get(1));
        assertEquals(person, events.get(0).getValuesBySlotId().get(2));
        assertNull(events.get(0).getOutcome());
        assertEquals(RecordedOutcome.returnedNormally(), events.get(1).getOutcome());

        assertSame(ValueMarker.NULL, events.get(2).getValuesBySlotId().get(1));
        assertSame(ValueMarker.SKIPPED, events.get(2).getValuesBySlotId().get(2));
        assertSame(ValueMarker.TRUNCATED, events.get(3).getValuesBySlotId().get(2));
        RecordedOutcome thrown = events.get(3).getOutcome();
        assertTrue(thrown.threw());
        assertEquals("java.lang.ArithmeticException", thrown.exceptionClass());
        assertEquals("/ by zero", thrown.message());
    }

    @Test
    void givenEntryEvent_whenSetOutcome_thenThrows() {
        RecordedEvent entry = new RecordedEvent(true);

        assertThrows(IllegalStateException.class, () -> entry.setOutcome(RecordedOutcome.returnedNormally()));
    }

    private static RecordedEvents serializeAndDeserialize(RecordedEvents events) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(events);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (RecordedEvents) in.readObject();
        }
    }
}
