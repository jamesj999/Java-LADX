package linksawakening.testing;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.*;
/** Central ROM test classification; keep rom-tests.txt in sync when adding integration tests. */
public final class RomCondition implements ExecutionCondition {
    private static final Set<String> ROM_TESTS;
    static {
        try (var in = RomCondition.class.getResourceAsStream("/rom-tests.txt")) {
            ROM_TESTS = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                .lines().collect(Collectors.toSet());
        } catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        boolean dependent = context.getTestClass().map(c -> ROM_TESTS.contains(c.getName().split("\\$")[0])).orElse(false);
        if (dependent && (Boolean.getBoolean("la.romFree") ||
                RomCondition.class.getResource("/rom/azle.gbc") == null))
            return ConditionEvaluationResult.disabled("Requires user ROM: -Pla.rom=/path/azle.gbc or LA_ROM");
        return ConditionEvaluationResult.enabled("ROM-free or user ROM available");
    }
}
