package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.Arrays;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.MotionTestFixture;

final class EntityDebugConfigurationFixture {
    private final RoomEntityRuntime runtime;

    EntityDebugConfigurationFixture(RoomEntityRuntime room) {
        runtime = room;
    }

    void setMagicPowderTransitionCountdownForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        if (!runtime.roomInspection.isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE)) {
            throw new IllegalArgumentException("Entity slot does not contain Magic Powder: " + slot);
        }
        runtime.entityState.physicsTables.writeEnemyTransitionCountdown(slot, value);
    }

    void setMagicPowderSlowTransitionCountdownForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        if (!runtime.roomInspection.isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE)) {
            throw new IllegalArgumentException("Entity slot does not contain Magic Powder: " + slot);
        }
        runtime.entityState.physicsTables.writeSlowTransitionCountdown(slot, value);
        runtime.entityState.physicsTables.writeSlowTimerInitialized(slot, true);
    }

    void setBombTransitionCountdownForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        if (!runtime.slots[slot].loaded() || runtime.slots[slot].type() != EntityRomConstants.ENTITY_BOMB) {
            throw new IllegalArgumentException("Entity slot does not contain a bomb: " + slot);
        }
        runtime.entityState.physicsTables.writeEnemyTransitionCountdown(slot, value);
    }

    void setBowWowTransitionCountdownForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        if (!runtime.roomInspection.isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_BOW_WOW)) {
            throw new IllegalArgumentException("Entity slot does not contain Bow-Wow: " + slot);
        }
        MotionTestFixture.setTransitionCountdownForTest(
                runtime.entityState.motions.get(BowWowMotion.class, BowWowMotion::new), slot, value);
    }

    void setEntityInertiaForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.entityState.physicsTables.writeEntityInertia(slot, value);
    }

    void setMoblinKingStateForTest(
            int slot, int state, int direction, int speedX, int speedY, int speedZ, int transitionCountdown) {
        EntitySlotValidation.validateCountdownTestValue(slot, transitionCountdown);
        MotionTestFixture.setStateForTest(
                runtime.entityState.motions.get(MoblinKingMotion.class, MoblinKingMotion::new),
                slot,
                state,
                direction,
                speedX,
                speedY,
                speedZ);
        runtime.entityState.physicsTables.writeEnemyTransitionCountdown(slot, transitionCountdown);
    }

    void setEntityPrivateState4ForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.entityState.physicsTables.writeEntityPrivateState4(slot, value);
    }

    void setLikeLikeLinkInventoryForTest(int itemA, int itemB) {
        runtime.configuration().setLikeLikeLinkInventory(itemA, itemB);
    }

    void setDroppedItemForTest(int slot, int itemType) {
        EntitySlotValidation.validateEntitySlot(slot);
        EntitySlotValidation.validateEnemyDropByte(itemType, "Dropped item");
        runtime.bossRewards.writeDroppedItemBySlot(slot, itemType);
    }

    void setEntityMapIdForTest(int mapId) {
        runtime.configuration().setEntityMapId(mapId);
    }

    void setEntityRoomIdForTest(int roomId) {
        runtime.configuration().setEntityRoomId(roomId);
    }

    void setEntityRoomStatusForTest(int roomStatus) {
        runtime.configuration().setEntityRoomStatus(roomStatus);
    }

    void setSecretSeashellPegasusCollisionForTest(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        runtime.configuration()
                .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
    }

    void setDroppablePegasusCollisionForTest(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        runtime.configuration()
                .setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY);
    }

    void setDroppableRevealedForTest(int slot, int slowCountdown) {
        EntitySlotValidation.validateCountdownTestValue(slot, slowCountdown);
        EntitySlotValidation.validateEntitySlot(slot);
        int type = runtime.slots[slot].type();
        if (!isCommonDroppableType(type)) {
            throw new IllegalArgumentException("Entity slot is not an ordinary droppable: " + slot);
        }
        runtime.bossRewards.writeDroppablePrivateState3(slot, 0);
        runtime.bossRewards.writeDroppablePrivateState4(slot, 0);
        runtime.entityState.physicsTables.writeEntityOptions1Override(
                slot, EntityRomConstants.DROPPABLE_REVEALED_OPTIONS1);
        runtime.entityState.physicsTables.writeSlowTransitionCountdown(slot, slowCountdown);
        runtime.entityState.physicsTables.writeSlowTimerInitialized(slot, true);
        runtime.bossRewards.writeEnemyDropActive(slot, false);
        runtime.entityState
                .motions
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .clear(slot);
    }

    void setSlowTransitionCountdownForTest(int slot, int countdown) {
        EntitySlotValidation.validateCountdownTestValue(slot, countdown);
        runtime.entityState.physicsTables.writeSlowTransitionCountdown(slot, countdown);
        runtime.entityState.physicsTables.writeSlowTimerInitialized(slot, true);
    }

    void setTransitionCountdownForTest(int slot, int countdown) {
        EntitySlotValidation.validateCountdownTestValue(slot, countdown);
        runtime.entityState.physicsTables.writeEnemyTransitionCountdown(slot, countdown);
    }

    void setThreeOfAKindSettledForTest(int slot, int face) {
        EntitySlotValidation.validateEntitySlot(slot);
        MotionTestFixture.setSettledForTest(
                runtime.entityState.motions.get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new), slot, face);
        runtime.entityState.physicsTables.writeEnemyTransitionCountdown(slot, 0);
    }

    void setHinoxStateForTest(int slot, int state) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (runtime.slots[slot].type() != EntityRomConstants.ENTITY_HINOX) {
            throw new IllegalArgumentException("Entity slot is not Hinox: " + slot);
        }
        MotionTestFixture.setStateForTest(
                runtime.entityState.motions.get(HinoxMotion.class, HinoxMotion::new), slot, state, 0, 0);
    }

    void setHinoxSpeedForTest(int slot, int speedX, int speedY) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (runtime.slots[slot].type() != EntityRomConstants.ENTITY_HINOX) {
            throw new IllegalArgumentException("Entity slot is not Hinox: " + slot);
        }
        MotionTestFixture.setSpeedForTest(
                runtime.entityState.motions.get(HinoxMotion.class, HinoxMotion::new), slot, speedX, speedY);
    }

    void setTarinRaccoonStateForTest(
            int slot,
            int state,
            int speedX,
            int speedY,
            int speedZ,
            int privateState2,
            int privateState3,
            boolean nearLinkLatch) {
        EntitySlotValidation.validateEntitySlot(slot);
        MotionTestFixture.setStateForTest(
                runtime.entityState.motions.get(TarinRaccoonMotion.class, TarinRaccoonMotion::new),
                slot,
                state,
                speedX,
                speedY,
                speedZ,
                privateState2,
                privateState3,
                nearLinkLatch);
    }

    void setTransitionSequenceCounterForTest(int counter) {
        if (counter < 0 || counter > 0xFF) {
            throw new IllegalArgumentException("Transition sequence counter must be an unsigned byte: " + counter);
        }
        runtime.entityState.roomVariables.writeTransitionSequenceCounter(counter);
    }

    void setGameplayWorldForTest(boolean gameplayWorld) {
        runtime.entityState.roomVariables.writeGameplayWorld(gameplayWorld);
    }

    void setRoomTransitionStateForTest(boolean active) {
        runtime.entityState.roomVariables.writeRoomTransitionActive(active);
    }

    void setWitchGotItemPresentationActiveForTest(boolean active) {
        Arrays.fill(
                runtime.entityState
                        .motions
                        .get(WitchMotion.class, WitchMotion::new)
                        .storageWitchGotItemCountdown(),
                active ? 1 : 0);
    }

    void setSideViewPotSpeedForTest(int slot, int speedX, int speedY) {
        runtime.entityState
                .motions
                .get(SideViewPotMotion.class, SideViewPotMotion::new)
                .setSpeedX(slot, speedX);
        runtime.entityState
                .motions
                .get(SideViewPotMotion.class, SideViewPotMotion::new)
                .setSpeedY(slot, speedY);
    }

    void setSwitchBlockAnimationActiveForTest(boolean active) {
        runtime.configuration().setSwitchBlockAnimationActive(active);
    }

    void setSideViewPlatformSpeedYForTest(int slot, int speed) {
        runtime.entityState
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .setSpeedY(slot, speed);
    }

    void setSideViewPlatformSpeedXForTest(int slot, int speed) {
        runtime.entityState
                .motions
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .setSpeedX(slot, speed);
    }

    void setLaserParentForTest(int slot, int countdown, int newSpeedX, int newSpeedY) {
        MotionTestFixture.setParentForTest(
                runtime.entityState.motions.get(LaserMotion.class, LaserMotion::new),
                slot,
                countdown,
                newSpeedX,
                newSpeedY);
    }

    void setPairoddProjectileForTest(int slot, int newSpeedX, int newSpeedY, int direction) {
        MotionTestFixture.setForTest(
                runtime.entityState.motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new),
                slot,
                newSpeedX,
                newSpeedY,
                direction);
    }

    void setLaserBeamForTest(int slot, int newSpeedX, int newSpeedY, int direction) {
        MotionTestFixture.setBeamForTest(
                runtime.entityState.motions.get(LaserMotion.class, LaserMotion::new),
                slot,
                newSpeedX,
                newSpeedY,
                direction);
    }

    void setZolPrivateCountdown1ForTest(int slot, int value) {
        EntitySlotValidation.validateByte(value, "Zol/Gel private countdown 1");
        MotionTestFixture.setPrivateCountdown1ForTest(
                runtime.entityState.motions.get(ZolGelMotion.class, ZolGelMotion::new), slot, value);
    }

    void setEvasivePrivateCountdown1ForTest(int slot, int value) {
        MotionTestFixture.setPrivateCountdown1ForTest(
                runtime.entityState.motions.get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new), slot, value);
    }

    void setEvasiveFleeingForTest(int slot, int newSpeedX, int newSpeedY) {
        MotionTestFixture.setFleeingForTest(
                runtime.entityState.motions.get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new),
                slot,
                newSpeedX,
                newSpeedY);
    }

    void setWizrobeStateForTest(
            int slot, int state, int transitionCountdown, int privateState1, int privateCountdown1, int direction) {
        EntitySlotValidation.validateEntitySlot(slot);
        EntitySlotValidation.validateCountdownTestValue(slot, transitionCountdown);
        MotionTestFixture.setForTest(
                runtime.entityState.motions.get(WizrobeMotion.class, WizrobeMotion::new),
                slot,
                state,
                transitionCountdown,
                privateState1,
                privateCountdown1,
                direction,
                runtime.slots[slot].spriteVariant());
        runtime.entityState.physicsTables.writeEnemyTransitionCountdown(slot, transitionCountdown);
        runtime.entityState.physicsTables.writeEnemyPhysicsFlags(slot, 0x42);
    }

    void setWizrobeProjectileForTest(int slot, int newSpeedX, int newSpeedY, int direction) {
        MotionTestFixture.setForTest(
                runtime.entityState.motions.get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new),
                slot,
                newSpeedX,
                newSpeedY,
                direction);
    }

    void setEnemyIgnoreHitsCountdownForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.entityState.combatTables.writeEnemyIgnoreHitsCountdown(slot, value);
    }

    void setOcarinaPlaybackForTest(int countdown, int songFlags, int selectedSong) {
        runtime.configuration().setOcarinaPlayback(countdown, songFlags, selectedSong);
    }

    void setOcarinaAnimationForTest(int animationCounter, int animationPhase) {
        EntitySlotValidation.validateByte(animationCounter, "Ocarina animation counter");
        if (animationPhase < 0 || animationPhase > 1) {
            throw new IllegalArgumentException("Ocarina animation phase must be 0 or 1");
        }
        runtime.entityState.linkState.ocarinaAnimationCounter = animationCounter;
        runtime.entityState.linkState.ocarinaAnimationPhase = animationPhase;
    }

    void setHitboxFlagsForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.entityState.combatTables.writeEnemyHitboxFlags(slot, value);
    }

    void setEnemyFlashCountdownForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.entityState.combatTables.writeEnemyFlashCountdown(slot, value);
    }

    void setEnemyHealthForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.entityState.combatTables.writeEnemyHealth(slot, value);
    }

    void setGeniePrivateState1ForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.genie.setGeniePrivateState1(slot, value);
    }

    void setGenieActiveStateForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.genie.setGenieActiveState(slot, value);
    }

    void setGeniePrivateState3ForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.genie.setGeniePrivateState3(slot, value);
    }

    void setGeniePrivateCountdown1ForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.genie.setGeniePrivateCountdown1(slot, value);
    }

    void setGeniePrivateState4ForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.genie.setGeniePrivateState4(slot, value);
    }

    void setPhysicsFlagsForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        runtime.entityState.physicsTables.writeEnemyPhysicsFlags(slot, value);
    }

    void setSpriteVariantForTest(int slot, int variant) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (variant < -1 || variant > 0xFF) {
            throw new IllegalArgumentException("Invalid sprite variant: " + variant);
        }
        runtime.slots[slot] = withVariant(runtime.slots[slot], variant);
    }

    void setThrownDirectionForTest(int slot, int value) {
        runtime.configuration().setThrownDirection(slot, value);
    }

    void setLedgeTransitionTimerForTest(int slot, int value) {
        runtime.configuration().setLedgeTransitionTimer(slot, value);
    }

    void setKeyDropTransitionCountdownForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        if (!runtime.roomInspection.isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_KEY_DROP_POINT)) {
            throw new IllegalArgumentException("Entity slot does not contain a key drop point: " + slot);
        }
        runtime.entityState.physicsTables.writeEnemyTransitionCountdown(slot, value);
    }

    void setHidingSlimeKeyTransitionCountdownForTest(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        if (!runtime.roomInspection.isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_HIDING_SLIME_KEY)) {
            throw new IllegalArgumentException("Entity slot does not contain a Hiding Slime Key: " + slot);
        }
        runtime.entityState.physicsTables.writeEnemyTransitionCountdown(slot, value);
    }

    void setGoldenLeavesCountForTest(int value) {
        runtime.configuration().setGoldenLeavesCount(value);
    }

    void setBooBuddyTriggerCountForTest(int value) {
        runtime.configuration().setBooBuddyTriggerCount(value);
    }

    void setColorShellStateForTest(
            int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) {
        MotionTestFixture.setStateForTest(
                runtime.entityState.motions.get(ColorShellMotion.class, ColorShellMotion::new),
                slot,
                state,
                transitionCountdown,
                direction,
                speedX,
                speedY);
    }

    void configureEnemyRecoilForTest(int slot, int linkEntityX, int linkEntityY, int length) {
        EntitySlotValidation.validateEntitySlot(slot);
        RoomEntity entity = runtime.slots[slot];
        if (!entity.loaded()) {
            throw new IllegalArgumentException("Entity slot is not loaded: " + slot);
        }
        runtime.entityState
                .motions
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .configure(slot, entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, length);
    }
}
