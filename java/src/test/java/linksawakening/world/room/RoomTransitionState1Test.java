package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.List;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomTransitionState1Test {

    @Test
    void keyCavernEntranceRouteEarnsAndSpendsFirstKeyToReachNorthBasement() throws Exception {
        byte[] rom = loadRom();
        RomTables tables = RomTables.load(TestRoms.partial(rom));
        OverworldCollision collision = new OverworldCollision(tables);
        RoomSession session = newSession(rom, tables, collision);
        TransitionController transition = new TransitionController();
        ScrollController scroll = new ScrollController();
        RoomTransitionCoordinator coordinator =
                new RoomTransitionCoordinator(session, new RoomBoundaryController(), transition, scroll);
        InputState input = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new Link(input, inputConfig, tables, collision, null, new PlayerState(), new ItemRegistry());
        session.rooms().loadInitialOverworld(0xB5);
        session.rooms().loadIndoor(0x02, 0x52);
        link.position().setPixelPosition(0x48, 0x6C);
        int frame = 0;
        // Approach an authored pot from above, using the bracelet's ROM object probe.
        List<int[]> potPath = reachablePositionPath(
                collision,
                link.position().pixelX(),
                link.position().pixelY(),
                (x, y) -> objectAt(session, ((y + 0x10) & 0xF0) | (((x + 8) & 0xF0) >>> 4)) == 0x20);
        assertNotNull(potPath, collisionGrid(collision));
        for (int[] position : potPath) link.position().setPixelPosition(position[0], position[1]);
        for (int pull = 0; pull < 8; pull++) {
            assertTrue(session.doors()
                    .tryLiftIndoorObject(
                            link.position().pixelX(),
                            link.position().pixelY(),
                            Direction.fromJavaOrUnknown(Direction.DOWN.javaIndex()),
                            true,
                            0x04));
        }
        for (int tick = 0; tick < 0x40 && session.rooms().liftedEntityState().carryState() != 1; tick++) {
            tickInteractiveEntities(
                    session,
                    frame++,
                    link.position().romEntityX(),
                    link.position().romEntityY());
        }
        assertEquals(1, session.rooms().liftedEntityState().carryState());
        List<int[]> throwPath = reachablePositionPath(
                collision, link.position().pixelX(), link.position().pixelY(), (x, y) -> x == 0x40 && y == 0x18);
        assertNotNull(throwPath, collisionGrid(collision));
        for (int[] position : throwPath) {
            link.position().setPixelPosition(position[0], position[1]);
            session.entities()
                    .tickEntities(
                            frame++,
                            link.position().romEntityX(),
                            link.position().romEntityY(),
                            0,
                            Direction.UP.javaIndex());
        }
        assertTrue(session.playerItems().throwLiftedEntity(Direction.fromJava(Direction.UP.javaIndex())));
        for (int tick = 0; tick < 0x40 && RoomDebugFixture.of(session).activeRoomEventForTest() != 0; tick++) {
            tickInteractiveEntities(
                    session,
                    frame++,
                    link.position().romEntityX(),
                    link.position().romEntityY());
        }
        assertEquals(0, RoomDebugFixture.of(session).activeRoomEventForTest());
        walkToAndCrossIndoorBoundary(coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x51, session.rooms().currentRoomId());
        assertEquals(0x61, RoomDebugFixture.of(session).activeRoomEventForTest());
        assertEquals(0x0D, objectAt(session, 0x28));
        tickInteractiveEntities(session, frame++, 0x48, 0x70);
        List<Integer> enemies = session.rooms().activeRoom().entities().loadedEntities().stream()
                .map(RoomEntity::slot)
                .toList();
        assertEquals(4, enemies.size());
        for (int slot : enemies) {
            for (int attempt = 0; attempt < 12; attempt++) {
                RoomEntity enemy =
                        session.rooms().activeRoom().entities().slots().get(slot);
                if (!enemy.loaded()) break;
                session.entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                slot ^ 1,
                                enemy.x(),
                                enemy.y() + 0x20,
                                false,
                                0,
                                true,
                                new SwordBox(true, enemy.x(), 0x10, enemy.y(), 0x10),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
                for (int tick = 0; tick < 0x60; tick++) {
                    tickInteractiveEntities(session, frame++, 0x48, 0x70);
                }
            }
        }
        assertEquals(
                0,
                RoomDebugFixture.of(session).activeRoomEventForTest(),
                session.rooms().activeRoom().entities().loadedEntities().toString());
        assertEquals(0xA0, objectAt(session, 0x28));
        List<int[]> chestPath = reachablePositionPath(
                collision,
                link.position().pixelX(),
                link.position().pixelY(),
                (x, y) -> (x + 8) / 16 == 8 && (y + 5) / 16 == 2);
        assertNotNull(chestPath, collisionGrid(collision));
        for (int[] position : chestPath) link.position().setPixelPosition(position[0], position[1]);
        ChestOpenResult chest = session.doors()
                .tryOpenChest(
                        link.position().pixelX(),
                        link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        1);
        assertTrue(chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, chest.itemType());
        session.entities()
                .tickEntities(
                        frame++, link.position().romEntityX(), link.position().romEntityY());
        assertEquals(1, session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        walkToAndCrossIndoorBoundary(coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x4F, session.rooms().currentRoomId());
        assertEquals(0x21, RoomDebugFixture.of(session).activeRoomEventForTest());
        tickInteractiveEntities(
                session, frame++, link.position().romEntityX(), link.position().romEntityY());
        List<Integer> hiddenZols = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x9B)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(4, hiddenZols.size());
        for (int slot : hiddenZols) {
            for (int attempt = 0; attempt < 0x200; attempt++) {
                RoomEntity enemy =
                        session.rooms().activeRoom().entities().slots().get(slot);
                if (!enemy.loaded()) break;
                tickInteractiveEntities(session, frame++, enemy.x(), enemy.y() + 0x10);
                session.entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                slot ^ 1,
                                enemy.x(),
                                enemy.y() + 0x10,
                                false,
                                0,
                                true,
                                new SwordBox(true, enemy.x(), 0x10, enemy.y(), 0x10),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
            }
        }
        for (int tick = 0; tick < 0x40; tick++) {
            tickInteractiveEntities(
                    session,
                    frame++,
                    link.position().romEntityX(),
                    link.position().romEntityY());
        }
        assertEquals(
                0,
                RoomDebugFixture.of(session).activeRoomEventForTest(),
                session.rooms().activeRoom().entities().loadedEntities().toString());
        walkToAndCrossIndoorBoundary(coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x4E, session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x4C, session.rooms().currentRoomId());
        Warp stairs = session.rooms().activeRoom().warps().getFirst();
        assertEquals(0x56, stairs.destRoom());
        coordinator.handleWarpAndIndoorBoundaries(link);
        List<int[]> stairPath = reachablePositionPath(
                collision, link.position().pixelX(), link.position().pixelY(), (x, y) -> x == 0x60 && y == 0x30);
        assertNotNull(stairPath, collisionGrid(collision));
        for (int[] position : stairPath) {
            link.position().setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            if (transition.isActive()) break;
        }
        assertTrue(transition.isActive());
        while (transition.isActive()) transition.tick();
        assertEquals(0x56, session.rooms().currentRoomId());
        assertEquals(1, session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        List<int[]> keyDoorPath = reachablePositionPath(
                collision, link.position().pixelX(), link.position().pixelY(), (x, y) -> x == 0x48 && y == 0x0A);
        assertNotNull(keyDoorPath, collisionGrid(collision));
        for (int[] position : keyDoorPath) link.position().setPixelPosition(position[0], position[1]);
        input.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
        link.motion().update();
        input.onKeyEvent(inputConfig.upKey(), GLFW_RELEASE);
        assertEquals(0x01, link.position().romCollisionType());
        assertEquals(0x0A, link.position().pixelY());
        assertTrue(session.doors()
                .tryUnlockIndoorKeyDoor(
                        link.position().romCollisionProbePixelX(),
                        link.position().romCollisionProbePixelY(),
                        Direction.fromJavaOrUnknown(link.animation().direction()),
                        link.position().romCollisionType()));
        for (int tick = 0; tick < 9; tick++) {
            tickInteractiveEntities(session, frame++, 0x48, 0x19);
        }
        assertEquals(0, session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        walkToAndCrossIndoorBoundary(coordinator, transition, scroll, collision, link, ScrollController.UP);
        assertEquals(0x54, session.rooms().currentRoomId());
        assertEquals(0x04, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x02, 0x56) & 0x04);
        assertEquals(0x08, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x02, 0x54) & 0x08);
        session.rooms().loadIndoor(0x02, 0x51);
        assertEquals(0xA1, objectAt(session, 0x28));
        assertFalse(session.doors()
                .tryOpenChest(0x78, 0x21, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 1)
                .opened());
        assertEquals(0, session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
    }
}
