package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityCombatResolver. */
final class EntityCombatPass {
    static List<EntityCombatEvent> resolveCombat(EntityCombatResolver owner, EnemyCombatInputs inputs) {
        var runtime = owner.runtime;
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        boolean linkAirborne = inputs.linkAirborne();
        int linkVerticalVelocity = inputs.linkVerticalVelocity();
        boolean linkInteractive = inputs.linkInteractive();
        boolean swordCollisionActive = inputs.swordCollisionActive();
        int swordX = inputs.swordX();
        int swordWidth = inputs.swordWidth();
        int swordY = inputs.swordY();
        int swordHeight = inputs.swordHeight();
        EnemyAttackContext attackContext = inputs.attackContext();
        EntitySlotValidation.validateByte(linkVerticalVelocity, "Link vertical velocity");
        owner.clearDeferredContactRecoil();
        if (attackContext == null) {
            throw new IllegalArgumentException("Enemy attack context cannot be null");
        }
        runtime.linkState().runningWithPegasusBoots = attackContext.pegasusBoots();
        List<EntityCombatEvent> events = runtime.events().outbox.channel(EntityCombatEvent.class, "resolvedCombat");
        events.clear();
        EntityCombatSlot slot = owner.acquireSlot();
        try {
            for (int index = runtime.slots().length - 1; index >= 0; index--) {
                slot.reset(inputs, events, runtime.slots()[index]);
                EntitySlotCombat.resolve(slot);
            }
        } finally {
            owner.releaseSlot(slot);
        }
        return runtime.events().outbox.drain(EntityCombatEvent.class, "resolvedCombat");
    }
}
