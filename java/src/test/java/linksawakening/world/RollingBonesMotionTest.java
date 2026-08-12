package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RollingBonesMotionTest {

    @Test
    void bossWalksToTheBarThenLaunchesItAfterTheRomDelays() {
        RollingBonesMotion motion = new RollingBonesMotion();
        RoomEntity boss = entity(0, RollingBonesMotion.ENTITY_BOSS, 0x68, 0x38);
        RoomEntity bar = entity(1, RollingBonesMotion.ENTITY_BAR, 0x38, 0x38);
        motion.initializeBoss(0);
        motion.initializeBar(1);

        RollingBonesMotion.BossUpdate halfStep = motion.advanceBoss(
            boss, bar, 0, 0x08, (entity, direction, nextX, nextY) -> false);
        RollingBonesMotion.BossUpdate approach = motion.advanceBoss(
            halfStep.entity(), bar, 0, 0x08,
            (entity, direction, nextX, nextY) -> false);
        assertEquals(0x67, approach.entity().x());
        assertEquals(0, motion.bossState(0));

        boss = withX(approach.entity(), 0x47);
        RollingBonesMotion.BossUpdate reached = motion.advanceBoss(
            boss, bar, 0, 0x08, (entity, direction, nextX, nextY) -> false);
        assertEquals(1, motion.bossState(0));
        assertEquals(0x18, reached.transitionCountdown());

        RollingBonesMotion.BossUpdate launch = motion.advanceBoss(
            reached.entity(), bar, 0, 0x08,
            (entity, direction, nextX, nextY) -> false);
        assertEquals(2, motion.bossState(0));
        assertEquals(0x20, launch.transitionCountdown());
        assertEquals(1, motion.barState(1));
        assertEquals(-0x10, motion.barSpeedX(1));
    }

    @Test
    void rollingBarReboundsAtHalfSpeedThenDeceleratesToRest() {
        RollingBonesMotion motion = new RollingBonesMotion();
        RoomEntity bar = entity(1, RollingBonesMotion.ENTITY_BAR, 0x38, 0x38);
        motion.initializeBar(1);
        motion.launchBar(1, 0x10);

        RollingBonesMotion.BarUpdate rolling = motion.advanceBar(
            bar, 1, (entity, direction, nextX, nextY) -> true);
        assertTrue(rolling.strongBump());
        assertEquals(0x20, rolling.screenShakeCountdown());
        assertEquals(-0x08, motion.barSpeedX(1));
        assertEquals(2, motion.barState(1));

        RoomEntity current = rolling.entity();
        for (int frame = 8; frame <= 72; frame += 8) {
            RollingBonesMotion.BarUpdate update = motion.advanceBar(
                current, frame, (entity, direction, nextX, nextY) -> false);
            current = update.entity();
        }
        assertEquals(0, motion.barSpeedX(1));
        assertEquals(0, motion.barState(1));
        assertEquals(0x50, motion.barTransitionCountdown(1));
    }

    @Test
    void airborneLinkDoesNotCollideWithTheRollingBar() {
        assertTrue(RollingBonesMotion.barCanDamageLink(0));
        assertFalse(RollingBonesMotion.barCanDamageLink(1));
        assertFalse(RollingBonesMotion.barCanDamageLink(0x18));
    }

    @Test
    void bossRectangleVariantIncludesTheRomDirectionOffset() {
        RollingBonesMotion motion = new RollingBonesMotion();
        RoomEntity boss = entity(0, RollingBonesMotion.ENTITY_BOSS, 0x10, 0x38);
        RoomEntity bar = entity(1, RollingBonesMotion.ENTITY_BAR, 0x38, 0x38);
        motion.initializeBoss(0);

        RollingBonesMotion.BossUpdate first = motion.advanceBoss(
            boss, bar, 0, 0x08, (entity, direction, nextX, nextY) -> false);
        RollingBonesMotion.BossUpdate update = motion.advanceBoss(
            first.entity(), bar, first.transitionCountdown(), 0x08,
            (entity, direction, nextX, nextY) -> false);

        assertEquals(4, update.entity().spriteVariant());
    }

    private static RoomEntity entity(int slot, int type, int x, int y) {
        return new RoomEntity(slot, slot, type, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(type), 0);
    }

    private static RoomEntity withX(RoomEntity entity, int x) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x,
            entity.y(), entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }
}
