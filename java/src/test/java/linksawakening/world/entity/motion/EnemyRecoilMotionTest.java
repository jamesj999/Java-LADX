package linksawakening.world.entity.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class EnemyRecoilMotionTest {

    @Test
    void octorokShieldRecoilMaintainsTheNativePositionFractionsAndPause() {
        int slot = 1;
        RoomEntity octorok = entity(slot, 0x47, 0x18, 0);
        var recoil = new EnemyRecoilMotion();
        var roaming = new RoamingEnemyMotion();
        roaming.initialize(slot);
        roaming.speedXAccumulator()[slot] = 0x80;
        roaming.speedYAccumulator()[slot] = 0x80;
        recoil.configureFromSpeed(slot, 0x20, 0xFE);
        int[] expectedY = {0x18, 0x18, 0x18, 0x18, 0x17, 0x17, 0x17, 0x17, 0x17, 0x17};
        for (int i = 0; i < expectedY.length; i++) {
            octorok = roaming.applyIncomingRecoil(octorok, RoomEntityBackgroundCollision.OPEN, recoil)
                    .entity();
            assertEquals(0x49 + i * 2, octorok.x());
            assertEquals(expectedY[i], octorok.y());
            assertEquals(0x40, roaming.transitionCountdown(slot));
        }
        assertEquals(0x80, roaming.speedXAccumulator()[slot]);
        assertEquals(0x40, roaming.speedYAccumulator()[slot]);
    }

    @Test
    void bankThreeStopsOnTheDominantAxisAndBankFourKeepsItsCountdown() {
        RoomEntity target = entity(0, 0x31, 0x2C, 0);
        var recoil = new EnemyRecoilMotion();
        recoil.configureFromSpeed(0, 0x15, 0x20);
        var roaming = new RoamingEnemyMotion();
        var leever = new LeeverMotion();
        RoomEntityBackgroundCollision horizontalWall = (e, direction, x, y) -> direction == 0;
        RoomEntityBackgroundCollision verticalWall = (e, direction, x, y) -> direction == 3;
        assertFalse(roaming.applyIncomingRecoil(target, horizontalWall, recoil).blocked());
        assertTrue(roaming.applyIncomingRecoil(target, verticalWall, recoil).blocked());
        assertFalse(leever.applyIncomingRecoil(target, verticalWall, recoil).blocked());
    }

    @Test
    void leeverShieldRecoilKeepsIntegratingPastTheBlockedVerticalAxis() {
        int slot = 2;
        RoomEntity leever = entity(slot, 0x31, 0x2C, 0);
        var recoil = new EnemyRecoilMotion();
        recoil.configureFromSpeed(slot, 0x15, 0x20);
        int[] fractionX = {0, 0, 0};
        int[] fractionY = {0, 0, 0x80};
        int[] expectedX = {0x32, 0x33, 0x34, 0x36, 0x37, 0x38, 0x3A, 0x3B, 0x3C, 0x3E};
        RoomEntityBackgroundCollision wall = (e, direction, x, y) -> direction == 3 && y > 0x32;
        for (int i = 0; i < expectedX.length; i++) {
            leever = recoil.advanceWithHandlerFractions(leever, wall, fractionX, fractionY, false)
                    .entity();
            assertEquals(expectedX[i], leever.x());
            assertEquals(Math.min(0x2E + 2 * i, 0x32), leever.y());
        }
        assertEquals(0x20, fractionX[slot]);
        assertEquals(0x80, fractionY[slot]);
    }

    @Test
    void configureMirrorsRomVectorAwayFromLinkAcrossTheDominantAxes() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();

        motion.configure(0, 64, 64, 0, 100, 64, 0x30);
        assertEquals(0xD0, motion.recoilSpeedX(0));
        assertEquals(0x00, motion.recoilSpeedY(0));

        motion.configure(0, 64, 64, 0, 64, 28, 0x30);
        assertEquals(0x00, motion.recoilSpeedX(0));
        assertEquals(0x30, motion.recoilSpeedY(0));

        motion.configure(0, 64, 64, 0, 100, 100, 0x30);
        assertEquals(0xD0, motion.recoilSpeedX(0));
        assertEquals(0xD0, motion.recoilSpeedY(0));

        motion.configure(0, 64, 64, 0, 96, 80, 0x30);
        assertEquals(0xD0, motion.recoilSpeedX(0));
        assertEquals(0xE8, motion.recoilSpeedY(0));
    }

    @Test
    void configureUsesEntityZInTheRomYDistance() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();

        motion.configure(0, 64, 64, 8, 64, 64, 0x30);

        // Link is eight pixels below the entity's Z-adjusted Y coordinate;
        // recoil therefore points eight pixels upward in the ROM vector.
        assertEquals(0x00, motion.recoilSpeedX(0));
        assertEquals(0xD0, motion.recoilSpeedY(0));
    }

    @Test
    void configureUsesRomTieBreakWhenEntityAndLinkOverlap() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();

        motion.configure(0, 64, 64, 0, 64, 64, 0x30);

        assertEquals(0xD0, motion.recoilSpeedX(0));
        assertEquals(0xD0, motion.recoilSpeedY(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 8, 0xF8})
    void overlappingVisualPositionRecoilsUpAndLeftForEveryEntityHeight(int z) {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();
        int linkY = (0x50 - z) & 0xFF;
        motion.configure(0, 0x40, 0x50, z, 0x40, linkY, 0x30);
        RoomEntity result = motion.advance(
                        entity(0, 0x40, 0x50, z), linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .entity();
        assertEquals(0x3D, result.x());
        assertEquals(0x4D, result.y());
        assertEquals(
                new EnemyRecoilMotion.Vector(0x30, 0x30),
                EnemyRecoilMotion.vectorTowardsLink(0x40, 0x50, z, 0x40, linkY, 0x30));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sword", "projectile", "bomb"})
    void assigningAnotherRecoilVelocityPreservesBothFractionalCoordinates(String source) {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();
        motion.configureFromSpeed(0, 8, 0xF8);
        RoomEntity first = motion.advance(
                        entity(0, 0x40, 0x50, 0), linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .entity();
        assertEquals(0x40, first.x());
        assertEquals(0x4F, first.y());
        switch (source) {
            case "sword" -> motion.configure(0, first.x(), first.y(), 0, first.x() - 0x10, first.y() + 0x10, 8);
            case "projectile" -> motion.configureFromSpeed(0, 8, 0xF8);
            case "bomb" -> motion.configureFromSource(0, 0x20, 0x60, 0, 0x30, 0x50, 8);
            default -> throw new AssertionError(source);
        }
        RoomEntity second = motion.advance(first, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .entity();
        assertEquals(0x41, second.x());
        assertEquals(0x4F, second.y());
    }

    @Test
    void configureFromSpeedCopiesProjectileVelocityWithoutChangingItsSign() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();

        motion.configureFromSpeed(0, 0x40, 0xD0);

        assertEquals(0x40, motion.recoilSpeedX(0));
        assertEquals(0xD0, motion.recoilSpeedY(0));
        assertTrue(motion.isActive(0));
    }

    @Test
    void configureFromSourceMirrorsBombVectorSignsAndSourceZ() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();

        motion.configureFromSource(0, 0x48, 0x50, 0x01, 0x40, 0x4F, 0x30);
        assertEquals(0xD0, motion.recoilSpeedX(0));
        assertEquals(0x00, motion.recoilSpeedY(0));

        motion.configureFromSource(0, 0x40, 0x50, 0x00, 0x40, 0x40, 0x30);
        assertEquals(0x00, motion.recoilSpeedX(0));
        assertEquals(0xD0, motion.recoilSpeedY(0));

        // The source Z is added by GetEntityYDistanceToLink; it makes this
        // target's raw Y coordinate level with the source for vector math.
        motion.configureFromSource(0, 0x40, 0x50, 0x08, 0x50, 0x48, 0x30);
        assertEquals(0x30, motion.recoilSpeedX(0));
        assertEquals(0x00, motion.recoilSpeedY(0));
    }

    @Test
    void advanceUsesSignedSixteenSubpixelMovementAndUnsignedWrapping() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();
        RoomEntity entity = entity(0, 0x02, 0x20, 0);

        motion.configure(0, entity.x(), entity.y(), entity.z(), 0x0A, 0x20, 0x08);

        RoomEntity first = motion.advance(entity, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .entity();
        RoomEntity second = motion.advance(first, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .entity();
        RoomEntity third = motion.advance(second, linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .entity();

        // A speed of -$08 is -0.5 pixels/frame in the ROM accumulator.
        assertEquals(0x01, first.x());
        assertEquals(0x01, second.x());
        assertEquals(0x00, third.x());
        assertEquals(entity.y(), first.y());
        assertTrue(motion.isActive(0));

        motion.configure(0, 0x00, 0x40, 0, 0x40, 0x40, 0x30);
        RoomEntity wrapped = motion.advance(
                        entity(0, 0x00, 0x40, 0), linksawakening.world.entity.RoomEntityBackgroundCollision.OPEN)
                .entity();
        assertEquals(0xFD, wrapped.x());
    }

    @Test
    void advanceStopsRecoilWhenTheDominantAxisHitsBackground() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();
        RoomEntity entity = entity(0, 0x40, 0x40, 0);
        motion.configure(0, entity.x(), entity.y(), entity.z(), 0x70, 0x40, 0x30);

        EnemyRecoilMotion.Update update =
                motion.advance(entity, (candidate, direction, nextX, nextY) -> direction == 1);

        assertEquals(entity.x(), update.entity().x());
        assertEquals(entity.y(), update.entity().y());
        assertTrue(update.blocked());
        assertFalse(motion.isActive(0));
    }

    @Test
    void clearIsIdempotent() {
        EnemyRecoilMotion motion = new EnemyRecoilMotion();
        motion.configure(0, 64, 64, 0, 96, 64, 0x30);

        motion.clear(0);
        motion.clear(0);

        assertFalse(motion.isActive(0));
        assertEquals(0, motion.recoilSpeedX(0));
        assertEquals(0, motion.recoilSpeedY(0));
    }

    private static RoomEntity entity(int slot, int x, int y, int z) {
        return new RoomEntityBuilder()
                .slot(slot)
                .sourceLoadOrder(0)
                .type(0x09)
                .at(x, y)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x09))
                .spriteVariant(-1)
                .z(z)
                .build();
    }
}
