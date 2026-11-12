package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.room.ChestContentsTable;

/** Focused behavior using the state owned by EntityPickupController. */
final class ChestPickupBehavior {
    static void initializeChestEntity(EntityPickupController owner, RoomEntity entity) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        int itemType = owner.readChestItemBySlot(slot) & 0xFF;
        owner.writeChestSpeedY(slot, 0xFC);
        owner.writeChestSpeedYAccumulator(slot, 0);
        owner.writeChestInertia(slot, 0);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        slot,
                        EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        EntityRomConstants.CHEST_OPEN_NOISE_ID));
        if (itemType == ChestContentsTable.CHEST_TAIL_KEY) {
            for (RoomEntity candidate : runtime.slots()) {
                if (candidate.loaded() && candidate.type() == EntityRomConstants.ENTITY_OWL_EVENT) {
                    runtime.story().writeOwlPrivateCountdown1(candidate.slot(), 0x38);
                    break;
                }
            }
        }
        if (itemType < ChestContentsTable.CHEST_MESSAGE) {
            runtime.events().pendingChestRewardEvents.add(new ChestRewardEvent(slot, itemType));
        }
    }

    static boolean advanceChestEntity(EntityPickupController owner, int index, RoomEntity entity) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        int itemType = owner.readChestItemBySlot(slot) & 0xFF;
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            owner.spawnChestZol(entity);
            runtime.clearEntity(slot);
            return true;
        }

        // ChestWithItemEntityHandler ($07:7C5E) renders before this gate,
        // but does not move, advance inertia, or unload while text is open.
        if (runtime.linkState().dialogActive) {
            return false;
        }

        int y = addFallingSpeedToPosition(
                entity.y(), owner.readChestSpeedY(slot), owner.storageChestSpeedYAccumulator(), slot);
        int inertia = (owner.readChestInertia(slot) + 1) & 0xFF;
        owner.writeChestInertia(slot, inertia);
        if (inertia == 0x10) {
            owner.writeChestSpeedY(slot, 0);
        }
        if (inertia == 0x08 && runtime.chestContentsTable() != null) {
            int soundValue = runtime.chestContentsTable().presentationSoundValue(itemType);
            if (soundValue == EntityRomConstants.CHEST_TREASURE_JINGLE_ID) {
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_CHEST_WITH_ITEM,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                EntityRomConstants.CHEST_TREASURE_JINGLE_ID));
            } else if (soundValue != 0) {
                runtime.presentationState().writePendingMusicTrack(soundValue);
            }
        }
        if (inertia == 0x26 && runtime.chestContentsTable() != null) {
            int roomId = runtime.presentationState().readSpriteSelection() == null
                    ? -1
                    : runtime.presentationState().readSpriteSelection().roomId();
            int dialogLow = runtime.chestContentsTable()
                    .dialogLowIdFor(
                            itemType,
                            owner.readChestShieldLevel(),
                            owner.readChestSwordLevel(),
                            owner.readChestPowerBraceletLevel(),
                            runtime.roomVariables().readEntityMapId() < 0
                                    ? 0
                                    : runtime.roomVariables().readEntityMapId(),
                            roomId < 0 ? 0 : roomId);
            int tableId = itemType == ChestContentsTable.CHEST_MESSAGE && roomId == 0x96 ? 1 : 0;
            runtime.events().pendingDialogRequests.add(new DialogRequest(tableId, dialogLow));
        }
        if (inertia == 0x28) {
            runtime.clearEntity(slot);
            return true;
        }
        runtime.slots()[index] = withPositionAndVariant(entity, entity.x(), y, itemType);
        return false;
    }

    static void spawnChestZol(EntityPickupController owner, RoomEntity chest) {
        var runtime = owner.runtime;
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }
        EntitySpriteDefinition definition = runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_ZOL);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity zol = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_ZOL,
                chest.x(),
                chest.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0x06,
                -1,
                false);
        runtime.slots()[freeSlot] = zol;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).prepareChestSpawn(freeSlot);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_ZOL));
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_ZOL));
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        freeSlot,
                        EntityRomConstants.ENTITY_ZOL,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x1D));
    }
}
