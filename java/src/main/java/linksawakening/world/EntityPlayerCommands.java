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

/** Player lift, throw, block movement and held-item presentation commands. */
final class EntityPlayerCommands {
    private final RoomEntityRuntime runtime;
    EntityPlayerCommands(RoomEntityRuntime runtime) { this.runtime = runtime; }

    boolean beginLift(int slot, int romDirection) {
        EntitySlotValidation.validateEntitySlot(slot);
        EntitySlotValidation.validateRomDirection(romDirection);
        RoomEntity entity = runtime.slots[slot];
        if (!entity.loaded()) {
            return false;
        }
        if (entity.type() == runtime.ENTITY_BOMB && runtime.bombFinalPresentationPending[slot]) {
            return false;
        }
        if (runtime.liftedEntitySlot >= 0 && runtime.liftedEntitySlot != slot
            && runtime.slots[runtime.liftedEntitySlot].status() == EntityStatus.LIFTED) {
            return false;
        }

        runtime.liftedEntitySlot = slot;
        runtime.liftedPhase[slot] = 0;
        runtime.liftedSourceDirection[slot] = romDirection;
        runtime.liftedStateInitialized[slot] = true;
        runtime.liftedCarryState = 0;
        runtime.liftedEffectiveDirection = romDirection;
        runtime.enemyTransitionCountdown[slot] = 0x02;
        if (entity.type() == runtime.ENTITY_BOMB) {
            if (runtime.placedBombMotionInitialized[slot]) {
                runtime.thrownEntityMotion.clear(slot);
                runtime.placedBombMotionInitialized[slot] = false;
            }
            runtime.bombFinalPresentationPending[slot] = false;
            runtime.enemyFlashCountdown[slot] = 0;
            entity = withDefinition(entity, runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_BOMB), 0);
        }
        runtime.slots[slot] = withStatus(entity, EntityStatus.LIFTED);
        return true;
    }

    boolean throwLiftedEntity(int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        LiftedEntityState state = runtime.liftedEntityState();
        if (!state.active() || state.carryState() != 0x01) {
            return false;
        }

        int slot = state.slot();
        RoomEntity entity = runtime.slots[slot];
        runtime.pendingPreTickLinkAttackStepRequests.add(new LinkAttackStepRequest(slot));
        runtime.pendingPreTickEntityEvents.add(new EntityCombatEvent(slot, entity.type(), 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, runtime.LIFTED_THROW_JINGLE_ID));
        if (entity.type() == runtime.ENTITY_SIDE_VIEW_POT) {
            runtime.thrownDirection[slot] = romDirection;
            runtime.sideViewPotMotion.start(slot, romDirection);
            runtime.thrownEntityMotion.clear(slot);
            runtime.thrownMotionInitialized[slot] = false;
            runtime.liftedStateInitialized[slot] = false;
            runtime.liftedPhase[slot] = 0;
            runtime.liftedCarryState = 0;
            runtime.liftedEffectiveDirection = 0;
            runtime.liftedEntitySlot = -1;
            runtime.enemyTransitionCountdown[slot] = 0;
            runtime.slots[slot] = withStatus(entity, EntityStatus.ACTIVE);
            return true;
        }
        runtime.thrownDirection[slot] = romDirection;
        runtime.thrownEntityMotion.start(slot, romDirection, entity.type(),
            runtime.groundInteractionSideScrolling);
        runtime.thrownMotionInitialized[slot] = true;
        runtime.liftedStateInitialized[slot] = false;
        runtime.liftedPhase[slot] = 0;
        runtime.liftedCarryState = 0;
        runtime.liftedEffectiveDirection = 0;
        runtime.liftedEntitySlot = -1;
        runtime.enemyTransitionCountdown[slot] = entity.type() == runtime.ENTITY_BOMB
            ? BombMotion.INITIAL_COUNTDOWN : 0;
        runtime.slots[slot] = withStatus(entity, EntityStatus.THROWN);
        return true;
    }

    void movePushedBlock(int slot, int x, int y) {
        if (slot < 0 || slot >= runtime.slots.length
            || runtime.slots[slot].type() != runtime.ENTITY_PUSHED_BLOCK
            || runtime.slots[slot].status() == EntityStatus.DISABLED) {
            return;
        }
        RoomEntity entity = runtime.slots[slot];
        runtime.slots[slot] = new RoomEntity(slot, entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z(), -1, false);
    }

    void removePushedBlock(int slot) {
        if (slot >= 0 && slot < runtime.slots.length && runtime.slots[slot].type() == runtime.ENTITY_PUSHED_BLOCK) {
            runtime.slots[slot] = RoomEntity.disabled(slot);
        }
    }

    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        for (int slot = 0; slot < runtime.slots.length; slot++) {
            RoomEntity entity = runtime.slots[slot];
            if (entity.loaded() && entity.type() == 0x3E) {
                runtime.slots[slot] = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
                    x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), spriteVariant,
                    entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
                    entity.deathSpriteVariant(), entity.powerRecoilDeath());
                return true;
            }
        }
        return false;
    }

    void updateHouseTarinPresentation(int linkX, int linkY) {
        for (int slot = 0; slot < runtime.slots.length; slot++) {
            RoomEntity entity = runtime.slots[slot];
            if (entity.loaded() && entity.type() == runtime.ENTITY_TARIN) {
                int dx = (byte) (linkX - entity.x());
                int dy = (byte) (linkY - entity.y());
                int direction = Math.abs(dy) >= Math.abs(dx) ? (dy < 0 ? 2 : 3) : (dx < 0 ? 1 : 0);
                runtime.slots[slot] = withVariant(entity, direction);
            }
        }
    }

    void startSwordPickup(int slot) {
        runtime.swordPickupSequenceActive[slot] = true;
        runtime.swordPickupState[slot] = 0;
        runtime.enemyTransitionCountdown[slot] = 0xA0;
        runtime.slowTimerInitialized[slot] = true;
        runtime.pendingMusicTrack = 0x0F;
    }

    void startToadstoolPickup(int slot) {
        runtime.toadstoolPickupSequenceActive[slot] = true;
        runtime.enemyTransitionCountdown[slot] = 0x68;
        runtime.pendingMusicTrack = 0x10;
    }

    void presentWitchGotItem(int slot) {
        runtime.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
    }

    void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY,
                                    int linkZ, boolean finalPose) {
        int slot = entity.slot();
        runtime.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        int x = finalPose ? (linkEntityX - 4) & 0xFF : linkEntityX;
        RoomEntity held = withPositionAndVariant(entity, x,
            (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots[slot] = withZ(held, linkZ & 0xFF);
    }
}
