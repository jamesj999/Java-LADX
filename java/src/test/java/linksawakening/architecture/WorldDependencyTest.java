package linksawakening.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Checks compiled references, including descriptors and generic signatures. */
class WorldDependencyTest {
    private static final Path ROOT = Path.of("build/classes/java/main/linksawakening");
    private static final Pattern TYPE = Pattern.compile("linksawakening/[A-Za-z0-9_$/]+");

    private record FieldReference(String owner, String name) {}

    private record CompiledType(String name, List<String> references, List<FieldReference> fields) {}

    private static List<CompiledType> classes() throws Exception {
        List<CompiledType> result = new ArrayList<>();
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                List<String> references = new ArrayList<>();
                List<FieldReference> fields = new ArrayList<>();
                try (var input = new DataInputStream(Files.newInputStream(path))) {
                    if (input.readInt() != 0xCAFEBABE) throw new IllegalArgumentException(path.toString());
                    input.readUnsignedShort();
                    input.readUnsignedShort();
                    int count = input.readUnsignedShort();
                    String[] utf8 = new String[count];
                    int[] classNames = new int[count];
                    int[][] nameAndTypes = new int[count][];
                    List<int[]> fieldRefs = new ArrayList<>();
                    for (int i = 1; i < count; i++) {
                        switch (input.readUnsignedByte()) {
                            case 1 -> {
                                utf8[i] = input.readUTF();
                                var matcher = TYPE.matcher(utf8[i]);
                                while (matcher.find()) references.add(matcher.group());
                            }
                            case 3, 4 -> input.skipNBytes(4);
                            case 5, 6 -> {
                                input.skipNBytes(8);
                                i++;
                            }
                            case 7 -> classNames[i] = input.readUnsignedShort();
                            case 9 -> fieldRefs.add(new int[] {input.readUnsignedShort(), input.readUnsignedShort()});
                            case 12 -> nameAndTypes[i] =
                                    new int[] {input.readUnsignedShort(), input.readUnsignedShort()};
                            case 8, 16, 19, 20 -> input.skipNBytes(2);
                            case 10, 11, 17, 18 -> input.skipNBytes(4);
                            case 15 -> input.skipNBytes(3);
                            default -> throw new IllegalArgumentException("Unknown constant-pool tag: " + path);
                        }
                    }
                    for (int[] field : fieldRefs) {
                        fields.add(new FieldReference(utf8[classNames[field[0]]], utf8[nameAndTypes[field[1]][0]]));
                    }
                }
                result.add(new CompiledType(
                        "linksawakening/"
                                + ROOT.relativize(path)
                                        .toString()
                                        .replace('\\', '/')
                                        .replace(".class", ""),
                        references,
                        fields));
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
    void handlersAccessRuntimeStateOnlyThroughServices() throws Exception {
        Set<String> runtimeOwners = Set.of(
                "linksawakening/world/entity/EntityRuntimeState", "linksawakening/world/entity/RoomEntityRuntime");
        for (var type : classes()) {
            if (!type.name().startsWith("linksawakening/world/entity/handler/")) continue;
            // CONSTANT_Fieldref covers reads, writes and field method handles,
            // including inherited fields qualified through RoomEntityRuntime.
            for (var field : type.fields()) {
                assertTrue(
                        !runtimeOwners.contains(field.owner()),
                        type.name() + " -> " + field.owner() + "." + field.name() + "; use the frame's typed services");
            }
        }
    }

    @Test
    void eventDependenciesAreValuesOrEventContracts() throws Exception {
        for (var type : classes()) {
            if (!type.name().startsWith("linksawakening/world/entity/event/")) continue;
            for (String reference : type.references()) {
                if (reference.startsWith("linksawakening/world/entity/event/")) continue;
                Class<?> dependency = Class.forName(
                        reference.replace('/', '.'), false, getClass().getClassLoader());
                assertTrue(dependency.isRecord() || dependency.isEnum(), type.name() + " -> " + reference);
            }
        }
    }

    // Explicit facade/value surface used by app, including construction-time APIs.
    private static final Set<String> APP_WORLD_API = Set.of(
            "room/ActiveRoom",
            "room/ChestContentsTable",
            "room/DroppableRupeeSystem",
            "entity/EnemyAttackContext",
            "entity/EntityPickupEvent",
            "entity/EntityStatus",
            "object/OverworldBushInteraction",
            "room/OverworldTilesetTable",
            "entity/RomRandomByteSource",
            "room/RoomConstants",
            "entity/RoomEntity",
            "entity/RoomEntitySnapshot",
            "room/RoomLoadListener",
            "room/RoomLoader",
            "room/RoomRenderSnapshot",
            "room/RoomSession",
            "room/ButtonInputs",
            "room/EntityFrameInputs",
            "room/EntityFrameResults",
            "room/InventoryInputs",
            "room/LinkInputs",
            "room/LinkMotion",
            "room/LinkOutputs",
            "room/LinkPosition",
            "room/LinkWrite",
            "room/PlayerHealth",
            "room/PlayerInputs",
            "room/PlayerLevels",
            "room/PresentationOutputs",
            "room/ProgressInputs",
            "room/RewardOutputs",
            "room/RoomDoors",
            "room/RoomPlayerItems",
            "room/RoomStory",
            "room/RoomTransitions",
            "room/SwordBox",
            "room/UiInputs",
            "room/RoomTransitionCoordinator",
            "transition/RoomBoundaryController",
            "transition/ScrollController",
            "transition/TransitionController",
            "transition/Warp");

    @Test
    void motionAndHandlersAreWorldInternal() throws Exception {
        for (var type : classes()) {
            if (type.name().startsWith("linksawakening/world/")) continue;
            for (String reference : type.references()) {
                assertTrue(
                        !reference.startsWith("linksawakening/world/entity/motion/")
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
                String facade = reference.substring("linksawakening/world/".length());
                int nested = facade.indexOf('$');
                if (nested >= 0) facade = facade.substring(0, nested);
                assertTrue(APP_WORLD_API.contains(facade), type.name() + " -> " + reference);
            }
        }
    }

    @Test
    void motionAndHandlersAreReferencedOnlyByEntity() throws Exception {
        for (var type : classes()) {
            if (type.name().startsWith("linksawakening/world/entity/")) continue;
            for (String reference : type.references()) {
                assertTrue(
                        !reference.startsWith("linksawakening/world/entity/motion/")
                                && !reference.startsWith("linksawakening/world/entity/handler/"),
                        type.name() + " -> " + reference);
            }
        }
    }

    @Test
    void scanIncludesAppFacadesAndInternalImplementationClasses() throws Exception {
        var compiled = classes();
        Set<String> names = compiled.stream().map(CompiledType::name).collect(java.util.stream.Collectors.toSet());
        assertTrue(names.contains("linksawakening/app/gameplay/EntityPhase"));
        assertTrue(names.contains("linksawakening/world/room/RoomSession"));
        assertTrue(names.contains("linksawakening/world/entity/handler/HinoxHandler"));
        assertTrue(compiled.stream()
                .filter(t -> t.name().startsWith("linksawakening/app/"))
                .flatMap(t -> t.references().stream())
                .anyMatch(r -> r.startsWith("linksawakening/world/")));
    }
}
