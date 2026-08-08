package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MiniMoldormMotionTest {

    @Test
    void recordsPositionHistoryBySourceLoadOrderAndSelectsTheRomAngle() {
        RoomEntity entity = entity(2, 5, 0x50, 0x60, 0);
        MiniMoldormMotion motion = new MiniMoldormMotion();
        motion.initialize(entity);

        motion.beginFrame(entity, false);
        assertEquals(1, motion.inertia(entity.slot()));
        assertEquals(0x50, motion.historyX(entity.sourceLoadOrder(), 1));
        assertEquals(0x60, motion.historyY(entity.sourceLoadOrder(), 1));

        MiniMoldormMotion.Update update = motion.advanceAfterHistory(
            entity, 0, 0, 0, null, () -> 0, 0);

        assertEquals(0x50, update.entity().x());
        assertEquals(0x60, update.entity().y());
        assertEquals(0x03, update.headSpriteVariant());
        assertEquals(0x10, update.transitionCountdown());
        assertEquals(-1, motion.privateState2(entity.slot()));
        assertEquals(0x10, motion.speedX(entity.slot()));
        assertEquals(0x00, motion.speedY(entity.slot()));
    }

    @Test
    void resetsOnlyHistoryWhenHitsAreIgnoredThenUsesFixedPointSpeed() {
        RoomEntity entity = entity(0, 7, 0x50, 0x60, 0);
        MiniMoldormMotion motion = new MiniMoldormMotion();
        motion.initialize(entity);
        motion.beginFrame(entity, false);
        motion.advanceAfterHistory(entity, 0, 0, 0, null, () -> 0, 0);

        RoomEntity moved = new RoomEntity(0, 7, 0x29, 0x50, 0x60,
            EntityStatus.ACTIVE, entity.spriteDefinition(), 0);
        motion.beginFrame(moved, true);
        assertEquals(2, motion.inertia(moved.slot()));
        assertEquals(0, motion.historyX(moved.sourceLoadOrder(), 0));
        assertEquals(0, motion.historyX(moved.sourceLoadOrder(), 1));
        assertEquals(0x50, motion.historyX(moved.sourceLoadOrder(), 2));

        MiniMoldormMotion.Update update = motion.advanceAfterHistory(
            moved, 0x0F, 0, 0, null, () -> 0, 1);

        assertEquals(0x51, update.entity().x());
        assertEquals(0x60, update.entity().y());
        assertEquals(0x0E, update.transitionCountdown());
        assertEquals(3, update.headSpriteVariant());
    }

    @Test
    void collisionUsesRomDirectionBitsAndRandomTurningChoice() {
        RoomEntity entity = entity(0, 0, 0x50, 0x60, 0);
        MiniMoldormMotion motion = new MiniMoldormMotion();
        motion.initialize(entity);
        motion.beginFrame(entity, false);
        motion.advanceAfterHistory(entity, 0, 0, 0, null, () -> 0, 0);

        AtomicInteger probes = new AtomicInteger();
        RoomEntityBackgroundInteraction blockedRight = (candidate, direction, nextX, nextY) -> {
            probes.incrementAndGet();
            return direction == EntityBackgroundCollisionResult.RIGHT
                ? EntityBackgroundCollisionResult.blocked(direction,
                    EntityBackgroundCollisionResult.NO_OBJECT, 0, nextX, nextY)
                : EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
        };

        RoomEntity moved = new RoomEntity(0, 0, 0x29, 0x50, 0x60,
            EntityStatus.ACTIVE, entity.spriteDefinition(), 0);
        motion.beginFrame(moved, false);
        MiniMoldormMotion.Update update = motion.advanceAfterHistory(
            moved, 0x0F, 0, 0, blockedRight, () -> 0, 1);

        assertEquals(0x50, update.entity().x());
        assertEquals(0x10, update.transitionCountdown());
        assertEquals(0x08, motion.privateState1(0));
        assertEquals(1, motion.privateState2(0));
        assertEquals(1, update.collisionFlags());
        assertEquals(1, probes.get());
    }

    private static RoomEntity entity(int slot, int loadOrder, int x, int y, int variant) {
        return new RoomEntity(slot, loadOrder, 0x29, x, y, EntityStatus.ACTIVE,
            EntitySpriteDefinition.dynamic(0x29, 0x04, 0x5A49, 0,
                java.util.List.of(java.util.List.of(
                    new EntitySpriteDefinition.DynamicSprite(0, 0,
                        new EntitySpriteDefinition.OamAttribute(0x70, 0),
                        EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                        true)))),
            variant);
    }
}
