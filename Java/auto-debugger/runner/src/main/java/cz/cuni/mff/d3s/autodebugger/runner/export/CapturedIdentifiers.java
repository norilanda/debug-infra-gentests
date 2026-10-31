package cz.cuni.mff.d3s.autodebugger.runner.export;

import cz.cuni.mff.d3s.autodebugger.model.common.identifiers.ExportableValue;
import cz.cuni.mff.d3s.autodebugger.model.java.identifiers.JavaArgumentIdentifier;
import cz.cuni.mff.d3s.autodebugger.model.java.identifiers.JavaClassIdentifier;
import cz.cuni.mff.d3s.autodebugger.model.java.identifiers.JavaFieldIdentifier;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the {@code captured} list of run.json from the run configuration's exportable values.
 * Keeps their order (arguments, then fields in {@code --fields} order).
 */
@Slf4j
public final class CapturedIdentifiers {

    public enum Kind { ARGUMENT, FIELD }

    /** One entry of {@code captured}. {@code captureId} is the slot id the instrumentation records values under. */
    public record Captured(int captureId, Kind kind, String name, String type) {}

    private CapturedIdentifiers() {}

    public static List<Captured> from(List<? extends ExportableValue> values) {
        // Field names are made distinct step by step: name, then Owner.name, then package.Owner.name
        Map<String, Integer> nameCounts = new HashMap<>();
        Map<String, Integer> ownerNameCounts = new HashMap<>();
        for (ExportableValue value : values) {
            if (value instanceof JavaFieldIdentifier field) {
                nameCounts.merge(field.getFieldName(), 1, Integer::sum);
                ownerNameCounts.merge(ownerQualifiedName(field, false), 1, Integer::sum);
            }
        }

        List<Captured> captured = new ArrayList<>();
        for (ExportableValue value : values) {
            if (value instanceof JavaArgumentIdentifier argument) {
                // Real argument names are unknown, so the contract's arg<position> is used
                captured.add(new Captured(argument.getInternalId(), Kind.ARGUMENT,
                        "arg" + argument.getArgumentSlot(), argument.getType()));
            } else if (value instanceof JavaFieldIdentifier field) {
                String name = field.getFieldName();
                if (nameCounts.get(name) > 1) {
                    name = ownerQualifiedName(field, false);
                    if (ownerNameCounts.get(name) > 1) {
                        name = ownerQualifiedName(field, true);
                    }
                }
                captured.add(new Captured(field.getInternalId(), Kind.FIELD, name, field.getType()));
            } else {
                log.warn("Skipping captured value of unsupported kind: {}", value.getClass().getName());
            }
        }
        return captured;
    }

    /** {@code Owner.name}, or {@code package.Owner.name} when {@code withPackage} is set and the owner has a package. */
    private static String ownerQualifiedName(JavaFieldIdentifier field, boolean withPackage) {
        JavaClassIdentifier owner = field.getOwnerClassIdentifier();
        if (owner == null) {
            return field.getFieldName();
        }
        String ownerName = owner.getClassName();
        String packageName = owner.getPackageIdentifier() != null ? owner.getPackageIdentifier().getPackageName() : "";
        if (withPackage && packageName != null && !packageName.isEmpty()) {
            ownerName = packageName + "." + ownerName;
        }
        return ownerName + "." + field.getFieldName();
    }
}
