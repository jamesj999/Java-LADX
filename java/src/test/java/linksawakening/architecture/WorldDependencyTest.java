package linksawakening.architecture;

import static linksawakening.architecture.ClassFileReferences.classes;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import linksawakening.architecture.ClassFileReferences.CompiledType;
import org.junit.jupiter.api.Test;

/** Checks compiled references, including descriptors and generic signatures. */
class WorldDependencyTest {
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
            "room/LinkGroundFrame",
            "room/ShovelStartResult",
            "room/ArrowShotResult",
            "room/ChestOpenResult",
            "room/RoomEntities",
            "room/RoomEntityState",
            "room/RoomRooms",
            "room/RoomDungeon",
            "room/RoomProgress",
            "room/RoomTerrain",
            "room/ButtonInputs",
            "room/EntityFrameInputs",
            "room/EntityTickInputs",
            "room/EntityCombatInputs",
            "room/EntityPickupInputs",
            "room/RoomSessionConfig",
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
