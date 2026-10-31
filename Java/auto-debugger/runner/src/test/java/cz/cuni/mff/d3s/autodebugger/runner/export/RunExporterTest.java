package cz.cuni.mff.d3s.autodebugger.runner.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cuni.mff.d3s.autodebugger.model.common.TargetLanguage;
import cz.cuni.mff.d3s.autodebugger.runner.args.Arguments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RunExporterTest {

    @TempDir Path tempDir;

    private static final Instant CREATED_AT = Instant.parse("2026-10-31T10:15:00Z");

    private static Arguments arguments(String outputDirectory) {
        Arguments a = new Arguments();
        a.language = TargetLanguage.JAVA;
        a.targetMethodReference = "com.example.Calculator.add(int,int)";
        a.outputDirectory = outputDirectory;
        return a;
    }

    @Test
    void withoutConfigurationWritesFailedRunToTheOutputDirectoryFromTheArguments() throws Exception {
        Path out = tempDir.resolve("out");
        var failure = new RuntimeException("Failed to create run configuration",
                new IllegalArgumentException("DiSL home path not specified"));
        Arguments args = arguments(out.toString());
        args.traceMode = "Temporal";

        RunExporter.writeRunJson(args, null, CREATED_AT.plusNanos(123_456_789), failure);

        JsonNode json = new ObjectMapper().readTree(out.resolve("run.json").toFile());
        assertEquals("failed", json.get("status").asText());
        assertEquals("Failed to create run configuration: DiSL home path not specified", json.get("error").asText());
        assertEquals(0, json.get("captured").size());
        assertEquals("temporal", json.get("traceMode").asText());
        assertEquals("java", json.get("language").asText());
        assertEquals("2026-10-31T10:15:00Z", json.get("createdAt").asText());
    }

    @Test
    void withoutConfigurationOrOutputDirectoryDoesNotThrow() {
        assertDoesNotThrow(() -> RunExporter.writeRunJson(arguments(null), null, CREATED_AT, new RuntimeException("x")));
    }

    @Test
    void exportErrorsAreLoggedNotThrown() {
        Arguments broken = arguments(tempDir.resolve("out").toString());
        broken.language = null;   // building the summary fails with a NullPointerException

        assertDoesNotThrow(() -> RunExporter.writeRunJson(broken, null, CREATED_AT, new RuntimeException("x")));
        assertFalse(Files.exists(tempDir.resolve("out").resolve("run.json")));
    }

    @Test
    void describeJoinsCausesAndSkipsRepeatedMessages() {
        var root = new IllegalStateException("Analysis produced no trace file");
        var wrapped = new RuntimeException(root);
        assertEquals("java.lang.IllegalStateException: Analysis produced no trace file", RunExporter.describe(wrapped));
        assertEquals("java.lang.NullPointerException", RunExporter.describe(new NullPointerException()));
    }
}
