package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** Ordered combatoverlap operations for a single slot. */
final class EntityCombatOverlap {
    static boolean probeBodyAndSword(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.goombaStomp = runtime.roomInspection()
                .canGoombaStomp(
                        slot.entity,
                        frame.linkEntityX(),
                        frame.linkEntityY(),
                        frame.linkAirborne(),
                        frame.linkVerticalVelocity(),
                        frame.linkInteractive(),
                        frame.frameCounter());
        slot.linkCollision = slot.entity.type() != EntityRomConstants.ENTITY_LIKE_LIKE
                && !frame.linkAirborne()
                && frame.linkInteractive()
                && slot.booBuddyAllowsLinkCollision
                && RoomEntityCombatRules.collisionCadenceMatches(frame.frameCounter(), slot.entity.slot())
                && slot.peaHatGrounded
                && slot.hidingZolLinkCollision
                && RoomEntityCombatRules.overlapsLink(slot.entity, frame.linkEntityX(), frame.linkEntityY());
        slot.swordHit = frame.swordCollisionActive()
                && slot.booBuddyAllowsSwordCollision
                && !runtime.slotProperties().readWingedSwordAttackThisFrame(slot.entity.slot())
                && (slot.entity.type() != EntityRomConstants.ENTITY_LIKE_LIKE
                        || runtime.motions()
                                        .get(LikeLikeMotion.class, LikeLikeMotion::new)
                                        .state(slot.entity.slot())
                                == 0)
                && slot.hidingZolSwordCollision
                && runtime.backgroundMechanics()
                        .moldormOrHeadOverlapsSword(
                                slot.entity, frame.swordX(), frame.swordWidth(), frame.swordY(), frame.swordHeight());
        return true;
    }

    static boolean applyShieldContact(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // $20:4B4A supplies the shield rectangle to the same collision
        // pass as the sword, but $03:6EA5 dispatches it separately.
        if (slot.entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE
                && frame.linkInteractive()
                && frame.attackContext().usingShield()
                && !frame.swordCollisionActive()
                && runtime.combatTables().readEnemyIgnoreHitsCountdown(slot.entity.slot()) == 0
                && (runtime.combatTables().readEnemyFlashCountdown(slot.entity.slot()) == 0
                        || runtime.combatTables().readEnemyFlashCountdown(slot.entity.slot()) >= 0x18)) {
            int direction = frame.attackContext().romDirection();
            int shieldX = frame.attackContext().shieldX(frame.linkEntityX());
            int shieldY = frame.attackContext().shieldY(frame.linkEntityY());
            if (shieldX != 0
                    && RoomEntityCombatRules.overlapsSword(
                            slot.entity,
                            shieldX,
                            frame.attackContext().shieldWidth(),
                            shieldY,
                            frame.attackContext().shieldHeight())) {
                if (runtime.motions()
                                .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                                .state(slot.entity.slot())
                        != 3) {
                    runtime.motions()
                            .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                            .flipFromShield(slot.entity.slot(), direction);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot.entity.slot(), 0xFF);
                }
                // func_003_6F5C clears the ignore-hits countdown after
                // the shared shield bump; it does not damage the shell.
                runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0);
                runtime.motions()
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .clear(slot.entity.slot());
                EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        slot.entity.x(),
                        slot.entity.y(),
                        slot.entity.z(),
                        frame.linkEntityX(),
                        frame.linkEntityY(),
                        0x12);
                slot.events.add(linksawakening.world.entity.event.EntityCombatEvent.builder(
                                slot.entity.slot(), slot.entity.type())
                        .linkDamage(0)
                        .swordHit(false)
                        .enemyDamage(0)
                        .enemySpecialAction(-1)
                        .soundChannel(EntityCombatEvent.SoundChannel.JINGLE)
                        .soundId(0x09)
                        .secondarySoundChannel(EntityCombatEvent.SoundChannel.NONE)
                        .secondarySoundId(-1)
                        .swordPokeVfx(null)
                        .linkCollisionResponse(
                                new EntityCombatEvent.LinkCollisionResponse(vector.x() & 0xFF, vector.y() & 0xFF, 0x0C))
                        .build());
                return false;
            }
        }
        return true;
    }

    static boolean acceptContactOrStomp(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.moldormTailSwordHit = slot.swordHit
                && slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM
                && !RoomEntityCombatRules.overlapsSword(
                        slot.entity, frame.swordX(), frame.swordWidth(), frame.swordY(), frame.swordHeight());
        if (!slot.linkCollision && !slot.swordHit) {
            if (!slot.goombaStomp) {
                return false;
            }
        }

        if (slot.goombaStomp) {
            runtime.motions().get(GoombaMotion.class, GoombaMotion::new).enterStomp(slot.entity.slot());
            runtime.physicsTables().writeEnemyTransitionCountdown(slot.entity.slot(), 0x30);
            EntityCombatEvent.LinkAction linkAction = runtime.physicsTables().readGroundInteractionSideScrolling()
                    ? EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING
                    : EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN;
            slot.events.add(
                    linksawakening.world.entity.event.EntityCombatEvent.builder(slot.entity.slot(), slot.entity.type())
                            .linkDamage(0)
                            .swordHit(false)
                            .enemyDamage(0)
                            .enemySpecialAction(-1)
                            .soundChannel(EntityCombatEvent.SoundChannel.WAVE)
                            .soundId(0x0E)
                            .secondarySoundChannel(EntityCombatEvent.SoundChannel.NONE)
                            .secondarySoundId(-1)
                            .swordPokeVfx(null)
                            .linkAction(linkAction)
                            .build());
            return false;
        }
        return true;
    }
}
