package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ThreeOfAKindMotion;

/** Focused behavior using the state owned by EntitySlotProperties. */
final class EntityCompositePresentation {
    static void resolveThreeOfAKindPuzzle(EntitySlotProperties owner) {
        var runtime = owner.runtime;
        ThreeOfAKindMotion.PuzzleResult result = runtime.motions()
                .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                .resolvePuzzle(Arrays.asList(runtime.slots()), runtime.physicsTables()::readEnemyTransitionCountdown);
        if (result == ThreeOfAKindMotion.PuzzleResult.NONE) {
            return;
        }
        int coordinatorSlot = Arrays.stream(runtime.slots())
                .filter(entity -> entity.status() == EntityStatus.ACTIVE
                        && entity.type() == EntityRomConstants.ENTITY_THREE_OF_A_KIND)
                .mapToInt(RoomEntity::slot)
                .findFirst()
                .orElse(0);
        int jingle = result == ThreeOfAKindMotion.PuzzleResult.MATCH
                ? runtime.motions()
                        .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                        .matchedJingle(coordinatorSlot)
                : 0x1D;
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        coordinatorSlot,
                        EntityRomConstants.ENTITY_THREE_OF_A_KIND,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        jingle));
        if (result != ThreeOfAKindMotion.PuzzleResult.MATCH) {
            return;
        }
        for (int index = 0; index < runtime.slots().length; index++) {
            RoomEntity entity = runtime.slots()[index];
            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_THREE_OF_A_KIND) {
                continue;
            }
            int slot = entity.slot();
            runtime.bossRewards()
                    .writeDroppedItemBySlot(
                            slot,
                            runtime.motions()
                                    .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                                    .matchedDrop(slot));
            runtime.combatTables().writeDyingCountdown(slot, 0x1F);
            runtime.combatTables().writeEnemyHealth(slot, 0);
            runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0x04);
            runtime.slots()[index] = withStatus(entity, EntityStatus.DYING);
        }
        // The source writes the shared noise register once per card in the
        // same handler pass; only the final register value is audible.
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        coordinatorSlot,
                        EntityRomConstants.ENTITY_THREE_OF_A_KIND,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x13));
    }

    static void updateBowWowChain(EntitySlotProperties owner, int frame) {
        var runtime = owner.runtime;
        List<BowWowChain.Entry> entries = new ArrayList<>();
        // The handler's dialogue/inventory/transition branches still run the
        // chain. Its sequence check only returns early on the unpaused path.
        if (runtime.roomVariables().readTransitionSequenceCounter() != 4
                && !runtime.roomVariables().readRoomTransitionActive()
                && !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !runtime.inspection().witchGotItemPresentationActive()) {
            owner.writeBowWowChainOam(List.of());
            return;
        }
        boolean moving = (runtime.linkState().currentLinkSpeedX
                        | runtime.linkState().currentLinkSpeedY
                        | runtime.linkState().currentLinkVelocityZ)
                != 0;
        for (int slot = 0; slot < runtime.slots().length; slot++) {
            RoomEntity entity = runtime.slots()[slot];
            if (entity.type() != EntityRomConstants.ENTITY_BOW_WOW || entity.status() != EntityStatus.ACTIVE) continue;
            runtime.slots()[slot] = runtime.motions()
                    .get(BowWowMotion.class, BowWowMotion::new)
                    .advanceChain(
                            entity,
                            runtime.motions()
                                    .get(BowWowMotion.class, BowWowMotion::new)
                                    .readBowWowState(),
                            moving);
            entries.addAll(runtime.motions()
                    .get(BowWowMotion.class, BowWowMotion::new)
                    .chainEntries(
                            slot,
                            runtime.motions()
                                    .get(BowWowMotion.class, BowWowMotion::new)
                                    .readBowWowState(),
                            frame));
        }
        owner.writeBowWowChainOam(List.copyOf(entries));
    }
}
