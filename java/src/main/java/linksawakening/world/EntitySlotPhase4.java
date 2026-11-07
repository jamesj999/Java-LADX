package linksawakening.world;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.RoomEntityRuntime.*;

/** Ordered slot dispatch phase 4; false preserves the ROM handler early return. */
final class EntitySlotPhase4 {
    static boolean run(EntityFrameExecution state) {

            if (!state.runtime.entityHandlers.tick(72, state)) return false;
            if (!state.runtime.entityHandlers.tick(73, state)) return false;
            if (!state.runtime.entityHandlers.tick(74, state)) return false;
            if (!state.runtime.entityHandlers.tick(75, state)) return false;
            if (!state.runtime.entityHandlers.tick(76, state)) return false;
            if (!state.runtime.entityHandlers.tick(77, state)) return false;
            if (!state.runtime.entityHandlers.tick(78, state)) return false;
            if (!state.runtime.entityHandlers.tick(79, state)) return false;
            if (!state.runtime.entityHandlers.tick(80, state)) return false;
            if (!state.runtime.entityHandlers.tick(81, state)) return false;
            if (!state.runtime.entityHandlers.tick(82, state)) return false;
            if (!state.runtime.entityHandlers.tick(83, state)) return false;
            if (!state.runtime.entityHandlers.tick(84, state)) return false;
            if (!state.runtime.entityHandlers.tick(85, state)) return false;
            if (!state.runtime.entityHandlers.tick(86, state)) return false;
            if (!state.runtime.entityHandlers.tick(138, state)) return false;
            if (!state.runtime.entityHandlers.tick(87, state)) return false;
            if (!state.runtime.entityHandlers.tick(88, state)) return false;
            if (!state.runtime.entityHandlers.tick(89, state)) return false;
            if (!state.runtime.entityHandlers.tick(139, state)) return false;
            if (!state.runtime.entityHandlers.tick(90, state)) return false;
            if (!state.runtime.entityHandlers.tick(91, state)) return false;
            if (!state.runtime.entityHandlers.tick(92, state)) return false;
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && (isDynamicFollowingNpc(state.entity)
                    || state.entity.type() == state.runtime.ENTITY_BOW_WOW)) {
                if (state.entity.type() == state.runtime.ENTITY_BOW_WOW) {
                    BowWowMotion.Update bowWowUpdate = state.runtime.bowWowMotion.advanceAfterTimers(
                        state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.runtime.followingLinkZ,
                        state.randomByteSupplier, state.backgroundCollision, Arrays.asList(state.runtime.slots),
                        state.runtime.enemyCombatTables == null
                            ? type -> false : state.runtime.enemyCombatTables::canBowWowEatEntity,
                        state.runtime.bowWowState);
                    state.updated = bowWowUpdate.entity();
                    if (bowWowUpdate.targetContact() != null) {
                        state.runtime.lifecycle.handleBowWowTargetContact(state.entity, bowWowUpdate.targetContact(),
                            state.randomByteSupplier);
                    }
                } else {
                    state.updated = state.runtime.followingNpcMotion.advance(state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                        state.runtime.followingLinkZ, state.runtime.followingLinkDirection, state.runtime.followingEntityYOffset,
                        state.runtime.followingLinkPositionHistory, state.backgroundCollision);
                }
            }
        return true;
    }
}
