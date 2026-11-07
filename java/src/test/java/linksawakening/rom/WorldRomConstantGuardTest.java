package linksawakening.rom;

import linksawakening.world.transition.ManboTransitionWave;

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
        "OverworldTilesetTable", "IndoorRoomPointerTables",
        "WarpMotion", "EntityRoomLoader", "RoomSession", "RoomWorldState",
        "EnemyDropResolver", "RomEnemyCombatTables",
        "RoomTilemapBuilder", "OverworldBushInteraction", "RoomPaletteLoader", "RoomObjectParser", "RoomLoader", "ChestContentsTable", "DungeonRoomEventTable", "ManboTransitionWave", "ManboWarpResolver");
    // No genuine non-ROM _BANK/_ADDR literals in these files currently need exemption.
    private static final Set<String> NON_ROM_ALLOW_LIST = Set.of();
    private static final Pattern LITERAL = Pattern.compile(
        "\\bint\\s+(\\w*(?:_BANK(?:_[AB])?|_ADDR(?:_[AB])?|_ADDRESS))\\s*=\\s*(?:0[xX][0-9a-fA-F]+|[0-9]+)\\s*;");

    @Test
    void ownedPackagesDoNotIntroduceLiteralRomLocations() throws Exception {
        for (String pkg : List.of("physics", "save", "state", "scene", "startup", "config")) {
            try (var paths = Files.walk(Path.of("src/main/java/linksawakening", pkg))) {
                for (Path path : paths.filter(p -> p.toString().endsWith(".java")
                        && !p.getFileName().toString().equals("PlayerState.java")).toList()) {
                    assertFalse(Files.readString(path).matches("(?s).*new BackgroundSceneSpec\\(\\s*(?:0[xX]|[0-9]).*"),
                        path + ": scene ROM locations must use RomSymbols");
                    var matcher = LITERAL.matcher(Files.readString(path));
                    while (matcher.find()) {
                        assertFalse(!NON_ROM_ALLOW_LIST.contains(
                            path.getFileName() + "." + matcher.group(1)),
                            path + ": use RomSymbols for " + matcher.group(1));
                    }
                }
            }
        }
    }

    @Test
    void migratedFilesDoNotReintroduceLiteralRomLocations() throws Exception {
        for (String name : MIGRATED) {
            Path source;
            try (var files = Files.walk(Path.of("src/main/java/linksawakening/world"))) {
                source = files.filter(path -> path.getFileName().toString().equals(name + ".java"))
                    .findFirst().orElseThrow();
            }
            var matcher = LITERAL.matcher(Files.readString(source));
            while (matcher.find()) {
                assertFalse(!NON_ROM_ALLOW_LIST.contains(name + "." + matcher.group(1)),
                    name + ": use RomSymbols for " + matcher.group(1));
            }
        }
    }
}
