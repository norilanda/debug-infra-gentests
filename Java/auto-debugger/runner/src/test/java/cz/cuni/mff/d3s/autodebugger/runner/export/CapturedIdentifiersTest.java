package cz.cuni.mff.d3s.autodebugger.runner.export;

import cz.cuni.mff.d3s.autodebugger.model.common.identifiers.ExportableValue;
import cz.cuni.mff.d3s.autodebugger.model.java.identifiers.*;
import cz.cuni.mff.d3s.autodebugger.runner.export.CapturedIdentifiers.Captured;
import cz.cuni.mff.d3s.autodebugger.runner.export.CapturedIdentifiers.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CapturedIdentifiersTest {

    private static JavaArgumentIdentifier argument(int slot, String type) {
        return new JavaArgumentIdentifier(ArgumentIdentifierParameters.builder()
                .argumentSlot(slot)
                .variableType(type)
                .build());
    }

    // The owner's package only appears in a name when two owners share a simple class name; tests that don't cover
    // that case use this arbitrary package.
    private static final String UNUSED_PACKAGE = "any.pkg";

    private static JavaFieldIdentifier field(String owner, String name, String type) {
        return field(UNUSED_PACKAGE, owner, name, type);
    }

    private static JavaFieldIdentifier field(String ownerPackage, String owner, String name, String type) {
        var ownerClass = new JavaClassIdentifier(ClassIdentifierParameters.builder()
                .packageIdentifier(new JavaPackageIdentifier(ownerPackage))
                .className(owner)
                .build());
        return new JavaFieldIdentifier(FieldIdentifierParameters.builder()
                .variableType(type)
                .variableName(name)
                .ownerClassIdentifier(ownerClass)
                .build());
    }

    @Test
    void keepsOrderAndUsesInternalIdsAndArgumentPositions() {
        var arg0 = argument(0, "java.lang.String");
        var arg1 = argument(1, "int");
        var calls = field("Divider", "calls", "int");

        List<Captured> captured = CapturedIdentifiers.from(List.of(arg0, arg1, calls));

        assertEquals(List.of(
                new Captured(arg0.getInternalId(), Kind.ARGUMENT, "arg0", "java.lang.String"),
                new Captured(arg1.getInternalId(), Kind.ARGUMENT, "arg1", "int"),
                new Captured(calls.getInternalId(), Kind.FIELD, "calls", "int")
        ), captured);
    }

    @Test
    void qualifiesFieldsWithTheSameNameByOwner() {
        var first = field("Counter", "count", "int");
        var second = field("Other", "count", "long");
        var unique = field("Counter", "total", "int");

        List<String> names = CapturedIdentifiers.from(List.of(first, second, unique)).stream()
                .map(Captured::name)
                .toList();

        assertEquals(List.of("Counter.count", "Other.count", "total"), names);
    }

    @Test
    void addsThePackageWhenOwnersShareASimpleName() {
        var first = field("org.lang3", "Counter", "count", "int");
        var second = field("org.lang3.text", "Counter", "count", "int");
        var defaultPackage = field("", "Other", "count", "int");

        List<String> names = CapturedIdentifiers.from(List.of(first, second, defaultPackage)).stream()
                .map(Captured::name)
                .toList();

        assertEquals(List.of("org.lang3.Counter.count", "org.lang3.text.Counter.count", "Other.count"), names);
    }

    @Test
    void skipsUnsupportedKinds() {
        var arg0 = argument(0, "int");
        ExportableValue other = () -> 99;

        List<Captured> captured = CapturedIdentifiers.from(List.of(arg0, other));

        assertEquals(1, captured.size());
        assertEquals("arg0", captured.get(0).name());
    }
}
