package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** Ordered linkcontact operations for a single slot. */
final class EntityLinkContact {
    static boolean prepare(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        if (slot.linkCollision && slot.entity.type() == EntityRomConstants.ENTITY_GEL) {
            runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).onLinkCollision(slot.entity.slot());
        }
        // ApplyLinkCollisionWithEnemy's Gel branch ($03:6D3D) returns
        // immediately after starting the cling, before damage/recoil.
        slot.linkDamage = slot.linkCollision && slot.entity.type() != EntityRomConstants.ENTITY_GEL
                ? runtime.contactDamage(slot.entity.slot())
                : 0;
        if ((runtime.physicsTables().readEnemyPhysicsFlags(slot.entity.slot())
                        & EntityRomConstants.ENTITY_PHYSICS_HARMLESS)
                != 0) {
            // func_003_6CC0 keeps CheckLinkCollisionWithEnemy's carry
            // result for harmless entities but suppresses Link damage.
            slot.linkDamage = 0;
        }
        slot.linkCollisionResponse = EntityCombatEvent.LinkCollisionResponse.NONE;
        // AnimateRoamingEnemy calls the body collision before the sword
        // pass ($03:585B/$6E28). Hitting a plain roaming enemy does not
        // cancel the recoil that its body contact already wrote.
        slot.bodyRecoilBeforeSword = slot.entity.type() == EntityRomConstants.ENTITY_OCTOROK
                || slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN;
        return true;
    }

    static boolean applyRecoil(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        if (slot.linkDamage > 0
                && (!slot.swordHit || slot.bodyRecoilBeforeSword)
                && RoomEntityCombatRules.usesStandardLinkContactResponse(slot.entity.type())) {
            if (slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR) {
                // $03:6DE9-6DED / $03:6FA7-6FB8: horizontal only.
                int speedX = ((frame.linkEntityX() - slot.entity.x()) & 0xFF) < 0x80 ? 0x18 : 0xE8;
                slot.linkCollisionResponse = new EntityCombatEvent.LinkCollisionResponse(speedX, 0, 0x10);
            } else {
                // $03:6DFA-6E02 selects the magnitude of Link's recoil.
                int speed = slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM ? 0x18 : 0x14;
                EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        slot.entity.x(),
                        slot.entity.y(),
                        slot.entity.z(),
                        frame.linkEntityX(),
                        frame.linkEntityY(),
                        speed);
                slot.linkCollisionResponse =
                        new EntityCombatEvent.LinkCollisionResponse(vector.x() & 0xFF, vector.y() & 0xFF, 0x10);
            }
        }
        if (slot.linkDamage > 0
                && !slot.swordHit
                && slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING
                && runtime.motions()
                                .get(MoblinKingMotion.class, MoblinKingMotion::new)
                                .state(slot.entity.slot())
                        == 9
                && runtime.physicsTables().readEnemyTransitionCountdown(slot.entity.slot()) == 0x60) {
            slot.linkCollisionResponse = new EntityCombatEvent.LinkCollisionResponse(
                    runtime.motions()
                            .get(MoblinKingMotion.class, MoblinKingMotion::new)
                            .speedX(slot.entity.slot()),
                    0,
                    0x28,
                    0x40);
        }
        return true;
    }

    static boolean emitOutcome(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.events.add(
                linksawakening.world.entity.event.EntityCombatEvent.builder(slot.entity.slot(), slot.entity.type())
                        .linkDamage(slot.linkDamage)
                        .swordHit(slot.swordHit)
                        .enemyDamage(slot.enemyDamage)
                        .enemySpecialAction(slot.enemySpecialAction)
                        .soundChannel(slot.soundChannel)
                        .soundId(slot.soundId)
                        .secondarySoundChannel(slot.secondarySoundChannel)
                        .secondarySoundId(slot.secondarySoundId)
                        .swordPokeVfx(slot.swordPokeVfx)
                        .linkCollisionResponse(slot.linkCollisionResponse)
                        .build());
        return true;
    }
}
