package cz.cuni.mff.d3s.autodebugger.runner.export;

import cz.cuni.mff.d3s.autodebugger.model.common.RunConfiguration;
import cz.cuni.mff.d3s.autodebugger.runner.args.Arguments;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the contract files of a run into its output directory. This is the one entry point the runner calls.
 * Export failures are logged and never replace the run's own error.
 */
@Slf4j
public final class RunExporter {

    private RunExporter() {}

    /**
     * Writes run.json. {@code config} is null when the run configuration could not be created; then the captured
     * values are unknown and the output directory is taken from the arguments, and without an explicit output
     * directory nothing is written. {@code failure} is null when the run succeeded. {@code createdAt} is written
     * in whole seconds.
     */
    public static void writeRunJson(Arguments arguments, RunConfiguration config, Instant createdAt, Throwable failure) {
        try {
            Path outputDirectory = config != null ? config.getOutputDirectory()
                    : arguments.outputDirectory != null ? Path.of(arguments.outputDirectory) : null;
            if (outputDirectory == null) {
                log.warn("No output directory known, run.json not written");
                return;
            }

            var summary = new RunJsonWriter.RunSummary(
                    arguments.language.getIdentifier(),
                    arguments.targetMethodReference,
                    config != null ? CapturedIdentifiers.from(config.getExportableValues()) : List.of(),
                    arguments.traceMode != null ? arguments.traceMode.toLowerCase(Locale.ROOT) : null,
                    failure == null,
                    failure == null ? null : describe(failure),
                    createdAt.truncatedTo(ChronoUnit.SECONDS));
            Path written = RunJsonWriter.write(outputDirectory, summary);
            log.info("Run summary written to: {}", written);
        } catch (Exception e) {
            log.error("Failed to write run.json", e);
        }
    }

    /** The exception's message, followed by its causes' messages, e.g. "Failed to create run configuration: DiSL home ...". */
    static String describe(Throwable failure) {
        var parts = new ArrayList<String>();
        for (Throwable t = failure; t != null && parts.size() < 5; t = t.getCause()) {
            String message = t.getMessage() != null && !t.getMessage().isBlank() ? t.getMessage() : t.getClass().getName();
            if (parts.isEmpty() || !parts.get(parts.size() - 1).contains(message)) {
                parts.add(message);
            }
        }
        return String.join(": ", parts);
    }
}
