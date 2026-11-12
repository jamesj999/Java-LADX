package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.ZolGelMotion;

/** Slot clearing, status countdowns and persistence lifecycle. */
public final class EntitySlotLifecycle {
    final EntitySlotLifecycleServices runtime;

    EntitySlotLifecycle(EntitySlotLifecycleServices runtime) {
        this.runtime = runtime;
    }

    public int transitionSequenceCounter() {
        return runtime.roomVariables().readTransitionSequenceCounter();
    }

    EntityPickupEvent collectIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, 0, 0);
    }

    EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, 0);
    }

    EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ) {
        return collectIfNeeded(new EntityPickupFrame(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkInteractive,
                linkDirection,
                linkZ,
                false,
                0,
                0,
                0,
                0));
    }

    EntityPickupEvent collectIfNeeded(EntityPickupFrame inputs) {
        return new EntitySlotLifecycleFrameStep(this, inputs).run();
    }

    public int clearEntity(int slot) {
        return EntityPersistentSlotReset.clearEntity(runtime, slot);
    }

    public RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated, ZolGelMotion.Split split) {
        return ZolSplitController.applyZolSplit(runtime, original, updated, split);
    }

    void handleBowWowTargetContact(
            RoomEntity source, BowWowMotion.TargetContact contact, IntSupplier randomByteSupplier) {
        int targetSlot = contact.slot();
        if (targetSlot < 0 || targetSlot >= runtime.slots().length) {
            return;
        }
        RoomEntity target = runtime.slots()[targetSlot];
        if (!target.loaded()) {
            return;
        }

        if (target.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            if (runtime.physicsTables().readEntityPrivateState4(targetSlot) == 0
                    || runtime.linkState().dialogActive
                    || runtime.motions()
                                    .get(BowWowMotion.class, BowWowMotion::new)
                                    .privateCountdown2(source.slot())
                            != 0) {
                return;
            }
            runtime.motions().get(BowWowMotion.class, BowWowMotion::new).setTransitionCountdown(source.slot(), 0);
            runtime.motions().get(BowWowMotion.class, BowWowMotion::new).setPrivateCountdown2(source.slot(), 0x80);
            runtime.events().pendingDialogRequests.add(new DialogRequest(1, EntityRomConstants.DIALOG_BOW_WOW));
            return;
        }

        if (runtime.combatTables().readEnemyFlashCountdown(targetSlot) != 0) {
            return;
        }
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x03));
        if (target.type() == EntityRomConstants.ENTITY_KIKI_THE_MONKEY) {
            runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0x18);
            runtime.physicsTables()
                    .writeEntityInertia(targetSlot, (runtime.physicsTables().readEntityInertia(targetSlot) + 1) & 0xFF);
            return;
        }
        runtime.bossRewards().handleTerminalEnemyDeath(target, randomByteSupplier);
    }

    void finishBurning(RoomEntity entity) {
        int slot = entity.slot();
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
        if (entity.type() == EntityRomConstants.ENTITY_GIBDO) {
            EntitySpriteDefinition definition =
                    runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_STALFOS_EVASIVE);
            RoomEntity stalfos = withType(entity, EntityRomConstants.ENTITY_STALFOS_EVASIVE, definition);
            runtime.slots()[slot] = withStatus(stalfos, EntityStatus.ACTIVE);
            runtime.initializeHealthGroup(slot); // $03:4C89 reconfigures attributes/health.
            runtime.combatTables()
                    .writeEnemyHealth(slot, runtime.initialHealth(EntityRomConstants.ENTITY_STALFOS_EVASIVE));
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
            runtime.physicsTables().writeEntityOptions1Override(slot, -1);
            runtime.physicsTables().writeEnemyPhysicsFlags(slot, EntityRomConstants.EVASIVE_PHYSICS_FLAGS);
            runtime.combatTables().writeDyingCountdown(slot, 0);
            runtime.combatTables().writePowerRecoilDeath(slot, false);
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(slot);
            runtime.motions()
                    .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                    .clear(slot);
            return;
        }

        runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0x04);
        runtime.combatTables().writeDyingCountdown(slot, 0x1F);
        runtime.combatTables().writePowerRecoilDeath(slot, false);
        runtime.slots()[slot] = withDeathPresentation(withStatus(entity, EntityStatus.DYING), 3, false);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    void decrementSlowTransitionCountdown(int slot, int frameCounter) {
        if ((frameCounter & 0x03) == 0
                && runtime.physicsTables().readSlowTimerInitialized(slot)
                && runtime.physicsTables().readSlowTransitionCountdown(slot) > 0) {
            runtime.physicsTables().postSlowTransitionCountdown(slot, -1);
        }
    }

    boolean shouldDisappear(RoomEntity entity) {
        return runtime.enemyInspection().shouldDisappear(entity);
    }

    public void disableEntityWithoutPersistence(int slot) {
        EntityTransientSlotReset.disableEntityWithoutPersistence(runtime, slot);
    }

    boolean decrementEnemyCombatCountdowns(int slot, boolean decrementIgnoreHits) {
        if (runtime.combatTables().readEnemyFlashCountdown(slot) > 0) {
            runtime.combatTables().postEnemyFlashCountdown(slot, -1);
        }
        if (decrementIgnoreHits
                && !runtime.motions()
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .isActive(slot)
                && runtime.combatTables().readEnemyIgnoreHitsCountdown(slot) > 0) {
            runtime.combatTables().postEnemyIgnoreHitsCountdown(slot, -1);
            return true;
        }
        return false;
    }

    void decrementEnemyStatusCountdowns(int slot) {
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) > 0) {
            runtime.physicsTables().postEnemyTransitionCountdown(slot, -1);
        }
        if (runtime.combatTables().readEnemyStunnedCountdown(slot) > 0) {
            runtime.combatTables().postEnemyStunnedCountdown(slot, -1);
        }
        if (runtime.combatTables().readDyingCountdown(slot) > 0) {
            runtime.combatTables().postDyingCountdown(slot, -1);
        }
        if (runtime.bombInteractions().readBombPrivateCountdown1(slot) > 0) {
            runtime.bombInteractions().postBombPrivateCountdown1(slot, -1);
        }
        if (runtime.bombInteractions().readBombPrivateCountdown3(slot) > 0) {
            runtime.bombInteractions().postBombPrivateCountdown3(slot, -1);
        }
        if (runtime.story().owlState().readOwlPrivateCountdown1(slot) > 0
                && runtime.backgroundMechanics().entityTimersInteractive()) {
            runtime.story().owlState().postOwlPrivateCountdown1(slot, -1);
        }
        runtime.motions().get(HinoxMotion.class, HinoxMotion::new).decrementPrivateCountdown1(slot);
        if (runtime.liftThrow().readLiftableRockSmashActive(slot)
                && runtime.liftThrow().readLiftableRockSmashCountdown(slot) > 0) {
            runtime.liftThrow().postLiftableRockSmashCountdown(slot, -1);
        }
        runtime.motions().get(BlooperMotion.class, BlooperMotion::new).decrementPrivateCountdown3(slot);
        runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .decrementPrivateCountdown2(slot);
    }

    public void requestPersistentClearMask(int mask) {
        runtime.roomVariables()
                .writePendingClearedEntityMask(runtime.roomVariables().readPendingClearedEntityMask() | (mask));
    }

    public void delayInteractions(int countdown) {
        runtime.roomVariables().writeEntityInteractionCountdown(countdown);
    }

    public void recordGelClinging(boolean clinging) {
        runtime.motions()
                .get(ZolGelMotion.class, ZolGelMotion::new)
                .writeGelClingingToLink(runtime.motions()
                                .get(ZolGelMotion.class, ZolGelMotion::new)
                                .readGelClingingToLink()
                        | (clinging));
    }

    public void setDroppedItemBySlot(int slot, int value) {
        runtime.bossRewards().writeDroppedItemBySlot(slot, value);
    }

    public void setDyingCountdown(int slot, int value) {
        runtime.combatTables().writeDyingCountdown(slot, value);
    }

    public void setDropPrivateCountdown3(int slot, int value) {
        runtime.bossRewards().writeDropPrivateCountdown3(slot, value);
    }

    public int warpState(int slot) {
        return runtime.warpState().readWarpState(slot);
    }

    public void setWarpState(int slot, int value) {
        runtime.warpState().writeWarpState(slot, value);
    }

    public int warpTransitionCountdown(int slot) {
        return runtime.warpState().readWarpTransitionCountdown(slot);
    }

    public void setWarpTransitionCountdown(int slot, int value) {
        runtime.warpState().writeWarpTransitionCountdown(slot, value);
    }

    public int droppablePrivateState3(int slot) {
        return runtime.bossRewards().readDroppablePrivateState3(slot);
    }
}
