package linksawakening.rom;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Extend the migrated-file list as each owner finishes a ROM access migration. */
class WorldRomConstantGuardTest {
    private static final List<String> MIGRATED = List.of(
        "OverworldTilesetTable", "IndoorRoomPointerTables");
    // No genuine non-ROM _BANK/_ADDR literals in these files currently need exemption.
    private static final Set<String> NON_ROM_ALLOW_LIST = Set.of();
    private static final Pattern LITERAL = Pattern.compile(
        "\\bint\\s+(\\w*(?:_BANK|_ADDR))\\s*=\\s*(?:0[xX][0-9a-fA-F]+|[0-9]+)\\s*;");

    @Test
    void migratedFilesDoNotReintroduceLiteralRomLocations() throws Exception {
        for (String name : MIGRATED) {
            var matcher = LITERAL.matcher(Files.readString(Path.of(
                "src/main/java/linksawakening/world", name + ".java")));
            while (matcher.find()) {
                assertFalse(!NON_ROM_ALLOW_LIST.contains(name + "." + matcher.group(1)),
                    name + ": use RomSymbols for " + matcher.group(1));
            }
        }
    }
}
