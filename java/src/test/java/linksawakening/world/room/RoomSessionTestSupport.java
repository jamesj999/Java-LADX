package linksawakening.world.room;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTestFrames;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.EntityCombatEvent;

final class RoomSessionTestSupport {
    static int[] torchTiles(RoomSession session, int location) {
        int index = (location >>> 4) * 2 * RoomConstants.ROOM_TILE_WIDTH + (location & 0x0F) * 2;
        int[] tiles = session.activeRoom().tileIds();
        int[] attributes = session.activeRoom().tileAttrs();
        int lower = index + RoomConstants.ROOM_TILE_WIDTH;
        return new int[] {
            tiles[index],
            tiles[index + 1],
            tiles[lower],
            tiles[lower + 1],
            attributes[index],
            attributes[index + 1],
            attributes[lower],
            attributes[lower + 1]
        };
    }

    static RoomSession newSession() {
        return newSession(room -> {});
    }

    static void tickInteractive(RoomSession session, int frame, RoomEntity witch) {
        session.tickEntitiesWithProjectileEvents(frame, witch.x(), 0x56, 0, 0, 1, false);
    }

    static void tickInteractive(RoomSession session, int frame, int linkEntityX, int linkEntityY) {
        session.tickEntitiesWithProjectileEvents(frame, linkEntityX, linkEntityY, 0, 0, Link.DIRECTION_DOWN, false);
    }

    static RoomSession newSession(RoomLoadListener roomLoadListener) {
        return newSession(new TransientVfxSystem(16), roomLoadListener);
    }

    static RoomSession newSession(TransientVfxSystem transientVfxSystem) {
        return newSession(transientVfxSystem, room -> {});
    }

    static RoomSession newSession(TransientVfxSystem transientVfxSystem, RoomLoadListener roomLoadListener) {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        return new RoomSession(
                rom,
                new GPU(),
                new RoomLoader(rom),
                new OverworldTilesetTable(rom),
                new OverworldCollision(romTables),
                transientVfxSystem,
                null,
                roomLoadListener);
    }

    static byte[] loadRom() {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }

    static byte[] pattern(int length, int start) {
        byte[] values = new byte[length];
        for (int index = 0; index < values.length; index++) {
            values[index] = (byte) (start + index);
        }
        return values;
    }

    static void fillActiveObjects(RoomSession session, int objectId) {
        int[] roomObjects = session.activeRoom().roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] =
                        objectId;
            }
        }
    }

    static RoomEntity syntheticEntity(int type) {
        return new RoomEntity(
                0,
                0,
                type,
                0x20,
                0x30,
                EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(type),
                -1,
                0,
                0,
                0,
                -1,
                false);
    }

    static RoomEntityRuntime runtimeWithKillOrder(byte[] rom, int... killOrder) {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(rom);
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        int[] entitySlots = {15, 14, 13};
        int[] entityX = {0x20, 0x70, 0xC0};
        for (int sourceOrder = 0; sourceOrder < entitySlots.length; sourceOrder++) {
            int slot = entitySlots[sourceOrder];
            slots.set(
                    slot,
                    new RoomEntity(
                            slot,
                            sourceOrder,
                            0x0B,
                            entityX[sourceOrder],
                            0x50,
                            EntityStatus.ACTIVE,
                            catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.INDOORS_A),
                            0,
                            0,
                            0,
                            0,
                            -1,
                            false));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(
                new RoomEntitySnapshot(slots), true, () -> 0, catalog, new RomEnemyCombatTables(rom));
        runtime.configuration().setEnemyDropResolver(new EnemyDropResolver(rom));
        int frame = 0;
        for (int sourceOrder : killOrder) {
            int slot = entitySlots[sourceOrder];
            RoomEntity target = runtime.inspection().snapshot().slots().get(slot);
            linksawakening.world.entity.EntityRuntimeTestAccess.setEnemyHealthForTest(runtime, slot, 1);
            List<EntityCombatEvent> hit =
                    runtime.resolveCombat(frame++, 0, 0, false, true, true, target.x() - 8, 0x10, target.y() - 8, 0x10);
            assertTrue(hit.stream().anyMatch(event -> event.slot() == slot && event.swordHit()));
            int deathDeadline = frame + 0x80;
            while (runtime.inspection().snapshot().slots().get(slot).loaded() && frame < deathDeadline) {
                runtime.tick(EntityTestFrames.frame(runtime, frame++, 0, 0, () -> 0));
            }
            assertFalse(runtime.inspection().snapshot().slots().get(slot).loaded());
        }
        return runtime;
    }
}
