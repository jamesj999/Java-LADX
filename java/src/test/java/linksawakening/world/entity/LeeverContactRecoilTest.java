package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import org.junit.jupiter.api.Test;

final class LeeverContactRecoilTest {
    @Test
    void acceptedContactKeepsCachedHitboxButUsesMovedPositionForRecoil() {
        var runtime = chasingLeever(110, 40);
        var contact = runtime.resolveCombat(new EntitySwordContactFrame(35, 109, 39, false, true, false, 0, 0, 0, 0))
                .getFirst();
        assertEquals(4, contact.linkDamage());
        assertTrue(runtime.deferContactRecoil(contact.slot(), 109, 39));

        runtime.tick(EntityTestFrames.frame(runtime, 35, 109, 39, () -> 0));

        var response = runtime.consumePendingEntityEvents().getFirst();
        assertEquals(109, runtime.inspection().snapshot().slots().get(2).x());
        assertResponse(response, 0, 0xEC);
    }

    @Test
    void backgroundRestorationKeepsTheOriginalRecoilVector() {
        var runtime = chasingLeever(64, 65);
        runtime.resolveCombat(new EntitySwordContactFrame(35, 63, 64, false, true, false, 0, 0, 0, 0));
        assertTrue(runtime.deferContactRecoil(2, 63, 64));

        runtime.tick(EntityTestFrames.frame(runtime, 35, 63, 64, () -> 0, (entity, direction, x, y) -> true));

        assertEquals(64, runtime.inspection().snapshot().slots().get(2).x());
        assertResponse(runtime.consumePendingEntityEvents().getFirst(), 0xEC, 0xEC);
    }

    @Test
    void contactsRejectedByThePlayerDamageGateDoNotWriteRecoil() {
        var runtime = chasingLeever(110, 40);
        runtime.resolveCombat(new EntitySwordContactFrame(35, 109, 39, false, true, false, 0, 0, 0, 0));
        runtime.tick(EntityTestFrames.frame(runtime, 35, 109, 39, () -> 0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    @Test
    void skippedHandlerDoesNotLeaveAResponseForTheFollowingFrame() {
        var runtime = chasingLeever(110, 40);
        runtime.resolveCombat(new EntitySwordContactFrame(35, 109, 39, false, true, false, 0, 0, 0, 0));
        runtime.deferContactRecoil(2, 109, 39);
        runtime.configuration().setDialogActive(true);
        runtime.tick(EntityTestFrames.frame(runtime, 35, 109, 39, () -> 0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
        runtime.configuration().setDialogActive(false);
        runtime.tick(EntityTestFrames.frame(runtime, 36, 109, 39, () -> 0));
        assertTrue(runtime.consumePendingEntityEvents().isEmpty());
    }

    private static void assertResponse(EntityCombatEvent event, int speedX, int speedY) {
        assertEquals(2, event.slot());
        assertEquals(0, event.linkDamage());
        assertEquals(EntityCombatEvent.SoundChannel.NONE, event.soundChannel());
        assertEquals(new EntityCombatEvent.LinkCollisionResponse(speedX, speedY, 0x10), event.linkCollisionResponse());
    }

    private static RoomEntityRuntime chasingLeever(int x, int y) {
        var slots = new ArrayList<RoomEntity>();
        for (int slot = 0; slot < 16; slot++) slots.add(RoomEntity.disabled(slot));
        var sprite = new EntitySpriteDefinition(
                0x0E,
                4,
                0x7EE5,
                EntitySpriteDefinition.Shape.PAIR,
                0,
                Collections.nCopies(
                        4,
                        new EntitySpriteDefinition.Variant(
                                new EntitySpriteDefinition.OamAttribute(0, 0),
                                new EntitySpriteDefinition.OamAttribute(0, 0))));
        slots.set(
                2,
                new RoomEntityBuilder()
                        .slot(2)
                        .sourceLoadOrder(0)
                        .type(0x0E)
                        .at(x, y)
                        .spriteDefinition(sprite)
                        .build());
        var runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(slots)).build();
        for (int frame = 0; frame <= 34; frame++)
            runtime.tick(EntityTestFrames.frame(runtime, frame, x - 10, y, () -> 0));
        assertEquals(2, runtime.enemies().leeverState(2));
        assertEquals(0xF8, runtime.enemies().leeverSpeedX(2));
        return runtime;
    }
}
