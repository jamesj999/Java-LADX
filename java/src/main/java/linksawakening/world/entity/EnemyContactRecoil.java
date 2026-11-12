package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** Focused behavior using the state owned by EntityCombatResolver. */
final class EnemyContactRecoil {
    static void finishLeeverContactRecoil(EntityCombatResolver owner, RoomEntity moved) {
        var runtime = owner.runtime;
        int slot = moved.slot();
        if (!owner.deferredLeeverContact[slot]) return;
        owner.deferredLeeverContact[slot] = false;
        // $04:7F01 moves before $7F49 calls the shared collision handler.
        // Its hitbox uses cached hActiveEntityPosX/Y, but $03:7565 reads
        // the updated position tables when selecting Link's recoil vector.
        var vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                moved.x(), moved.y(), moved.z(), owner.contactLinkX[slot], owner.contactLinkY[slot], 0x14);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        slot,
                        moved.type(),
                        0,
                        false,
                        0,
                        -1,
                        EntityCombatEvent.SoundChannel.NONE,
                        -1,
                        EntityCombatEvent.SoundChannel.NONE,
                        -1,
                        null,
                        EntityCombatEvent.LinkAction.NONE,
                        new EntityCombatEvent.LinkCollisionResponse(vector.x() & 255, vector.y() & 255, 0x10)));
    }
}
