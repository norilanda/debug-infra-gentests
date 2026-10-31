package cz.cuni.mff.d3s.autodebugger.runner.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Writes run.json, the summary of one run, into the run's output directory.
 * The file is written to a temporary file first and then moved into place, so readers never see a partial file.
 */
public final class RunJsonWriter {

    public static final String FILE_NAME = "run.json";
    public static final int SCHEMA_VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** The values written to run.json. {@code error} is set only when {@code ok} is false. */
    public record RunSummary(
            String language,
            String targetSignature,
            List<CapturedIdentifiers.Captured> captured,
            String traceMode,
            boolean ok,
            String error,
            Instant createdAt) {}

    private RunJsonWriter() {}

    public static Path write(Path outputDirectory, RunSummary summary) throws IOException {
        Files.createDirectories(outputDirectory);
        Path target = outputDirectory.resolve(FILE_NAME);
        Path temp = outputDirectory.resolve(FILE_NAME + ".tmp");
        MAPPER.writeValue(temp.toFile(), toJson(summary));
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    static ObjectNode toJson(RunSummary summary) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("language", summary.language());
        root.putObject("target").put("signature", summary.targetSignature());

        ArrayNode captured = root.putArray("captured");
        for (CapturedIdentifiers.Captured c : summary.captured()) {
            captured.addObject()
                    .put("captureId", c.captureId())
                    .put("kind", c.kind().name().toLowerCase(Locale.ROOT))
                    .put("name", c.name())
                    .put("type", c.type());
        }

        if (summary.traceMode() != null) {
            root.put("traceMode", summary.traceMode());
        }
        root.put("status", summary.ok() ? "ok" : "failed");
        if (!summary.ok()) {
            root.put("error", summary.error());
        }
        root.put("createdAt", summary.createdAt().toString());
        return root;
    }
}
