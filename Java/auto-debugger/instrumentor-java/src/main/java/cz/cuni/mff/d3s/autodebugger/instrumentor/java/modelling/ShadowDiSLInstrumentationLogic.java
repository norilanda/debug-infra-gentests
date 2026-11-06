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
    append("}\n");
    return getCode();
  }
}
