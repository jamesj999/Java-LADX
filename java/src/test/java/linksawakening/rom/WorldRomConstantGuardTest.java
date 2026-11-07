package linksawakening.rom;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Guards every Java source under src/main against copied ROM locations. */
class WorldRomConstantGuardTest {
    // Genuine non-ROM constants only, keyed by filename and declaration name.
    // Empty after the full src/main audit; RomSymbols is generated separately.
    private static final Set<String> NON_ROM_ALLOW_LIST = Set.of();
    private static final Pattern LITERAL = Pattern.compile(
        "\\bint\\s+(\\w*(?:_BANK(?:_[AB])?|_ADDR(?:_[AB])?|_ADDRESS))\\s*=\\s*(?:0[xX][0-9a-fA-F]+|[0-9]+)\\b");

    @Test
    void ownedPackagesDoNotIntroduceLiteralRomLocations() throws Exception {
        try (var paths = Files.walk(Path.of("src/main"))) {
                for (Path path : paths.filter(p -> p.toString().endsWith(".java")
                        && !p.getFileName().toString().equals("RomSymbols.java")).toList()) {
                    var matcher = LITERAL.matcher(Files.readString(path));
                    while (matcher.find()) {
                        assertFalse(!NON_ROM_ALLOW_LIST.contains(
                            path.getFileName() + "." + matcher.group(1)),
                            path + ": use RomSymbols for " + matcher.group(1));
                    }
                }
            }
    }

    @Test
    void sceneSpecificationsUseSymbols() throws Exception {
        try (var paths = Files.walk(Path.of("src/main"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                    assertFalse(Files.readString(path).matches("(?s).*new BackgroundSceneSpec\\(\\s*(?:0[xX]|[0-9]).*"),
                        path + ": scene ROM locations must use RomSymbols");
            }
        }
    }
}
