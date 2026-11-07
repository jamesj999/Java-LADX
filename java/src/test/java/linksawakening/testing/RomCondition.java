package linksawakening.testing;

import org.junit.jupiter.api.extension.*;

/** Skips explicitly marked integration tests when no user ROM is available. */
public final class RomCondition implements ExecutionCondition {
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return TestRom.class.getResource("/rom/azle.gbc") == null
            ? ConditionEvaluationResult.disabled("Requires user ROM: -Pla.rom=/path/azle.gbc or LA_ROM")
            : ConditionEvaluationResult.enabled("User ROM available");
    }
}
