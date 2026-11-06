# instrumentor-java

This module owns the Java instrumentation code generation for DiSL.

Templates
- Canonical Collector templates are embedded as resources here:
  - src/main/resources/templates/java/disl-analysis/Collector.jt
  - src/main/resources/templates/java/disl-analysis/CollectorRE.java
- At generation time, DiSLInstrumentor copies these resources to the generated source folder and transforms Collector.jt to Collector.java, injecting:
  - PATH: absolute path to the serialized identifier mapping
  - RESULTS: absolute path to the generated test list (generated-tests-<runId>.lst)
  - TARGET_PACKAGE, TARGET_CLASS, TARGET_METHOD, TARGET_RETURN: target metadata used to construct TestGenerationContext inside Collector via TestGenerationContextFactory.createFromStrings(...)
  - TRACE_MODE: either "naive" or "temporal"; controls Trace vs TemporalTrace and generator selection

- Each hook starts a new event via CollectorRE.startEvent(isEntryEvent): true in the @Before hook, false in the @After / @AfterReturning hook. The Collector records the events in arrival order, each marked as entry or exit.

Compilation and manifest
- DiSLCompiler compiles DiSLClass.java, Collector.java, and CollectorRE.java into a JAR
- The JAR manifest is created programmatically (no MANIFEST.MF in the repo). It sets DiSL-Classes=DiSLClass

What moved out of analyzer-disl
- Previously, analyzer-disl/src/main/java/cz/.../disl contained generated sources and templates
- These files have been removed; the instrumentor is now the single source of truth for templates and generated code

Tests
- ResourceEmbeddingTest ensures Collector.jt and CollectorRE.java are present on the classpath
- Integration tests exercise end-to-end generation and packaging

