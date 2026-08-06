package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EntityBackgroundCollisionResolverTest {

    @Test
    void fineAndOpenDoorCollisionUseTheRomShapeQuadrantAtTheSamplePoint() {
        RomTables tables = tables(1, 0, 1, 0, 0x30, 0);
        RoomEntity entity = entity(0x30, 0);

        assertTrue(resolve(tables, entity, 0x00, 0x00, 0x80).blocked());
        assertFalse(resolve(tables, entity, 0x08, 0x00, 0x80).blocked());
        assertTrue(resolve(tables, entity, 0x00, 0x08, 0x80).blocked());
        assertFalse(resolve(tables, entity, 0x08, 0x08, 0x80).blocked());

        RomTables openDoorTables = tablesWithOpenDoorFineRow(
            1, 0, 1, 0, 0, 0, 0, 0, entity.type(), 0);
        assertTrue(resolve(openDoorTables, entity, 0, 0, 0x7C).blocked());
        assertFalse(resolve(openDoorTables, entity, 0x08, 0x00, 0x7C).blocked());
    }

    @Test
    void ordinaryEntitiesPassNoneAndDeepWaterButBlockSolidAndHookshotableObjects() {
        RoomEntity entity = entity(0x30, 0);
        RomTables tables = tables(0, 0, 0, 0, entity.type(), 0);

        assertFalse(resolve(tables, entity, 0, 0, PhysicsFlags.NONE).blocked());
        assertFalse(resolve(tables, entity, 0, 0, PhysicsFlags.DEEP_WATER).blocked());
        assertTrue(resolve(tables, entity, 0, 0, PhysicsFlags.SOLID).blocked());
        assertTrue(resolve(tables, entity, 0, 0, 0x60).blocked());
    }

    @Test
    void collisionResultRetainsTheObjectPhysicsSampleAndRightDirectionFlag() {
        RoomEntity entity = entity(0x30, 0);
        EntityBackgroundCollisionResult result = resolve(
            tables(0, 0, 0, 0, entity.type(), 0), entity, 0x2A, 0x35,
            PhysicsFlags.SOLID);

        assertTrue(result.blocked());
        assertEquals(0x22, result.objectId());
        assertEquals(PhysicsFlags.SOLID, result.physicsFlag());
        assertEquals(EntityBackgroundCollisionResult.RIGHT, result.direction());
        assertEquals(0x01, result.collisionFlag());
        assertEquals(0x2A, result.sampleX());
        assertEquals(0x35, result.sampleY());
    }

    @Test
    void fishAndWaterTektitesPassOnlyThroughShallowAndDeepWater() {
        for (int type : new int[] {0xCC, 0x99}) {
            RoomEntity waterEntity = entity(type, 0);
            RomTables tables = tables(0, 0, 0, 0, type, 0);

            assertFalse(resolve(tables, waterEntity, 0, 0,
                PhysicsFlags.SHALLOW_WATER).blocked());
            assertFalse(resolve(tables, waterEntity, 0, 0,
                PhysicsFlags.DEEP_WATER).blocked());
            assertTrue(resolve(tables, waterEntity, 0, 0,
                PhysicsFlags.SOLID).blocked());
            assertTrue(resolve(tables, waterEntity, 0, 0,
                PhysicsFlags.NONE).blocked());
        }
    }

    @Test
    void groundedEntitiesBlockPitsButAirborneEntitiesPassThem() {
        RoomEntity grounded = entity(0x09, 0);
        RoomEntity airborne = entity(0x09, 1);

        assertTrue(resolve(tables(0, 0, 0, 0, grounded.type(), 0), grounded,
            0, 0, PhysicsFlags.NORMAL_PIT).blocked());
        assertFalse(resolve(tables(0, 0, 0, 0, airborne.type(), 0), airborne,
            0, 0, PhysicsFlags.NORMAL_PIT).blocked());
    }

    @Test
    void ignoreHitsMakesGroundedPitsPassableExceptForMoldorm() {
        RoomEntity ordinary = entity(0x09, 0);
        for (int physics : new int[] {0x0B, 0x50, 0x51}) {
            RomTables tables = tables(0, 0, 0, 0, ordinary.type(), 0);
            assertTrue(resolve(tables, ordinary, 0, 0, physics).blocked());
            assertFalse(resolveWithIgnoreHits(tables, ordinary, 0, 0, physics, 1).blocked());
        }

        RoomEntity moldorm = entity(0x59, 0);
        RomTables moldormTables = tables(0, 0, 0, 0, moldorm.type(), 0);
        assertTrue(resolveWithIgnoreHits(moldormTables, moldorm, 0, 0, 0x50, 1).blocked());

        RoomEntity airborne = entity(ordinary.type(), 1);
        assertFalse(resolve(moldormTables, airborne, 0, 0, 0x50).blocked());
    }

    @Test
    void bombAndWreckingBallPassBlockingFineCollisionShapes() {
        RoomEntity bomb = entity(0x02, 0);
        RoomEntity wreckingBall = entity(0xA8, 0);

        assertFalse(resolve(tables(1, 1, 1, 1, bomb.type(), 0), bomb,
            0, 0, 0x80).blocked());
        assertFalse(resolve(tables(1, 1, 1, 1, wreckingBall.type(), 0), wreckingBall,
            0, 0, 0x80).blocked());
    }

    @Test
    void bombAndWreckingBallPassSwitchBlocksButOrdinaryEntitiesDoNot() {
        RoomEntity bomb = entity(0x02, 0);
        RoomEntity wreckingBall = entity(0xA8, 0);
        RoomEntity ordinary = entity(0x30, 0);

        assertFalse(resolve(tables(0, 0, 0, 0, bomb.type(), 0), bomb,
            0, 0, 0x04).blocked());
        assertFalse(resolve(tables(0, 0, 0, 0, wreckingBall.type(), 0), wreckingBall,
            0, 0, 0x04).blocked());
        assertTrue(resolve(tables(0, 0, 0, 0, ordinary.type(), 0), ordinary,
            0, 0, 0x04).blocked());
    }

    @Test
    void openDoorsAreSolidForSparksAndBosses() {
        RoomEntity counterClockwiseSpark = entity(0x16, 0);
        RoomEntity clockwiseSpark = entity(0x17, 0);
        RoomEntity boss = entity(0x40, 0);

        assertTrue(resolve(tables(0, 0, 0, 0, counterClockwiseSpark.type(), 0),
            counterClockwiseSpark, 0, 0, 0x7C).blocked());
        assertTrue(resolve(tables(0, 0, 0, 0, clockwiseSpark.type(), 0),
            clockwiseSpark, 0, 0, 0x7C).blocked());
        assertTrue(resolve(tables(0, 0, 0, 0, boss.type(), 0x80),
            boss, 0, 0, 0x7C).blocked());
    }

    @Test
    void noWallCollisionOptionMakesAGenericSolidObjectPassable() {
        RoomEntity entity = entity(0x30, 0);

        assertFalse(resolve(tables(0, 0, 0, 0, entity.type(), 0x01), entity,
            0, 0, PhysicsFlags.SOLID).blocked());
    }

    @Test
    void leftCollisionResultUsesTheLeftDirectionFlag() {
        RoomEntity entity = entity(0x30, 0);
        EntityBackgroundCollisionResult result = resolve(
            tables(0, 0, 0, 0, entity.type(), 0), entity,
            EntityBackgroundCollisionResult.LEFT, 0, 0, PhysicsFlags.SOLID);

        assertTrue(result.blocked());
        assertEquals(EntityBackgroundCollisionResult.LEFT, result.direction());
        assertEquals(0x02, result.collisionFlag());
    }

    private static RomTables tables(int fineRow0, int fineRow1,
                                    int fineRow2, int fineRow3,
                                    int entityType, int options) {
        return tablesWithFineRows(
            new int[] {0, 0, 0, 0},
            new int[] {fineRow0, fineRow1, fineRow2, fineRow3},
            entityType, options);
    }

    private static RomTables tablesWithOpenDoorFineRow(
            int openDoorRow0, int openDoorRow1, int openDoorRow2, int openDoorRow3,
            int fineRow0, int fineRow1, int fineRow2, int fineRow3,
            int entityType, int options) {
        return tablesWithFineRows(
            new int[] {openDoorRow0, openDoorRow1, openDoorRow2, openDoorRow3},
            new int[] {fineRow0, fineRow1, fineRow2, fineRow3},
            entityType, options);
    }

    private static RomTables tablesWithFineRows(int[] openDoorRow, int[] fineRow,
                                                int entityType, int options) {
        byte[] rom = new byte[0x100000];
        int fineOffset = RomBank.romOffset(0x03, 0x7A85) + (0x80 - 0x7C) * 4;
        writeFineRow(rom, fineOffset - (0x80 - 0x7C) * 4, openDoorRow);
        writeFineRow(rom, fineOffset, fineRow);
        rom[RomBank.romOffset(0x03, 0x42F1) + entityType] = (byte) options;
        return RomTables.loadFromRom(rom);
    }

    private static void writeFineRow(byte[] rom, int offset, int[] row) {
        for (int quadrant = 0; quadrant < row.length; quadrant++) {
            rom[offset + quadrant] = (byte) row[quadrant];
        }
    }

    private static RoomEntity entity(int type, int z) {
        return new RoomEntity(0, 0, type, 0x40, 0x40, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(type), -1, 0, 0, z);
    }

    private static EntityBackgroundCollisionResult resolve(
            RomTables tables, RoomEntity entity, int sampleX, int sampleY, int physics) {
        return resolve(tables, entity, EntityBackgroundCollisionResult.RIGHT,
            sampleX, sampleY, physics);
    }

    private static EntityBackgroundCollisionResult resolve(
            RomTables tables, RoomEntity entity, int direction,
            int sampleX, int sampleY, int physics) {
        return new EntityBackgroundCollisionResolver(tables).resolve(
            entity, direction,
            new EntityCollisionPointProbe.Sample(sampleX, sampleY), 0x22, physics);
    }

    private static EntityBackgroundCollisionResult resolveWithIgnoreHits(
            RomTables tables, RoomEntity entity, int sampleX, int sampleY, int physics,
            int ignoreHitsCountdown) {
        return new EntityBackgroundCollisionResolver(tables).resolve(
            entity, EntityBackgroundCollisionResult.RIGHT,
            new EntityCollisionPointProbe.Sample(sampleX, sampleY), 0x22, physics,
            ignoreHitsCountdown);
    }
}
