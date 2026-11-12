package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.GenieMotion;

/** Focused behavior using the state owned by GenieEntityController. */
final class GenieJarBehavior {
    static void advanceGenieJar(GenieEntityController owner, RoomEntity jarEntity, int linkEntityY) {
        owner.genieJarStep.run(owner, jarEntity, linkEntityY);
    }

    static RoomEntity registerGenieJarImpact(GenieEntityController owner, RoomEntity jar) {
        var runtime = owner.runtime;
        int slot = jar.slot();
        if (owner.genieImpactRegisteredThisFrame[slot]) {
            return jar;
        }
        owner.genieImpactRegisteredThisFrame[slot] = true;
        owner.geniePrivateState4[slot] = (owner.geniePrivateState4[slot] + 1) & 0xFF;
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0x20);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x07));
        if (owner.geniePrivateState4[slot] != GenieMotion.JAR_HEALTH_THRESHOLD) {
            return jar;
        }
        owner.genieActiveState[slot] = 1;
        owner.geniePrivateState3[slot] = 0;
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x80);
        runtime.motions().get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
        runtime.liftThrow().writeThrownMotionInitialized(slot, false);
        return withStatus(jar, EntityStatus.ACTIVE);
    }
}
