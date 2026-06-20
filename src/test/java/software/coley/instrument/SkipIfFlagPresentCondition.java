package software.coley.instrument;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * For some reason IntelliJ's debug agent conflicts with the InstrumentationServer and causes EXCEPTION_ACCESS_VIOLATION.
 * <pre>{@code
 * Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)
 * V  [jvm.dll+0x11ee3a]
 * V  [jvm.dll+0xde4c8]
 * V  [jvm.dll+0xdaa30]
 * V  [jvm.dll+0x17704f]
 * V  [jvm.dll+0x180ea3]
 * V  [jvm.dll+0x31091f]
 * C  [instrument.dll+0x3efd]
 * C  0x000001e24c39964e
 *
 * Java frames: (J=compiled Java code, j=interpreted, Vv=VM code)
 * j  sun.instrument.InstrumentationImpl.retransformClasses0(J[Ljava/lang/Class;)V+0
 * j  sun.instrument.InstrumentationImpl.retransformClasses([Ljava/lang/Class;)V+23
 * j  software.coley.instrument.InstrumentationHelper.transformBatch([Ljava/lang/Class;Ljava/lang/ClassLoader;)V+5
 * j  software.coley.instrument.InstrumentationHelper.populateExisting()V+114
 * }</pre>
 */
public class SkipIfFlagPresentCondition implements ExecutionCondition {
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		String flagProperty = System.getProperty("intellij.debug.agent");
		if (flagProperty != null)
			return ConditionEvaluationResult.disabled("IntelliJ agent conflicts with InstrumentationServer and causes EXCEPTION_ACCESS_VIOLATION.");
		return ConditionEvaluationResult.enabled("No agent incompatibilities found.");
	}
}