package linksawakening.app.gameplay.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.Link;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import org.junit.jupiter.api.Test;

class EntityBoundaryBehaviorTest {
    private static Link link() {
        var l = new Link(null, null, null, null, null, null, null);
        l.position().setPixelPosition(64, 64);
        return l;
    }

    @Test
    void hinoxThrowDamagesAnAlreadyInvinciblePlayer() {
        var p = new PlayerState();
        p.vitals().setMaxHearts(3);
        p.vitals().setHealth(24);
        p.vitals().setInvincibilityCounter(16);
        var r = new linksawakening.testing.HinoxEffects()
                .sourceSlot(0)
                .heldX(0)
                .heldY(0)
                .heldZ(0)
                .speedX(0)
                .speedY(0)
                .velocityZ(0)
                .airborneState(0)
                .damage(4)
                .motionBlocked(false)
                .applyHeldLinkPose(false)
                .build();
        LinkWriteApplier.applyHinoxEffect(link(), p, r);
        assertEquals(4, p.vitals().subtractHealthBuffer());
        assertEquals(0x50, p.vitals().invincibilityCounter());
    }

    @Test
    void tarinAttackWritesClearAndRestartTheRealLinkCountdown() {
        var l = link();
        l.apply(LinkCommandAdapter.from(new LinkAttackStepRequest(9)));
        assertTrue(l.equipment().romAttackStepAnimationCountdown() > 0);
        l.apply(LinkCommandAdapter.from(new LinkAttackClearRequest(8)));
        assertEquals(0, l.equipment().romAttackStepAnimationCountdown());
    }

    @Test
    void hookshotPullMovesImmediatelyWithoutReplacingIgnoreCountdown() {
        var l = link();
        l.position().applyRomSpeed(0xF0, 0);
        l.status().setCollisionIgnoreFrames(10);
        l.position().captureRomFinalPosition();
        l.apply(LinkCommandAdapter.from(EntityProjectileEvent.hookshotPull(0, 16, 0)));
        assertEquals(65, l.position().pixelX());
        assertEquals(0, l.position().romSpeedX());
        l.motion().update();
        assertEquals(64, l.position().pixelX());
    }

    @Test
    void ordinaryProjectileResponseWritesRecoilInsteadOfPullingImmediately() {
        var l = link();
        var r = EntityProjectileEvent.builder(0, 1, EntityProjectileEvent.Kind.SHIELD_BLOCK)
                .collisionValue(0)
                .linkDamage(0)
                .soundChannel(EntityProjectileEvent.SoundChannel.NONE)
                .soundId(-1)
                .remove(false)
                .swordPokeVfx(false)
                .swordPokeX(0)
                .swordPokeY(0)
                .linkSpeedX(16)
                .linkSpeedY(0)
                .linkIgnoreCollisionCountdown(10)
                .build();
        l.apply(LinkCommandAdapter.from(r));
        assertEquals(64, l.position().pixelX());
        l.motion().update();
        assertEquals(65, l.position().pixelX());
        assertEquals(16, l.position().romSpeedX());
    }
}
