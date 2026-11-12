package cz.cuni.mff.d3s.autodebugger.instrumentor.java.modelling;

import cz.cuni.mff.d3s.autodebugger.instrumentor.common.modelling.Metaclass;
import cz.cuni.mff.d3s.autodebugger.instrumentor.java.modelling.enums.ActivationTime;
import cz.cuni.mff.d3s.autodebugger.model.common.identifiers.MethodIdentifier;
import java.util.List;
import lombok.Getter;

@Getter
public class ShadowDiSLInstrumentationLogic extends DiSLInstrumentationLogic {

  public ShadowDiSLInstrumentationLogic(
          MethodIdentifier identifier, DiSLAnnotation annotation, List<JavaValue> exports) {
    super(identifier, annotation, exports);
  }

  @Override
  public String emitCode() {
    append(annotation.emitCode());
    append("\npublic static void ");
    append(identifier.getName());
    append("(DynamicContext di) {\n");
    for (Metaclass variable : exports) {
      append(variable.emitCode());
      append("\n");
    }
    append(
        "System.out.println(\"[Instrumentation process] PID: \" + ProcessHandle.current().pid());\n");
    // Start a new hook event: true in the entry hook (@Before), false in the exit hooks
    boolean isEntryHook = annotation.getActivationTime() == ActivationTime.BEFORE;
    append("CollectorRE.startEvent(" + isEntryHook + ");\n");
    for (JavaValue variable : exports) {
      append(variable.emitCollectorCode());
      append("\n");
    }
    // Exit hooks also record how the call ended, after its values
    if (!isEntryHook) {
      append(emitOutcomeCollectorCode());
    }
    append("}\n");
    return getCode();
  }

  /**
   * The outcome call for an exit hook. @After runs on normal return and on exception, and getException() tells them
   * apart. @AfterReturning runs only on normal return, so the call returned.
   */
  private String emitOutcomeCollectorCode() {
    return (annotation.getActivationTime() == ActivationTime.AFTER_RETURNING)
        ? "CollectorRE.collectOutcome(null);\n"
        : "CollectorRE.collectOutcome(di.getException());\n";
  }
}
