# analyzer-disl

This module contains the DiSL analysis-side code that runs inside the ShadowVM (evaluation JVM).

## Collector template (Collector.jt)

Collector.jt is the template used to generate the analysis-side Collector.java class.
Placeholders are substituted at instrumentation time by the instrumentor.

Placeholders:
- ${PATH}: absolute path to the identifier mapping (serialized) used by trace-based generators
- ${RESULTS}: absolute path to the results list file (one generated test path per line)
- ${TARGET_PACKAGE}, ${TARGET_CLASS}, ${TARGET_METHOD}, ${TARGET_RETURN}: injected target metadata used by Collector to construct a TestGenerationContext via TestGenerationContextFactory.createFromStrings(...)
- ${STRATEGY}: compile-time injected generation strategy id (e.g. "trace-based-naive", "ai-assisted")
- ${TRACE_MODE}: "naive" or "temporal" (controls Trace vs TemporalTrace and generator selection)


## New CLI argument

The runner now accepts a trace mode selector:
- -r, --trace-mode naive|temporal (default: naive)

When set to temporal, the instrumentor will:
- bake ${TRACE_MODE}=temporal into Collector
- inject event boundaries via CollectorRE.startEvent(isEntryEvent) at the beginning of instrumented method probes (true for the entry probe, false for the exit probe)
- enable a TemporalTrace alongside the classic Trace; at exit, the Collector will use TemporalTraceBasedGenerator with a proper TestGenerationContext constructed from ${TARGET_*} placeholders to generate tests

When set to naive (default), the Collector will use the classic Trace and NaiveTraceBasedGenerator.

Notes:
- ${STRATEGY} is baked into the generated Collector; the ShadowVM does not read it from system properties.
- Sensitive values such as API keys are not embedded; they are read from environment variables at runtime.

## Environment variables

The Collector process (ShadowVM) recognizes the following environment variables:

- ANTHROPIC_API_KEY: preferred key for the LLM-based generator
- AUTODEBUGGER_STUB: if set to "1" or "true", the Collector writes a minimal stub test and exits, used for testing

These variables are read via System.getenv(...) inside the Collector.

## Outputs

At analysis exit, the Collector writes the absolute paths of generated test files into the results list file (${RESULTS}).
The parent process reads the path to this file from its own system property (autodebugger.results) and loads the list.

