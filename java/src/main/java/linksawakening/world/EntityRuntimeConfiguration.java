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

/** Room entity configuration preserving source-shaped handler inputs. */
final class EntityRuntimeConfiguration {
    private final RoomEntityRuntime runtime;
    EntityRuntimeConfiguration(RoomEntityRuntime runtime) { this.runtime = runtime; }

    void setBoomerangSoundCounterForFrame(int counter) {
        if (counter < 0 || counter >= runtime.BOOMERANG_SFX_COUNTER_PERIOD) {
            throw new IllegalArgumentException("Boomerang sound counter out of range: " + counter);
        }
        runtime.boomerangSfxCounter = counter;
        runtime.boomerangSoundClockProvided = true;
    }

    void setTorchPaletteTransitionState(int targetAddress, boolean active) {
        runtime.validateByte(targetAddress, "Torch palette target");
        runtime.torchPaletteTargetAddress = targetAddress;
        runtime.torchPaletteTransitionActive = active;
    }

    void setLinkVelocityZ(int velocity) {
        runtime.currentLinkVelocityZ = velocity & 0xFF;
    }

    void setSpriteSelection(EntitySpriteSelection selection) {
        runtime.spriteSelection = selection;
    }

    void setGroundInteraction(RoomEntityGroundInteraction groundInteraction) {
        runtime.groundInteraction = groundInteraction == null
            ? (entity, frameCounter, previousGroundStatus, speedZ, sideScrolling) ->
                RoomEntityGroundInteraction.Result.unchanged(entity, 0)
            : groundInteraction;
    }

    void setBackgroundInteraction(RoomEntityBackgroundInteraction backgroundInteraction) {
        runtime.backgroundInteraction = backgroundInteraction;
    }

    void setObjectQuery(RoomEntityObjectQuery objectQuery) {
        runtime.objectQuery = objectQuery;
    }

    void setObjectIntersectionQuery(RoomEntityObjectQuery objectIntersectionQuery) {
        runtime.objectIntersectionQuery = objectIntersectionQuery;
    }

    void setActionButtonsHeld(boolean actionButtonsHeld) {
        setActionButtonsHeld(actionButtonsHeld, actionButtonsHeld);
    }

    void setActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) {
        runtime.actionButtonAHeld = actionButtonAHeld;
        runtime.actionButtonBHeld = actionButtonBHeld;
        runtime.actionButtonsHeld = actionButtonAHeld || actionButtonBHeld;
    }

    void setJoypadHeld(boolean joypadHeld) {
        runtime.joypadHeld = joypadHeld;
    }

    void setPressedButtonsMask(int pressedButtonsMask) {
        runtime.validateByte(pressedButtonsMask, "Pressed buttons mask");
        runtime.linkPressedButtonsMask = pressedButtonsMask;
    }

    void setDialogActive(boolean dialogActive) {
        runtime.dialogActive = dialogActive;
    }

    void setTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) {
        runtime.validateByte(dialogCooldown, "Dialog cooldown");
        runtime.validateByte(windowY, "Window Y");
        runtime.inventoryAppearing = inventoryAppearing;
        runtime.dialogCooldown = dialogCooldown;
        runtime.windowY = windowY;
    }

    void setDialogAskSelectionIndex(int selectionIndex) {
        if (selectionIndex < 0 || selectionIndex > 1) {
            throw new IllegalArgumentException("Dialog selection must be 0 or 1");
        }
        runtime.dialogAskSelectionIndex = selectionIndex;
    }

    void setActiveMusic(boolean activeMusic) {
        runtime.activeMusic = activeMusic;
    }

    void setBowWowState(int bowWowState) {
        runtime.bowWowState = bowWowState & 0xFF;
    }

    void setLikeLikeLinkInventory(int itemA, int itemB) {
        runtime.validateByte(itemA, "Link A inventory slot");
        runtime.validateByte(itemB, "Link B inventory slot");
        runtime.linkItemA = itemA;
        runtime.linkItemB = itemB;
        runtime.linkInventoryConfigured = true;
    }

    void setPowerBraceletButtonHeld(boolean powerBraceletButtonHeld) {
        runtime.powerBraceletButtonHeld = powerBraceletButtonHeld;
    }

    void setBombButtonHeld(boolean bombButtonHeld) {
        runtime.bombButtonHeld = bombButtonHeld;
    }

    void setLinkAttackStepAnimationCountdown(int countdown) {
        runtime.validateByte(countdown, "Link attack-step animation countdown");
        runtime.linkAttackStepAnimationCountdown = countdown;
    }

    void setLinkFinalPositionX(int positionX) {
        runtime.validateByte(positionX, "Link final position X");
        runtime.currentLinkFinalPositionX = positionX;
        runtime.currentLinkFinalPositionXConfigured = true;
    }

    void setLiftedLinkC13B(int linkC13B) {
        if ((linkC13B & ~0xFF) != 0) {
            throw new IllegalArgumentException("Lifted Link C13B must be an unsigned byte: "
                + linkC13B);
        }
        runtime.liftedLinkC13B = linkC13B;
    }

    void setGroundInteractionSideScrolling(boolean sideScrolling) {
        runtime.groundInteractionSideScrolling = sideScrolling;
    }

    void setEnemyDropResolver(EnemyDropResolver resolver) {
        runtime.enemyDropResolver = resolver;
    }

    void setEnemyDropCounters(EnemyDropResolver.CounterState counters) {
        runtime.enemyDropCounters = Objects.requireNonNull(counters, "Drop counters cannot be null");
    }

    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        runtime.validateEnemyDropByte(maxHearts, "Maximum hearts");
        runtime.validateEnemyDropByte(health, "Health");
        runtime.enemyDropMaxHearts = maxHearts;
        runtime.enemyDropHealth = health;
        runtime.enemyDropActivePowerUp = activePowerUp;
    }

    void setEnemyDropBossBattle(boolean bossBattle) {
        runtime.enemyDropBossBattle = bossBattle;
    }

    void setEntityMapId(int mapId) {
        if (mapId < -1 || mapId > 0xFF) {
            throw new IllegalArgumentException("Entity map id must be -1 or an unsigned byte: "
                + mapId);
        }
        runtime.entityMapId = mapId;
    }

    void setEntityRoomId(int roomId) {
        runtime.validateByte(roomId, "Entity room id");
        runtime.entityRoomId = roomId;
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == runtime.ENTITY_DROPPABLE_SECRET_SEASHELL) {
                runtime.secretSeashellMotion.ensureInitialized(entity.slot(), roomId);
                runtime.entityOptions1Override[entity.slot()] =
                    runtime.secretSeashellMotion.privateState3(entity.slot()) != 0
                        ? runtime.SECRET_SEASHELL_HIDDEN_OPTIONS1
                        : runtime.SECRET_SEASHELL_REVEALED_OPTIONS1;
            }
        }
    }

    void setEntityRoomStatus(int roomStatus) {
        runtime.validateByte(roomStatus, "Entity room status");
        runtime.entityRoomStatus = roomStatus;
    }

    void setDungeonMinibossReady(boolean ready) {
        runtime.dungeonMinibossReady = ready;
    }

    void setSecretSeashellPegasusCollisionState(boolean screenShakeActive,
                                                 boolean collisionActive,
                                                 int collisionX, int collisionY) {
        runtime.validateByte(collisionX, "Pegasus collision X");
        runtime.validateByte(collisionY, "Pegasus collision Y");
        runtime.secretSeashellScreenShakeActive = screenShakeActive;
        runtime.secretSeashellPegasusCollisionActive = collisionActive;
        runtime.secretSeashellPegasusCollisionCoordinatesKnown = true;
        runtime.secretSeashellPegasusCollisionX = collisionX;
        runtime.secretSeashellPegasusCollisionY = collisionY;
    }

    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        runtime.validateByte(shieldLevel, "Shield level");
        runtime.validateByte(swordLevel, "Sword level");
        runtime.validateByte(powerBraceletLevel, "Power Bracelet level");
        runtime.chestShieldLevel = shieldLevel;
        runtime.chestSwordLevel = swordLevel;
        runtime.chestPowerBraceletLevel = powerBraceletLevel;
        runtime.chestPlayerLevelsKnown = true;
        if (runtime.spriteHandlers != null) {
            EntitySpriteDefinition pickupDefinition =
                runtime.spriteHandlers.forSwordShieldPickup(swordLevel == 0);
            for (int index = 0; index < runtime.slots.length; index++) {
                if (runtime.slots[index].type() == runtime.ENTITY_SWORD_SHIELD_PICKUP) {
                    runtime.slots[index] = withDefinition(runtime.slots[index], pickupDefinition,
                        runtime.slots[index].spriteVariant());
                }
            }
        }
    }

    void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) {
        runtime.validateByte(magicPowderCount, "Magic Powder count");
        runtime.playerHasToadstool = hasToadstool;
        runtime.playerMagicPowderCount = magicPowderCount;
    }

    void setTailKeyOwned(boolean owned) {
        runtime.playerHasTailKey = owned;
    }

    void setFollowingNpcState(FollowingNpcState state) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        runtime.followingNpcState = state;
        if (state.bowWowFollowing()) runtime.bowWowState = 1;
    }

    void setColorShellWorld(ColorShellWorld world) {
        runtime.colorShellWorld = world == null ? ColorShellWorld.none() : world;
    }

    void setSwitchBlockAnimationActive(boolean active) {
        runtime.switchBlockAnimationActive = active;
    }

    void setOwlStatueDialogId(int globalDialogId) {
        runtime.owlStatueDialogId = globalDialogId;
    }

    void setOwlDialogResolver(IntUnaryOperator resolver) {
        runtime.owlDialogResolver = Objects.requireNonNull(resolver, "Owl dialog resolver");
    }

    void setOwlInstrumentFlags(int instrument1Flags, int instrument2Flags) {
        runtime.validateByte(instrument1Flags, "Instrument 1 flags");
        runtime.validateByte(instrument2Flags, "Instrument 2 flags");
        runtime.instrument1Flags = instrument1Flags;
        runtime.instrument2Flags = instrument2Flags;
    }

    void setOwlDefaultMusicResolver(IntUnaryOperator resolver) {
        runtime.owlDefaultMusicResolver = Objects.requireNonNull(resolver,
            "Owl default music resolver");
    }

    void setOcarinaPlayback(int countdown, int songFlags, int selectedSong) {
        setOcarinaPlayback(countdown, songFlags, selectedSong, 0, 0);
    }

    void setOcarinaPlayback(int countdown, int songFlags, int selectedSong,
                            int animationCounter, int animationPhase) {
        runtime.validateByte(countdown, "Ocarina playback countdown");
        runtime.validateByte(songFlags, "Ocarina song flags");
        runtime.validateByte(selectedSong, "Selected Ocarina song");
        runtime.validateByte(animationCounter, "Ocarina animation counter");
        if (animationPhase < 0 || animationPhase > 1) {
            throw new IllegalArgumentException("Ocarina animation phase must be 0 or 1");
        }
        runtime.linkPlayingOcarinaCountdown = countdown;
        runtime.ocarinaSongFlags = songFlags;
        runtime.selectedSongIndex = selectedSong;
        runtime.ocarinaAnimationCounter = animationCounter;
        runtime.ocarinaAnimationPhase = animationPhase;
    }

    void setThrownDirection(int slot, int value) {
        runtime.validateCountdownTestValue(slot, value);
        runtime.thrownDirection[slot] = value;
    }

    void setLedgeTransitionTimer(int slot, int value) {
        runtime.validateCountdownTestValue(slot, value);
        runtime.ledgeTransitionTimer[slot] = value;
    }

    void setGoldenLeavesCount(int value) {
        runtime.validateByte(value, "Golden leaves count");
        runtime.entityGoldenLeavesCount = value;
    }

    void setBooBuddyTriggerCount(int value) {
        runtime.validateByte(value, "Boo Buddy trigger count");
        runtime.booBuddyTriggerCount = value;
    }

    void setWitchEnvironment(int bgPaletteEffectAddress, int paletteDataFlags,
                             int defaultMusicTrack) {
        runtime.validateByte(bgPaletteEffectAddress, "BG palette effect address");
        runtime.validateByte(paletteDataFlags, "Palette data flags");
        runtime.validateByte(defaultMusicTrack, "Default music track");
        runtime.bgPaletteEffectAddress = bgPaletteEffectAddress;
        runtime.paletteDataFlags = paletteDataFlags;
        runtime.defaultMusicTrack = defaultMusicTrack;
    }
}
