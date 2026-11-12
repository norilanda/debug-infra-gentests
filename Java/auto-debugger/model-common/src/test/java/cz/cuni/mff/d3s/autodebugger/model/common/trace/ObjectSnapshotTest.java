package cz.cuni.mff.d3s.autodebugger.model.common.trace;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ObjectSnapshot and JsonObjectParser.
 */
class ObjectSnapshotTest {

    @Test
    void givenSimpleJson_whenParsing_thenCorrectObjectSnapshot() {
        // Given
        String json = "{\"$class\":\"com.example.User\",\"name\":\"John\",\"age\":30}";
        
        // When
        ObjectSnapshot snapshot = ObjectSnapshot.fromJson(json);
        
        // Then
        assertNotNull(snapshot);
        assertEquals("com.example.User", snapshot.getClassName());
        assertEquals("User", snapshot.getSimpleClassName());
        assertEquals("John", snapshot.getField("name"));
        assertEquals(30, snapshot.getField("age"));
    }
    
    @Test
    void givenJsonWithNestedObject_whenParsing_thenNestedObjectSnapshotCreated() {
        // Given
        String json = "{\"$class\":\"com.example.Order\",\"id\":123,\"customer\":{\"$class\":\"com.example.Customer\",\"name\":\"Alice\"}}";
        
        // When
        ObjectSnapshot snapshot = ObjectSnapshot.fromJson(json);
        
        // Then
        assertNotNull(snapshot);
        assertEquals("com.example.Order", snapshot.getClassName());
        assertEquals(123, snapshot.getField("id"));
        
        Object customer = snapshot.getField("customer");
        assertInstanceOf(ObjectSnapshot.class, customer);
        
        ObjectSnapshot customerSnapshot = (ObjectSnapshot) customer;
        assertEquals("com.example.Customer", customerSnapshot.getClassName());
        assertEquals("Alice", customerSnapshot.getField("name"));
    }
    
    @Test
    void givenJsonWithNullField_whenParsing_thenNullValueStored() {
        // Given
        String json = "{\"$class\":\"com.example.Entity\",\"value\":null}";
        
        // When
        ObjectSnapshot snapshot = ObjectSnapshot.fromJson(json);
        
        // Then
        assertNotNull(snapshot);
        assertNull(snapshot.getField("value"));
    }
    
    @Test
    void givenJsonWithBooleanFields_whenParsing_thenBooleanValuesStored() {
        // Given
        String json = "{\"$class\":\"com.example.Flags\",\"active\":true,\"deleted\":false}";
        
        // When
        ObjectSnapshot snapshot = ObjectSnapshot.fromJson(json);
        
        // Then
        assertNotNull(snapshot);
        assertEquals(true, snapshot.getField("active"));
        assertEquals(false, snapshot.getField("deleted"));
    }
    
    @Test
    void givenJsonWithEscapedStrings_whenParsing_thenUnescapedStringsStored() {
        // Given
        String json = "{\"$class\":\"com.example.Message\",\"text\":\"Hello\\nWorld\"}";
        
        // When
        ObjectSnapshot snapshot = ObjectSnapshot.fromJson(json);
        
        // Then
        assertNotNull(snapshot);
        assertEquals("Hello\nWorld", snapshot.getField("text"));
    }
    
    @Test
    void givenJsonWithCycleMarker_whenParsing_thenCycleMarkerStoredAsString() {
        // Given
        String json = "{\"$class\":\"com.example.Node\",\"$id\":0,\"child\":"
            + "{\"$class\":\"com.example.Node\",\"$id\":1,\"parent\":\"$cycle:0\"}}";

        // When
        ObjectSnapshot snapshot = ObjectSnapshot.fromJson(json);

        // Then
        assertNotNull(snapshot);
        assertEquals(0, snapshot.getField("$id"));
        ObjectSnapshot child = assertInstanceOf(ObjectSnapshot.class, snapshot.getField("child"));
        assertEquals(1, child.getField("$id"));
        assertEquals("$cycle:0", child.getField("parent")); // Cycle marker stored as string
    }

    @Test
    void givenJsonWithFieldTypes_whenParsing_thenFieldTypesStoredAsString() {
        // Given
        String json = "{\"$class\":\"com.example.Point\",\"$id\":0,"
            + "\"x\":3,\"y\":4,\"tags\":\"$ref:[I\",\"$fieldTypes\":\"short,long,int[]\"}";

        // When
        ObjectSnapshot snapshot = ObjectSnapshot.fromJson(json);

        // Then
        assertNotNull(snapshot);
        assertEquals("short,long,int[]", snapshot.getField("$fieldTypes"));
        assertEquals(3, snapshot.getField("x"));
    }
    
    @Test
    void givenObjectSnapshot_whenSerialized_thenCanBeDeserialized() throws Exception {
        // Given
        ObjectSnapshot original = new ObjectSnapshot("com.example.Test");
        original.putField("name", "TestValue");
        original.putField("count", 42);
        
        // When - serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();
        
        // When - deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        ObjectSnapshot deserialized = (ObjectSnapshot) ois.readObject();
        ois.close();
        
        // Then
        assertEquals(original.getClassName(), deserialized.getClassName());
        assertEquals(original.getField("name"), deserialized.getField("name"));
        assertEquals(original.getField("count"), deserialized.getField("count"));
    }
    
    @Test
    void givenTraceWithObjectValue_whenAdded_thenCanBeRetrieved() {
        // Given
        Trace trace = new Trace();
        ObjectSnapshot snapshot = new ObjectSnapshot("com.example.Entity");
        snapshot.putField("id", 1);
        
        // When
        trace.addObjectValue(0, snapshot);
        
        // Then
        assertFalse(trace.getObjectValues(0).isEmpty());
        assertTrue(trace.getObjectValues(0).contains(snapshot));
    }
}

