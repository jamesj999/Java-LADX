package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import java.util.Objects;
import java.util.function.IntUnaryOperator;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.FollowingNpcMotion;
import linksawakening.world.entity.motion.SecretSeashellMotion;

/** Room entity configuration preserving source-shaped handler inputs. */
public final class EntityRuntimeConfiguration {
    // The ROM-free source retains the supplied sword-pickup definition.
    private final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    private final EntityBossState bossRewards;
    private final EntityLiftState liftThrow;
    private final EntityLinkState linkState;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;
    private final EntityPickupState pickups;
    private final EntityPresentationState presentationState;
    private final EntityProjectileState projectiles;
    private final EntityRoomVariables roomVariables;
    private final EntityStoryState story;
    private final EntitySlots slots;
    private final SlimeEyeController slimeEye;
    private final EntitySpriteViews spriteViews;

    EntityRuntimeConfiguration(
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBossState bossRewards,
            EntityLiftState liftThrow,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityProjectileState projectiles,
            EntityRoomVariables roomVariables,
            EntityStoryState story,
            EntitySlots slots,
            SlimeEyeController slimeEye) {
        EntityPhysicsTables physicsTables = slots.physics();
        EntitySpriteViews spriteViews = spriteHandlers;
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.liftThrow = Objects.requireNonNull(liftThrow, "liftThrow");
        this.linkState = Objects.requireNonNull(linkState, "linkState");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        this.presentationState = Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = Objects.requireNonNull(projectiles, "projectiles");
        this.roomVariables = Objects.requireNonNull(roomVariables, "roomVariables");
        this.story = Objects.requireNonNull(story, "story");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.slimeEye = Objects.requireNonNull(slimeEye, "slimeEye");
        this.spriteViews = Objects.requireNonNull(spriteViews, "spriteViews");
    }

    public void setBoomerangSoundCounterForFrame(int counter) {
        if (counter < 0 || counter >= EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD) {
            throw new IllegalArgumentException("Boomerang sound counter out of range: " + counter);
        }
        this.projectiles.writeBoomerangSfxCounter(counter);
        this.projectiles.writeBoomerangSoundClockProvided(true);
    }

    public void setTorchPaletteTransitionState(int targetAddress, boolean active) {
        EntitySlotValidation.validateByte(targetAddress, "Torch palette target");
        this.projectiles.writeTorchPaletteTargetAddress(targetAddress);
        this.projectiles.writeTorchPaletteTransitionActive(active);
    }

    public void setLinkVelocityZ(int velocity) {
        this.linkState.writeCurrentLinkVelocityZ(velocity & 0xFF);
    }

    public void setSpriteSelection(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    selection) {
        this.presentationState.writeSpriteSelection(selection);
    }

    public void setGroundInteraction(RoomEntityGroundInteraction groundInteraction) {
        this.physicsTables.writeGroundInteraction(groundInteraction);
    }

    public void setBackgroundInteraction(RoomEntityBackgroundInteraction backgroundInteraction) {
        this.physicsTables.writeBackgroundInteraction(backgroundInteraction);
    }

    public void setObjectQuery(RoomEntityObjectQuery objectQuery) {
        this.physicsTables.writeObjectQuery(objectQuery);
    }

    public void setObjectIntersectionQuery(RoomEntityObjectQuery objectIntersectionQuery) {
        this.physicsTables.writeObjectIntersectionQuery(objectIntersectionQuery);
    }

    public void setActionButtonsHeld(boolean actionButtonsHeld) {
        setActionButtonsHeld(actionButtonsHeld, actionButtonsHeld);
    }

    public void setActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        this.linkState.writeActionButtonAHeld(actionButtonAHeld);
        this.linkState.writeActionButtonBHeld(actionButtonBHeld);
        this.linkState.writeActionButtonsHeld(actionButtonAHeld || actionButtonBHeld);
    }

    public void setJoypadHeld(boolean joypadHeld) {
        this.linkState.writeJoypadHeld(joypadHeld);
    }

    public void setPressedButtonsMask(int pressedButtonsMask) {
        EntitySlotValidation.validateByte(pressedButtonsMask, "Pressed buttons mask");
        this.linkState.writeLinkPressedButtonsMask(pressedButtonsMask);
    }

    public void setDialogActive(boolean dialogActive) {
        this.linkState.writeDialogActive(dialogActive);
    }

    public void setTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        EntitySlotValidation.validateByte(dialogCooldown, "Dialog cooldown");
        EntitySlotValidation.validateByte(windowY, "Window Y");
        this.linkState.writeInventoryAppearing(inventoryAppearing);
        this.linkState.writeDialogCooldown(dialogCooldown);
        this.linkState.writeWindowY(windowY);
    }

    public void setDialogAskSelectionIndex(int selectionIndex) {
        if (selectionIndex < 0 || selectionIndex > 1) {
            throw new IllegalArgumentException("Dialog selection must be 0 or 1");
        }
        this.linkState.writeDialogAskSelectionIndex(selectionIndex);
    }

    public void setActiveMusic(boolean activeMusic) {
        this.presentationState.writeActiveMusic(activeMusic);
    }

    public void setBowWowState(int bowWowState) {
        var bowWowMotion = this.motions.get(BowWowMotion.class, BowWowMotion::new);
        bowWowMotion.writeBowWowState(bowWowState & 0xFF);
    }

    public void setLikeLikeLinkInventory(int itemA, int itemB) {
        EntitySlotValidation.validateByte(itemA, "Link A inventory slot");
        EntitySlotValidation.validateByte(itemB, "Link B inventory slot");
        this.linkState.writeLinkItemA(itemA);
        this.linkState.writeLinkItemB(itemB);
        this.linkState.writeLinkInventoryConfigured(true);
    }

    public void setPowerBraceletButtonHeld(boolean powerBraceletButtonHeld) {
        this.linkState.writePowerBraceletButtonHeld(powerBraceletButtonHeld);
    }

    public void setBombButtonHeld(boolean bombButtonHeld) {
        this.linkState.writeBombButtonHeld(bombButtonHeld);
    }

    public void setLinkAttackStepAnimationCountdown(int countdown) {
        EntitySlotValidation.validateByte(countdown, "Link attack-step animation countdown");
        this.linkState.writeLinkAttackStepAnimationCountdown(countdown);
    }

    public void setLinkFinalPositionX(int positionX) {
        EntitySlotValidation.validateByte(positionX, "Link final position X");
        this.linkState.writeCurrentLinkFinalPositionX(positionX);
        this.linkState.writeCurrentLinkFinalPositionXConfigured(true);
    }

    public void setLiftStrengthBoost(boolean redTunic, boolean pieceOfPower) {
        this.linkState.writeFastLiftTransition(redTunic || pieceOfPower);
    }

    public void setLiftedLinkC13B(int linkC13B) {
        if ((linkC13B & ~0xFF) != 0) {
            throw new IllegalArgumentException("Lifted Link C13B must be an unsigned byte: " + linkC13B);
        }
        this.liftThrow.writeLiftedLinkC13B(linkC13B);
    }

    public void setGroundInteractionSideScrolling(boolean sideScrolling) {
        this.physicsTables.writeGroundInteractionSideScrolling(sideScrolling);
    }

    public void setEnemyDropResolver(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EnemyDropResolver
                    resolver) {
        this.bossRewards.writeEnemyDropResolver(resolver);
    }

    public void setEnemyDropCounters(EnemyDropResolver.CounterState counters) {
        this.bossRewards.writeEnemyDropCounters(Objects.requireNonNull(counters, "Drop counters cannot be null"));
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        EntitySlotValidation.validateEnemyDropByte(maxHearts, "Maximum hearts");
        EntitySlotValidation.validateEnemyDropByte(health, "Health");
        this.bossRewards.writeEnemyDropMaxHearts(maxHearts);
        this.bossRewards.writeEnemyDropHealth(health);
        this.bossRewards.writeEnemyDropLowHealth(null);
        this.bossRewards.writeEnemyDropActivePowerUp(activePowerUp);
    }

    public void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp, boolean lowHealth) {
        setEnemyDropPlayerState(maxHearts, health, activePowerUp);
        this.bossRewards.writeEnemyDropLowHealth(lowHealth);
    }

    public void setEntityMapId(int mapId) {
        if (mapId < -1 || mapId > 0xFF) {
            throw new IllegalArgumentException("Entity map id must be -1 or an unsigned byte: " + mapId);
        }
        this.roomVariables.writeEntityMapId(mapId);
    }

    public void setEntityRoomId(int roomId) {
        EntitySlotValidation.validateByte(roomId, "Entity room id");
        this.roomVariables.writeEntityRoomId(roomId);
        for (int entitySlotIndex = 0; entitySlotIndex < this.slots.size(); entitySlotIndex++) {
            RoomEntity entity = this.slots.get(entitySlotIndex);

            if (!entity.loaded() || entity.type() != EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) continue;
            SecretSeashellMotion seashell = this.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
            seashell.ensureInitialized(entity.slot(), roomId);
            boolean hidden = seashell.privateState3(entity.slot()) != 0;
            this.physicsTables.writeEntityOptions1Override(
                    entity.slot(),
                    hidden
                            ? EntityRomConstants.SECRET_SEASHELL_HIDDEN_OPTIONS1
                            : EntityRomConstants.SECRET_SEASHELL_REVEALED_OPTIONS1);
        }
    }

    public void setEntityRoomStatus(int roomStatus) {
        EntitySlotValidation.validateByte(roomStatus, "Entity room status");
        this.roomVariables.writeEntityRoomStatus(roomStatus);
    }

    public void setDungeonMinibossReady(boolean ready) {
        this.roomVariables.writeDungeonMinibossReady(ready);
    }

    public void setScreenShakeCountdown(int countdown) {
        this.slimeEye.setScreenShakeCountdown(countdown);
    }

    public void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) {
        EntitySlotValidation.validateByte(collisionX, "Pegasus collision X");
        EntitySlotValidation.validateByte(collisionY, "Pegasus collision Y");
        var secretSeashellMotion = this.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        secretSeashellMotion.writeSecretSeashellScreenShakeActive(screenShakeActive);
        secretSeashellMotion.writeSecretSeashellPegasusCollisionActive(collisionActive);
        secretSeashellMotion.writeSecretSeashellPegasusCollisionCoordinatesKnown(true);
        secretSeashellMotion.writeSecretSeashellPegasusCollisionX(collisionX);
        secretSeashellMotion.writeSecretSeashellPegasusCollisionY(collisionY);
    }

    public void setHouseNpcStoryState(int tarinFlag, int tradeItem, int instrument3Flags, int walrusStatus) {
        EntitySlotValidation.validateByte(tarinFlag, "Tarin flag");
        EntitySlotValidation.validateByte(tradeItem, "Trade item");
        EntitySlotValidation.validateByte(instrument3Flags, "Instrument 3 flags");
        EntitySlotValidation.validateByte(walrusStatus, "Walrus status");
        this.story.writeHouseTarinFlag(tarinFlag);
        this.story.writeHouseTradeItem(tradeItem);
        this.story.writeHouseInstrument3Flags(instrument3Flags);
        this.story.writeHouseWalrusStatus(walrusStatus);
    }

    public void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        EntitySlotValidation.validateByte(shieldLevel, "Shield level");
        EntitySlotValidation.validateByte(swordLevel, "Sword level");
        EntitySlotValidation.validateByte(powerBraceletLevel, "Power Bracelet level");
        this.pickups.writeChestShieldLevel(shieldLevel);
        this.pickups.writeChestSwordLevel(swordLevel);
        this.pickups.writeChestPowerBraceletLevel(powerBraceletLevel);
        this.pickups.writeChestPlayerLevelsKnown(true);
        if (this.spriteHandlers.supportsPresentation()) {
            EntitySpriteDefinition pickupDefinition = this.spriteViews.forSwordShieldPickup(swordLevel == 0);
            for (int index = 0; index < this.slots.size(); index++) {
                if (this.slots.get(index).type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP) {
                    this.slots.replace(
                            index,
                            withDefinition(
                                    this.slots.get(index),
                                    pickupDefinition,
                                    this.slots.get(index).spriteVariant()));
                }
            }
        }
    }

    public void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        EntitySlotValidation.validateByte(magicPowderCount, "Magic Powder count");
        this.story.writePlayerHasToadstool(hasToadstool);
        this.story.writePlayerMagicPowderCount(magicPowderCount);
    }

    public void setTailKeyOwned(boolean owned) {
        this.story.writePlayerHasTailKey(owned);
    }

    public void setFollowingNpcState(FollowingNpcState state) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        var followingNpcMotion = this.motions.get(FollowingNpcMotion.class, FollowingNpcMotion::new);
        followingNpcMotion.writeFollowingNpcState(state);
        var bowWowMotion = this.motions.get(BowWowMotion.class, BowWowMotion::new);
        if (state.bowWowFollowing()) bowWowMotion.writeBowWowState(1);
    }

    public void setColorShellWorld(ColorShellWorld world) {
        var colorShellMotion = this.motions.get(ColorShellMotion.class, ColorShellMotion::new);
        colorShellMotion.writeColorShellWorld(Objects.requireNonNullElse(world, ColorShellWorld.none()));
    }

    public void setSwitchBlockAnimationActive(boolean active) {
        this.roomVariables.writeSwitchBlockAnimationActive(active);
    }

    public void setOwlStatueDialogId(int globalDialogId) {
        this.story.owlState().writeOwlStatueDialogId(globalDialogId);
    }

    public void setOwlDialogResolver(IntUnaryOperator resolver) {
        this.story.owlState().writeOwlDialogResolver(Objects.requireNonNull(resolver, "Owl dialog resolver"));
    }

    public void setOwlInstrumentFlags(int instrument1Flags, int instrument2Flags) {
        EntitySlotValidation.validateByte(instrument1Flags, "Instrument 1 flags");
        EntitySlotValidation.validateByte(instrument2Flags, "Instrument 2 flags");
        this.roomVariables.writeInstrument1Flags(instrument1Flags);
        this.roomVariables.writeInstrument2Flags(instrument2Flags);
    }

    public void setOwlDefaultMusicResolver(IntUnaryOperator resolver) {
        this.story
                .owlState()
                .writeOwlDefaultMusicResolver(Objects.requireNonNull(resolver, "Owl default music resolver"));
    }

    public void setOcarinaPlayback(int countdown, int songFlags, int selectedSong) {
        setOcarinaPlayback(countdown, songFlags, selectedSong, 0, 0);
    }

    public void setOcarinaPlayback(
            int countdown, int songFlags, int selectedSong, int animationCounter, int animationPhase) {
        EntitySlotValidation.validateByte(countdown, "Ocarina playback countdown");
        EntitySlotValidation.validateByte(songFlags, "Ocarina song flags");
        EntitySlotValidation.validateByte(selectedSong, "Selected Ocarina song");
        EntitySlotValidation.validateByte(animationCounter, "Ocarina animation counter");
        if (animationPhase < 0 || animationPhase > 1) {
            throw new IllegalArgumentException("Ocarina animation phase must be 0 or 1");
        }
        this.linkState.writeLinkPlayingOcarinaCountdown(countdown);
        this.linkState.writeOcarinaSongFlags(songFlags);
        this.linkState.writeSelectedSongIndex(selectedSong);
        this.linkState.writeOcarinaAnimationCounter(animationCounter);
        this.linkState.writeOcarinaAnimationPhase(animationPhase);
    }

    public void setThrownDirection(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        this.liftThrow.writeThrownDirection(slot, value);
    }

    public void setLedgeTransitionTimer(int slot, int value) {
        EntitySlotValidation.validateCountdownTestValue(slot, value);
        this.liftThrow.writeLedgeTransitionTimer(slot, value);
    }

    public void setGoldenLeavesCount(int value) {
        EntitySlotValidation.validateByte(value, "Golden leaves count");
        this.roomVariables.writeEntityGoldenLeavesCount(value);
    }

    public void setBooBuddyTriggerCount(int value) {
        EntitySlotValidation.validateByte(value, "Boo Buddy trigger count");
        var booBuddyMotion = this.motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
        booBuddyMotion.writeBooBuddyTriggerCount(value);
    }

    public void setWitchEnvironment(int bgPaletteEffectAddress, int paletteDataFlags, int defaultMusicTrack) {
        EntitySlotValidation.validateByte(bgPaletteEffectAddress, "BG palette effect address");
        EntitySlotValidation.validateByte(paletteDataFlags, "Palette data flags");
        EntitySlotValidation.validateByte(defaultMusicTrack, "Default music track");
        this.presentationState.writeBgPaletteEffectAddress(bgPaletteEffectAddress);
        this.presentationState.writePaletteDataFlags(paletteDataFlags);
        this.presentationState.writeDefaultMusicTrack(defaultMusicTrack);
    }

    public void setFreeMovementMode(boolean enabled) {
        roomVariables.writeFreeMovementMode(enabled);
    }

    public void setHeartPiecesCount(int count) {
        pickups.writeHeartPiecesCount(count);
    }
}
