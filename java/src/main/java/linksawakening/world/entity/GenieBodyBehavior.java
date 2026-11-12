package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GenieMotion;

/** Focused behavior using the state owned by GenieEntityController. */
final class GenieBodyBehavior {
    static void advanceGenieAppearance(GenieEntityController owner, RoomEntity appearance) {
        var runtime = owner.runtime;
        int slot = appearance.slot();
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        if ((countdown & 0x80) != 0) {
            if ((countdown & 0x7F) == 0) {
                if (owner.genieControllerSlot >= 0
                        && owner.genieControllerSlot < runtime.slots().length
                        && runtime.slots()[owner.genieControllerSlot].loaded()
                        && runtime.slots()[owner.genieControllerSlot].type() == EntityRomConstants.ENTITY_GENIE) {
                    owner.genieActiveState[owner.genieControllerSlot] = 3;
                }
                runtime.lifecycle().disableEntityWithoutPersistence(slot);
            }
            return;
        }
        if (countdown == 0) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        if (countdown == 0x06) {
            owner.spawnGenieEntity(
                    appearance.x(), Math.max(appearance.y(), 0x14), 1, 0x40, GenieMotion.INITIAL_HEALTH, 0x01, 0x8C);
        }
    }

    static void advanceGenieBody(
            GenieEntityController owner,
            RoomEntity body,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier) {
        owner.genieBodyStep.run(owner, body, frameCounter, linkEntityX, linkEntityY, randomByteSupplier);
    }

    static int spawnGenieAimedFireball(
            GenieEntityController owner, RoomEntity body, int bodySlot, int linkEntityX, int linkEntityY) {
        var runtime = owner.runtime;
        int xOffset = (runtime.physicsTables().readEntityInertia(bodySlot) & 1) == 0 ? -0x0C : 0x0C;
        int fireballSlot = owner.spawnGenieEntity(
                body.x() + xOffset, body.y(), 4, 0, GenieMotion.INITIAL_HEALTH, 0x12, GenieMotion.INITIAL_HITBOX_FLAGS);
        if (fireballSlot < 0) {
            return -1;
        }
        runtime.physicsTables()
                .writeEntityInertia(bodySlot, (runtime.physicsTables().readEntityInertia(bodySlot) + 1) & 0xFF);
        owner.genieSpeedZ[fireballSlot] = 0x24;
        EnemyRecoilMotion.Vector towardLink = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                body.x() + xOffset, body.y(), 0, linkEntityX, linkEntityY, 0x1F);
        owner.genieSpeedX[fireballSlot] = towardLink.x() & 0xFF;
        owner.genieSpeedY[fireballSlot] = towardLink.y() & 0xFF;
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        bodySlot,
                        EntityRomConstants.ENTITY_GENIE,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x28));
        return fireballSlot;
    }
}
