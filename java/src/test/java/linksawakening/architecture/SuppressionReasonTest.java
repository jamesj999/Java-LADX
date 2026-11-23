package linksawakening.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/** Intentional compiler exceptions stay reviewable beside the smallest suppressed declaration. */
class SuppressionReasonTest {
    @Test
    void everySuppressionHasAnAdjacentReasonComment() throws Exception {
        var undocumented = new ArrayList<String>();
        try (var sources = Files.walk(Path.of("src"))) {
            for (var source :
                    sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                var lines = Files.readAllLines(source);
                for (int i = 0; i < lines.size(); i++) {
                    if (!lines.get(i).stripLeading().startsWith("@SuppressWarnings(")) continue;
                    int previous = i - 1;
                    while (previous >= 0 && lines.get(previous).isBlank()) previous--;
                    String reason = previous < 0 ? "" : lines.get(previous).strip();
                    if (!reason.startsWith("// ") && !reason.endsWith("*/")) {
                        undocumented.add(source + ":" + (i + 1));
                    }
                }
            }
        }
        assertTrue(undocumented.isEmpty(), "Add a reason beside each suppression: " + undocumented);
    }
}
