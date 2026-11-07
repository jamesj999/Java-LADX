package linksawakening.architecture;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks compiled references, including descriptors and generic signatures. */
class WorldDependencyTest {
    private static final Path ROOT = Path.of("build/classes/java/main/linksawakening");
    private static final Pattern TYPE = Pattern.compile("linksawakening/[A-Za-z0-9_$/]+");

    private record CompiledType(String name, List<String> references) {}

    private static List<CompiledType> classes() throws Exception {
        List<CompiledType> result = new ArrayList<>();
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                List<String> references = new ArrayList<>();
                try (var input = new DataInputStream(Files.newInputStream(path))) {
                    if (input.readInt() != 0xCAFEBABE) throw new IllegalArgumentException(path.toString());
                    input.readUnsignedShort();
                    input.readUnsignedShort();
                    int count = input.readUnsignedShort();
                    for (int i = 1; i < count; i++) {
                        switch (input.readUnsignedByte()) {
                            case 1 -> {
                                var matcher = TYPE.matcher(input.readUTF());
                                while (matcher.find()) references.add(matcher.group());
                            }
                            case 3, 4 -> input.skipNBytes(4);
                            case 5, 6 -> { input.skipNBytes(8); i++; }
                            case 7, 8, 16, 19, 20 -> input.skipNBytes(2);
                            case 9, 10, 11, 12, 17, 18 -> input.skipNBytes(4);
                            case 15 -> input.skipNBytes(3);
                            default -> throw new IllegalArgumentException("Unknown constant-pool tag: " + path);
                        }
                    }
                }
                result.add(new CompiledType("linksawakening/" + ROOT.relativize(path)
                    .toString().replace('\\', '/').replace(".class", ""), references));
            }
        }
        assertTrue(!result.isEmpty(), "No compiled world classes found");
        return result;
    }

    @Test
    void worldDoesNotDependOnApp() throws Exception {
        for (var type : classes()) {
            if (!type.name().startsWith("linksawakening/world/")) continue;
            for (String reference : type.references()) {
            assertTrue(!reference.startsWith("linksawakening/app/"), type.name() + " -> " + reference);
            }
        }
    }

    @Test
    void handlersDoNotDependOnRoom() throws Exception {
        for (var type : classes()) {
            if (!type.name().contains("/entity/handler/") && !type.name().endsWith("Handler")) continue;
            for (String reference : type.references()) {
                assertTrue(!reference.startsWith("linksawakening/world/room/"), type.name() + " -> " + reference);
            }
        }
    }

    @Test
    void eventDependenciesAreValuesOrEventContracts() throws Exception {
        for (var type : classes()) {
            if (!type.name().startsWith("linksawakening/world/entity/event/")) continue;
            for (String reference : type.references()) {
                if (reference.startsWith("linksawakening/world/entity/event/")) continue;
                Class<?> dependency = Class.forName(reference.replace('/', '.'), false, getClass().getClassLoader());
                boolean contract = reference.equals("linksawakening/world/EntityEvent")
                    || reference.equals("linksawakening/world/EntityRequestBridge");
                assertTrue(contract || dependency.isRecord() || dependency.isEnum(), type.name() + " -> " + reference);
            }
        }
    }

    // Explicit facade/value surface used by app, including construction-time APIs.
    private static final Set<String> APP_WORLD_API = Set.of(
        "ActiveRoom", "ChestContentsTable", "DroppableRupeeSystem", "EnemyAttackContext",
        "EntityCombatEvent", "EntityPickupEvent", "EntityProjectileEvent", "EntityStatus",
        "OverworldBushInteraction", "OverworldTilesetTable", "RomRandomByteSource",
        "RoomConstants", "RoomEntity", "RoomEntitySnapshot", "RoomLoadListener", "RoomLoader",
        "RoomRenderSnapshot", "RoomSession", "RoomTransitionCoordinator",
        "transition/RoomBoundaryController", "transition/ScrollController",
        "transition/TransitionController", "transition/Warp");

    @Test
    void motionAndHandlersAreWorldInternal() throws Exception {
        for (var type : classes()) {
            if (type.name().startsWith("linksawakening/world/")) continue;
            for (String reference : type.references()) {
                assertTrue(!reference.startsWith("linksawakening/world/entity/motion/")
                    && !reference.startsWith("linksawakening/world/entity/handler/"),
                    type.name() + " -> " + reference);
            }
        }
    }

    @Test
    void appUsesOnlyExplicitWorldFacades() throws Exception {
        for (var type : classes()) {
            if (!type.name().startsWith("linksawakening/app/")) continue;
            for (String reference : type.references()) {
                if (!reference.startsWith("linksawakening/world/")
                    || reference.startsWith("linksawakening/world/entity/event/")) continue;
                String facade = reference.substring("linksawakening/world/".length()).split("\\$")[0];
                assertTrue(APP_WORLD_API.contains(facade), type.name() + " -> " + reference);
            }
        }
    }
}
