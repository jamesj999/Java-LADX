package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.TransientVfxRequest;

/** Read-only views over ROM slot state for gameplay and diagnostics. */
public final class EntityEnemyInspection {
    private final RoomEntityRuntime runtime;

    EntityEnemyInspection(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    int octorokState(int slot) {
        return runtime.entityState.roamingEnemyMotion.state(slot);
    }

    int octorokTransitionCountdown(int slot) {
        return runtime.entityState.roamingEnemyMotion.transitionCountdown(slot);
    }

    int octorokDirection(int slot) {
        return runtime.entityState.roamingEnemyMotion.direction(slot);
    }

    int octorokSpeedX(int slot) {
        return runtime.entityState.roamingEnemyMotion.speedX(slot);
    }

    int octorokSpeedY(int slot) {
        return runtime.entityState.roamingEnemyMotion.speedY(slot);
    }

    int ironMaskState(int slot) {
        return runtime.entityState.roamingEnemyMotion.state(slot);
    }

    int ironMaskTransitionCountdown(int slot) {
        return runtime.entityState.roamingEnemyMotion.transitionCountdown(slot);
    }

    int ironMaskDirection(int slot) {
        return runtime.entityState.roamingEnemyMotion.direction(slot);
    }

    int ironMaskSpeedX(int slot) {
        return runtime.entityState.roamingEnemyMotion.speedX(slot);
    }

    int ironMaskSpeedY(int slot) {
        return runtime.entityState.roamingEnemyMotion.speedY(slot);
    }

    int ironMaskPrivateState2(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.ironMaskPrivateState2[slot];
    }

    int moblinSwordState(int slot) {
        return runtime.entityState.moblinSwordMotion.state(slot);
    }

    int moblinSwordTransitionCountdown(int slot) {
        return runtime.entityState.moblinSwordMotion.transitionCountdown(slot);
    }

    int moblinSwordPrivateCountdown1(int slot) {
        return runtime.entityState.moblinSwordMotion.privateCountdown1(slot);
    }

    int moblinSwordPrivateState3(int slot) {
        return runtime.entityState.moblinSwordMotion.privateState3(slot);
    }

    int moblinSwordDirection(int slot) {
        return runtime.entityState.moblinSwordMotion.direction(slot);
    }

    int moblinSwordSpeedX(int slot) {
        return runtime.entityState.moblinSwordMotion.speedX(slot);
    }

    int swordMoblinAlertingSoundCounter() {
        return runtime.entityState.swordMoblinAlertingSoundCounter;
    }

    int tektiteState(int slot) {
        return runtime.entityState.tektiteMotion.state(slot);
    }

    int tektiteTransitionCountdown(int slot) {
        return runtime.entityState.tektiteMotion.transitionCountdown(slot);
    }

    int tektiteSpeedX(int slot) {
        return runtime.entityState.tektiteMotion.speedX(slot);
    }

    int tektiteSpeedY(int slot) {
        return runtime.entityState.tektiteMotion.speedY(slot);
    }

    int enemyProjectileSpeedX(int slot) {
        return runtime.entityState.enemyProjectileMotion.speedX(slot);
    }

    int enemyProjectileSpeedY(int slot) {
        return runtime.entityState.enemyProjectileMotion.speedY(slot);
    }

    int enemyProjectileTransitionCountdown(int slot) {
        return runtime.entityState.enemyProjectileMotion.transitionCountdown(slot);
    }

    int laserDirection(int slot) {
        return runtime.entityState.laserMotion.direction(slot);
    }

    int laserParentTransitionCountdown(int slot) {
        return runtime.entityState.laserMotion.parentTransitionCountdown(slot);
    }

    int laserSpeedX(int slot) {
        return runtime.entityState.laserMotion.speedX(slot);
    }

    int laserSpeedY(int slot) {
        return runtime.entityState.laserMotion.speedY(slot);
    }

    public List<TransientVfxRequest> transientVfxRequests() {
        return List.copyOf(runtime.entityState.events.transientVfxRequests);
    }

    public List<BoomerangObjectRequest> boomerangObjectRequests() {
        return List.copyOf(runtime.entityState.events.boomerangObjectRequests);
    }

    public List<MagicRodObjectRequest> magicRodObjectRequests() {
        return List.copyOf(runtime.entityState.events.magicRodObjectRequests);
    }

    public List<MagicPowderObjectRequest> magicPowderObjectRequests() {
        return List.copyOf(runtime.entityState.events.magicPowderObjectRequests);
    }

    public int physicsFlags(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.enemyPhysicsFlags[slot];
    }

    int tektiteSpeedZ(int slot) {
        return runtime.entityState.tektiteMotion.speedZ(slot);
    }

    int leeverState(int slot) {
        return runtime.entityState.leeverMotion.state(slot);
    }

    int leeverTransitionCountdown(int slot) {
        return runtime.entityState.leeverMotion.transitionCountdown(slot);
    }

    int leeverSpeedX(int slot) {
        return runtime.entityState.leeverMotion.speedX(slot);
    }

    int leeverSpeedY(int slot) {
        return runtime.entityState.leeverMotion.speedY(slot);
    }

    int antiFairySpeedX(int slot) {
        return runtime.entityState.antiFairyMotion.speedX(slot);
    }

    int antiFairySpeedY(int slot) {
        return runtime.entityState.antiFairyMotion.speedY(slot);
    }

    int starSpeedX(int slot) {
        return runtime.entityState.starMotion.speedX(slot);
    }

    int starSpeedY(int slot) {
        return runtime.entityState.starMotion.speedY(slot);
    }

    int blooperState(int slot) {
        return runtime.entityState.blooperMotion.state(slot);
    }

    int blooperTransitionCountdown(int slot) {
        return runtime.entityState.blooperMotion.transitionCountdown(slot);
    }

    int blooperDirection(int slot) {
        return runtime.entityState.blooperMotion.direction(slot);
    }

    int blooperSpeedX(int slot) {
        return runtime.entityState.blooperMotion.speedX(slot);
    }

    int blooperSpeedY(int slot) {
        return runtime.entityState.blooperMotion.speedY(slot);
    }

    int blooperPrivateCountdown3(int slot) {
        return runtime.entityState.blooperMotion.privateCountdown3(slot);
    }

    int pincerState(int slot) {
        return runtime.entityState.pincerMotion.state(slot);
    }

    int pincerHoleX(int slot) {
        return runtime.entityState.pincerMotion.holeX(slot);
    }

    int pincerHoleY(int slot) {
        return runtime.entityState.pincerMotion.holeY(slot);
    }

    int pincerLungeVariant(int slot) {
        return runtime.entityState.pincerMotion.lungeVariant(slot);
    }

    int pincerSpeedX(int slot) {
        return runtime.entityState.pincerMotion.speedX(slot);
    }

    int pincerSpeedY(int slot) {
        return runtime.entityState.pincerMotion.speedY(slot);
    }

    List<PincerBodyOam.Entry> pincerBodyOam() {
        return List.copyOf(runtime.entityState.pincerBodyOam);
    }

    int sparkPrivateState1(int slot) {
        return runtime.entityState.sparkMotion.privateState1(slot);
    }

    int sparkPrivateState2(int slot) {
        return runtime.entityState.sparkMotion.privateState2(slot);
    }

    int sparkTransitionCountdown(int slot) {
        return runtime.entityState.sparkMotion.transitionCountdown(slot);
    }

    int sparkSpeedX(int slot) {
        return runtime.entityState.sparkMotion.speedX(slot);
    }

    int sparkSpeedY(int slot) {
        return runtime.entityState.sparkMotion.speedY(slot);
    }

    int zolState(int slot) {
        return runtime.entityState.zolGelMotion.state(slot);
    }

    int zolTransitionCountdown(int slot) {
        return runtime.entityState.zolGelMotion.transitionCountdown(slot);
    }

    int zolSpeedX(int slot) {
        return runtime.entityState.zolGelMotion.speedX(slot);
    }

    int zolSpeedY(int slot) {
        return runtime.entityState.zolGelMotion.speedY(slot);
    }

    int zolSpeedZ(int slot) {
        return runtime.entityState.zolGelMotion.speedZ(slot);
    }

    int zolPrivateCountdown1(int slot) {
        return runtime.entityState.zolGelMotion.privateCountdown1(slot);
    }

    int hidingZolState(int slot) {
        return runtime.entityState.hidingZolMotion.state(slot);
    }

    int hidingZolTransitionCountdown(int slot) {
        return runtime.entityState.hidingZolMotion.transitionCountdown(slot);
    }

    int hidingZolPrivateState1(int slot) {
        return runtime.entityState.hidingZolMotion.privateState1(slot);
    }

    int hidingZolSpeedX(int slot) {
        return runtime.entityState.hidingZolMotion.speedX(slot);
    }

    int hidingZolSpeedY(int slot) {
        return runtime.entityState.hidingZolMotion.speedY(slot);
    }

    int hidingZolSpeedZ(int slot) {
        return runtime.entityState.hidingZolMotion.speedZ(slot);
    }

    int spikeTrapState(int slot) {
        return runtime.entityState.spikeTrapMotion.state(slot);
    }

    int spikeTrapTransitionCountdown(int slot) {
        return runtime.entityState.spikeTrapMotion.transitionCountdown(slot);
    }

    int spikeTrapDirection(int slot) {
        return runtime.entityState.spikeTrapMotion.direction(slot);
    }

    int spikeTrapSpeedX(int slot) {
        return runtime.entityState.spikeTrapMotion.speedX(slot);
    }

    int spikeTrapSpeedY(int slot) {
        return runtime.entityState.spikeTrapMotion.speedY(slot);
    }

    int spikeTrapPrivateState1(int slot) {
        return runtime.entityState.spikeTrapMotion.privateState1(slot);
    }

    int spikeTrapPrivateState2(int slot) {
        return runtime.entityState.spikeTrapMotion.privateState2(slot);
    }

    int waterTektiteState(int slot) {
        return runtime.entityState.waterTektiteMotion.state(slot);
    }

    int waterTektiteTransitionCountdown(int slot) {
        return runtime.entityState.waterTektiteMotion.transitionCountdown(slot);
    }

    int waterTektiteSpeedX(int slot) {
        return runtime.entityState.waterTektiteMotion.speedX(slot);
    }

    int waterTektiteSpeedY(int slot) {
        return runtime.entityState.waterTektiteMotion.speedY(slot);
    }

    int waterTektitePrivateState1(int slot) {
        return runtime.entityState.waterTektiteMotion.privateState1(slot);
    }

    int waterTektitePrivateState2(int slot) {
        return runtime.entityState.waterTektiteMotion.privateState2(slot);
    }

    int stalfosState(int slot) {
        return runtime.entityState.stalfosAggressiveMotion.state(slot);
    }

    int stalfosTransitionCountdown(int slot) {
        return runtime.entityState.stalfosAggressiveMotion.transitionCountdown(slot);
    }

    int stalfosSpeedX(int slot) {
        return runtime.entityState.stalfosAggressiveMotion.speedX(slot);
    }

    int stalfosSpeedY(int slot) {
        return runtime.entityState.stalfosAggressiveMotion.speedY(slot);
    }

    int stalfosSpeedZ(int slot) {
        return runtime.entityState.stalfosAggressiveMotion.speedZ(slot);
    }

    int evasivePrivateState1(int slot) {
        return runtime.entityState.stalfosEvasiveMotion.privateState1(slot);
    }

    int evasivePrivateCountdown1(int slot) {
        return runtime.entityState.stalfosEvasiveMotion.privateCountdown1(slot);
    }

    int evasiveInertia(int slot) {
        return runtime.entityState.stalfosEvasiveMotion.inertia(slot);
    }

    int evasiveSpeedX(int slot) {
        return runtime.entityState.stalfosEvasiveMotion.speedX(slot);
    }

    int evasiveSpeedY(int slot) {
        return runtime.entityState.stalfosEvasiveMotion.speedY(slot);
    }

    int evasiveSpeedZ(int slot) {
        return runtime.entityState.stalfosEvasiveMotion.speedZ(slot);
    }

    int gibdoState(int slot) {
        return runtime.entityState.gibdoMotion.state(slot);
    }

    int gibdoSpeedX(int slot) {
        return runtime.entityState.gibdoMotion.speedX(slot);
    }

    int gibdoSpeedY(int slot) {
        return runtime.entityState.gibdoMotion.speedY(slot);
    }

    int goombaState(int slot) {
        return runtime.entityState.goombaMotion.state(slot);
    }

    int goombaTransitionCountdown(int slot) {
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int goombaSpeedX(int slot) {
        return runtime.entityState.goombaMotion.speedX(slot);
    }

    int goombaSpeedY(int slot) {
        return runtime.entityState.goombaMotion.speedY(slot);
    }

    int snakeState(int slot) {
        return runtime.entityState.snakeMotion.state(slot);
    }

    int snakeTransitionCountdown(int slot) {
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int snakePrivateCountdown1(int slot) {
        return runtime.entityState.snakeMotion.privateCountdown1(slot);
    }

    int snakeSpeedX(int slot) {
        return runtime.entityState.snakeMotion.speedX(slot);
    }

    int snakeSpeedY(int slot) {
        return runtime.entityState.snakeMotion.speedY(slot);
    }

    int polsVoiceState(int slot) {
        return runtime.entityState.polsVoiceMotion.state(slot);
    }

    int polsVoiceTransitionCountdown(int slot) {
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int polsVoiceSpeedX(int slot) {
        return runtime.entityState.polsVoiceMotion.speedX(slot);
    }

    int polsVoiceSpeedY(int slot) {
        return runtime.entityState.polsVoiceMotion.speedY(slot);
    }

    int polsVoiceSpeedZ(int slot) {
        return runtime.entityState.polsVoiceMotion.speedZ(slot);
    }

    int spikedBeetleState(int slot) {
        return runtime.entityState.spikedBeetleMotion.state(slot);
    }

    int spikedBeetleTransitionCountdown(int slot) {
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int spikedBeetleSpeedX(int slot) {
        return runtime.entityState.spikedBeetleMotion.speedX(slot);
    }

    int spikedBeetleSpeedY(int slot) {
        return runtime.entityState.spikedBeetleMotion.speedY(slot);
    }

    int wizrobeState(int slot) {
        return runtime.entityState.wizrobeMotion.state(slot);
    }

    int wizrobeDirection(int slot) {
        return runtime.entityState.wizrobeMotion.direction(slot);
    }

    int wizrobePrivateState1(int slot) {
        return runtime.entityState.wizrobeMotion.privateState1(slot);
    }

    int wizrobePrivateCountdown1(int slot) {
        return runtime.entityState.wizrobeMotion.privateCountdown1(slot);
    }

    int wizrobeNextSpriteVariant(int slot) {
        return runtime.entityState.wizrobeMotion.nextSpriteVariant(slot);
    }

    int wizrobeProjectileSpeedX(int slot) {
        return runtime.entityState.wizrobeProjectileMotion.speedX(slot);
    }

    int wizrobeProjectileSpeedY(int slot) {
        return runtime.entityState.wizrobeProjectileMotion.speedY(slot);
    }

    int wizrobeProjectileDirection(int slot) {
        return runtime.entityState.wizrobeProjectileMotion.direction(slot);
    }

    int peaHatState(int slot) {
        return runtime.entityState.peaHatMotion.state(slot);
    }

    int peaHatSlowTransitionCountdown(int slot) {
        return runtime.entityState.peaHatMotion.slowTransitionCountdown(slot);
    }

    int peaHatPrivateState1(int slot) {
        return runtime.entityState.peaHatMotion.privateState1(slot);
    }

    int peaHatPrivateState2(int slot) {
        return runtime.entityState.peaHatMotion.privateState2(slot);
    }

    int peaHatPrivateState3(int slot) {
        return runtime.entityState.peaHatMotion.privateState3(slot);
    }

    int peaHatPrivateState4(int slot) {
        return runtime.entityState.peaHatMotion.privateState4(slot);
    }

    int peaHatSpeedX(int slot) {
        return runtime.entityState.peaHatMotion.speedX(slot);
    }

    int peaHatSpeedY(int slot) {
        return runtime.entityState.peaHatMotion.speedY(slot);
    }

    int armosState(int slot) {
        return runtime.entityState.armosMotion.state(slot);
    }

    int armosTransitionCountdown(int slot) {
        return runtime.entityState.armosMotion.transitionCountdown(slot);
    }

    int armosSpeedX(int slot) {
        return runtime.entityState.armosMotion.speedX(slot);
    }

    int armosSpeedY(int slot) {
        return runtime.entityState.armosMotion.speedY(slot);
    }

    int ghiniTransitionCountdown(int slot) {
        return runtime.entityState.ghiniMotion.transitionCountdown(slot);
    }

    int ghiniPrivateCountdown1(int slot) {
        return runtime.entityState.ghiniMotion.privateCountdown1(slot);
    }

    int ghiniTargetXDirection(int slot) {
        return runtime.entityState.ghiniMotion.targetXDirection(slot);
    }

    int ghiniTargetYDirection(int slot) {
        return runtime.entityState.ghiniMotion.targetYDirection(slot);
    }

    int ghiniSpeedX(int slot) {
        return runtime.entityState.ghiniMotion.speedX(slot);
    }

    int ghiniSpeedY(int slot) {
        return runtime.entityState.ghiniMotion.speedY(slot);
    }

    int hardHatSpeedX(int slot) {
        return runtime.entityState.hardHatMotion.speedX(slot);
    }

    int hardHatSpeedY(int slot) {
        return runtime.entityState.hardHatMotion.speedY(slot);
    }

    int butterflyPrivateStateX(int slot) {
        return runtime.entityState.butterflyMotion.privateStateX(slot);
    }

    int butterflyPrivateStateY(int slot) {
        return runtime.entityState.butterflyMotion.privateStateY(slot);
    }

    public int transitionCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int dogState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (!runtime.roomInspection.isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_DOG)) {
            throw new IllegalArgumentException("Entity slot does not contain a dog: " + slot);
        }
        return runtime.entityState.dogMotion.state(slot);
    }

    int witchRatState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (!runtime.roomInspection.isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.entityState.witchRatMotion.state(slot);
    }

    int witchRatTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (!runtime.roomInspection.isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int witchRatSpeedZ(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (!runtime.roomInspection.isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.entityState.witchRatMotion.speedZ(slot);
    }

    int stunnedCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.enemyStunnedCountdown[slot];
    }

    int dyingCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.dyingCountdown[slot];
    }

    boolean powerRecoilDeath(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.powerRecoilDeath[slot];
    }

    public int enemyHealth(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.enemyHealth[slot];
    }

    int enemyFlashCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.enemyFlashCountdown[slot];
    }

    public int enemyIgnoreHitsCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.enemyIgnoreHitsCountdown[slot];
    }

    int enemyRecoilSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyRecoilMotion.recoilSpeedX(slot);
    }

    int enemyRecoilSpeedYForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyRecoilMotion.recoilSpeedY(slot);
    }

    public int ledgeTransitionTimer(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.ledgeTransitionTimer[slot] & 0xFF;
    }

    public int fallingTargetX(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.fallingTargetX[slot];
    }

    int fallingTargetY(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.fallingTargetY[slot];
    }

    int fallingVisualYOffset(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.fallingVisualYOffset[slot];
    }

    int musicalNoteInertiaForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.musicalNoteInertia[slot];
    }

    int hitboxFlagsForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyHitboxFlags[slot];
    }

    int enemyRecoilSpeedX(int slot) {
        return runtime.entityState.enemyRecoilMotion.recoilSpeedX(slot);
    }

    int enemyRecoilSpeedY(int slot) {
        return runtime.entityState.enemyRecoilMotion.recoilSpeedY(slot);
    }

    boolean enemyRecoilActive(int slot) {
        return runtime.entityState.enemyRecoilMotion.isActive(slot);
    }

    public boolean polsVoiceBalladOcarinaTrigger() {
        return runtime.entityState.linkState.linkPlayingOcarinaCountdown == 0x01
                && (runtime.entityState.linkState.ocarinaSongFlags & 0x04) != 0
                && runtime.entityState.linkState.selectedSongIndex == 0;
    }

    boolean shouldDisappear(RoomEntity entity) {
        return runtime.entityState.slowTimerInitialized[entity.slot()]
                && isCommonDroppableType(entity.type())
                && entity.type() != EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                // DroppableDisappearIfNeeded is after the hidden-state helper in
                // every shared drop handler. A hidden item therefore must not be
                // unloaded merely because its global fade timer reached zero.
                && runtime.entityState.droppablePrivateState3[entity.slot()] == 0
                && runtime.entityState.slowTransitionCountdown[entity.slot()] < 0x1C;
    }

    boolean isDisabledFollower(int type) {
        return switch (type) {
            case FollowingNpcEntitySpawner.ENTITY_ROOSTER -> !runtime.entityState.followingNpcState.roosterFollowing();
            case FollowingNpcEntitySpawner.ENTITY_GHOST -> runtime.entityState.followingNpcState.ghostFollowingState()
                    != 1;
            case FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE -> !runtime.entityState.followingNpcState
                    .marinFollowing();
            case FollowingNpcEntitySpawner.ENTITY_BOW_WOW -> !runtime.entityState.followingNpcState.bowWowFollowing();
            default -> false;
        };
    }

    boolean entityHandlerInteractive(boolean creditsGameplay) {
        if (!creditsGameplay
                && (!runtime.entityState.gameplayWorld || runtime.entityState.transitionSequenceCounter != 0x04)) {
            return false;
        }
        return !runtime.entityState.linkState.dialogActive
                && !runtime.entityState.linkState.inventoryAppearing
                && !runtime.inspection().witchGotItemPresentationActive()
                && !runtime.entityState.roomTransitionActive;
    }

    boolean entityTimersInteractive() {
        return !runtime.entityState.linkState.dialogActive
                && !runtime.entityState.linkState.inventoryAppearing
                && runtime.entityState.linkState.linkPlayingOcarinaCountdown == 0;
    }
}
