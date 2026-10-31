package cz.cuni.mff.d3s.autodebugger.runner.export;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.cuni.mff.d3s.autodebugger.runner.export.CapturedIdentifiers.Captured;
import cz.cuni.mff.d3s.autodebugger.runner.export.CapturedIdentifiers.Kind;
import cz.cuni.mff.d3s.autodebugger.runner.export.RunJsonWriter.RunSummary;

class RunJsonWriterTest {

    @TempDir Path tempDir;

    private static final Instant CREATED_AT = Instant.parse("2026-10-31T10:15:00Z");

    private JsonNode writeAndRead(RunSummary summary) throws Exception {
        Path written = RunJsonWriter.write(tempDir.resolve("out"), summary);
        assertEquals(tempDir.resolve("out").resolve("run.json"), written);
        assertFalse(Files.exists(tempDir.resolve("out").resolve("run.json.tmp")));
        return new ObjectMapper().readTree(written.toFile());
    }

    @Test
    void writesOkRun() throws Exception {
        var summary = new RunSummary("java", "com.example.LineCounter.lineCount(java.lang.String,int)",
                List.of(new Captured(1, Kind.ARGUMENT, "arg0", "java.lang.String"),
                        new Captured(2, Kind.FIELD, "calls", "int")),
                "naive", true, null, CREATED_AT);

        JsonNode json = writeAndRead(summary);

        assertEquals(1, json.get("schemaVersion").asInt());
        assertEquals("java", json.get("language").asText());
        assertEquals("com.example.LineCounter.lineCount(java.lang.String,int)", json.get("target").get("signature").asText());
        assertEquals(2, json.get("captured").size());
        JsonNode field = json.get("captured").get(1);
        assertEquals(2, field.get("captureId").asInt());
        assertEquals("field", field.get("kind").asText());
        assertEquals("calls", field.get("name").asText());
        assertEquals("int", field.get("type").asText());
        assertEquals("naive", json.get("traceMode").asText());
        assertEquals("ok", json.get("status").asText());
        assertFalse(json.has("error"));
        assertEquals("2026-10-31T10:15:00Z", json.get("createdAt").asText());
        // TODO: update once trace.json is generated
        assertFalse(json.has("counts"));
        assertFalse(json.has("recorded"));
    }

    @Test
    void writesFailedRunWithErrorAndReplacesOldFile() throws Exception {
        writeAndRead(new RunSummary("java", "a.B.c()", List.of(), "naive", true, null, CREATED_AT));

        JsonNode json = writeAndRead(new RunSummary("java", "a.B.c()", List.of(), "temporal", false,
                "Test generation produced no test files", CREATED_AT));

        assertEquals("failed", json.get("status").asText());
        assertEquals("Test generation produced no test files", json.get("error").asText());
        assertEquals("temporal", json.get("traceMode").asText());
        assertEquals(0, json.get("captured").size());
    }
}
