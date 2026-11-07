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

/** Registered ROM type callbacks for ordered slot phase 5. */
final class EntityPhase5Handlers {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(RoomEntityRuntime.ENTITY_ROOSTER, 93, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                state.preserveRoosterPresentation = true;
                if (state.projectileLinkState.motionState() == 0
                    && (state.linkZ & 0xFF) == 0
                    && state.runtime.linkAttackStepAnimationCountdown == 0
                    && state.runtime.liftThrow.cuccoPowerBraceletHeld()
                    && RoomEntityCombatRules.overlapsLink(state.entity, state.linkEntityX, state.linkEntityY)
                    && state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
                    state.runtime.pendingRoosterLinkStateRequests.add(new RoosterLinkStateRequest(
                        state.entity.slot(), 0x02, 0x00, state.runtime.currentLinkSpeedX, state.runtime.currentLinkSpeedY,
                        state.romLinkDirection));
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.WAVE, RoosterMotion.LIFT_WAVE_SFX));
                    return false;
                }
            }
            return true;
        });
        for (int type = 0; type < 256; type++) {
            if ((ColorShellMotion.isColorShellType(type))) {
                registry.registerTick(type, 140, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                ColorShellMotion.Update shellUpdate = state.runtime.colorShellMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.backgroundCollision, state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                    state.runtime.colorShellWorld, Arrays.asList(state.runtime.slots));
                state.updated = shellUpdate.entity();
                state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = shellUpdate.nextIgnoreHitsCountdown();
                if (shellUpdate.unloadRequested()) {
                    state.runtime.pendingClearedEntityMask |= persistentClearMask(state.entity);
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
            }
                    return true;
                });
            }
        }
    }
}
