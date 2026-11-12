package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySlotValidation.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.*;

/** Source-ordered preparation, descending slot phases and presentation commits. */
final class EntitySlotOrchestrator {
    static List<EntityProjectileEvent> advanceFrame(RoomEntityRuntime runtime, EntityFrameContext context) {
        EntityFrameExecution state = runtime.frameExecution.acquire(context);
        try {
            return advancePreparedFrame(state);
        } finally {
            state.release();
        }
    }

    private static List<EntityProjectileEvent> advancePreparedFrame(EntityFrameExecution state) {
        EntityFramePreparation.copyInputs(state);
        EntityFramePreparation.prepareInteractionGates(state);
        EntityFramePreparation.advanceRoomClocks(state);
        EntityFramePreparation.prepareBackgroundCollision(state);
        EntityFramePreparation.resetSlotAndLinkEffects(state);
        EntityFramePreparation.resetRewardsAndObjects(state);
        EntityFramePreparation.prepareLateFrameEvents(state);
        EntityFramePreparation.prepareFollowerState(state);

        for (state.slotFrame().index(state.runtime().slots.length - 1);
                state.slotFrame().index() >= 0;
                state.slotFrame().postIndex(-1)) {
            if (!EntitySlotPhase1.run(state)) continue;
            if (!EntitySlotPhase2.run(state)) continue;
            if (!EntitySlotPhase3.run(state)) continue;
            if (!EntitySlotPhase4.run(state)) continue;
            if (!EntitySlotPhase5.run(state)) continue;
        }
        if (state.gates().threeOfAKindInteractive()) {
            state.slotProperties().resolveThreeOfAKindPuzzle();
        }
        state.slotProperties()
                .updateHookshotChainOam(
                        state.inputs().linkEntityX(),
                        state.inputs().linkEntityY(),
                        state.inputs().frame());
        state.slotProperties().updatePincerBodyOam();
        state.slotProperties().updateWingedOctorokOam();
        state.slotProperties().updateBowWowChain(state.inputs().frame());
        return state.events().outbox.drain(EntityProjectileEvent.class, "projectileEvents");
    }
}
