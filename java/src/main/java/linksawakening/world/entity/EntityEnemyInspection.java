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
    private final EntityEnemyInspectionServices runtime;

    EntityEnemyInspection(EntityEnemyInspectionServices runtime) {
        this.runtime = runtime;
    }

    int octorokState(int slot) {
        return runtime.motionSet().roamingEnemyMotion.state(slot);
    }

    int octorokTransitionCountdown(int slot) {
        return runtime.motionSet().roamingEnemyMotion.transitionCountdown(slot);
    }

    int octorokDirection(int slot) {
        return runtime.motionSet().roamingEnemyMotion.direction(slot);
    }

    int octorokSpeedX(int slot) {
        return runtime.motionSet().roamingEnemyMotion.speedX(slot);
    }

    int octorokSpeedY(int slot) {
        return runtime.motionSet().roamingEnemyMotion.speedY(slot);
    }

    int ironMaskState(int slot) {
        return runtime.motionSet().roamingEnemyMotion.state(slot);
    }

    int ironMaskTransitionCountdown(int slot) {
        return runtime.motionSet().roamingEnemyMotion.transitionCountdown(slot);
    }

    int ironMaskDirection(int slot) {
        return runtime.motionSet().roamingEnemyMotion.direction(slot);
    }

    int ironMaskSpeedX(int slot) {
        return runtime.motionSet().roamingEnemyMotion.speedX(slot);
    }

    int ironMaskSpeedY(int slot) {
        return runtime.motionSet().roamingEnemyMotion.speedY(slot);
    }

    int ironMaskPrivateState2(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motionSet().unmaskedIronMaskMotion.maskRemovalState(slot);
    }

    int moblinSwordState(int slot) {
        return runtime.motionSet().moblinSwordMotion.state(slot);
    }

    int moblinSwordTransitionCountdown(int slot) {
        return runtime.motionSet().moblinSwordMotion.transitionCountdown(slot);
    }

    int moblinSwordPrivateCountdown1(int slot) {
        return runtime.motionSet().moblinSwordMotion.privateCountdown1(slot);
    }

    int moblinSwordPrivateState3(int slot) {
        return runtime.motionSet().moblinSwordMotion.privateState3(slot);
    }

    int moblinSwordDirection(int slot) {
        return runtime.motionSet().moblinSwordMotion.direction(slot);
    }

    int moblinSwordSpeedX(int slot) {
        return runtime.motionSet().moblinSwordMotion.speedX(slot);
    }

    int swordMoblinAlertingSoundCounter() {
        return runtime.motionSet().moblinSwordMotion.readSwordMoblinAlertingSoundCounter();
    }

    int tektiteState(int slot) {
        return runtime.motionSet().tektiteMotion.state(slot);
    }

    int tektiteTransitionCountdown(int slot) {
        return runtime.motionSet().tektiteMotion.transitionCountdown(slot);
    }

    int tektiteSpeedX(int slot) {
        return runtime.motionSet().tektiteMotion.speedX(slot);
    }

    int tektiteSpeedY(int slot) {
        return runtime.motionSet().tektiteMotion.speedY(slot);
    }

    int enemyProjectileSpeedX(int slot) {
        return runtime.motionSet().enemyProjectileMotion.speedX(slot);
    }

    int enemyProjectileSpeedY(int slot) {
        return runtime.motionSet().enemyProjectileMotion.speedY(slot);
    }

    int enemyProjectileTransitionCountdown(int slot) {
        return runtime.motionSet().enemyProjectileMotion.transitionCountdown(slot);
    }

    int laserDirection(int slot) {
        return runtime.motionSet().laserMotion.direction(slot);
    }

    int laserParentTransitionCountdown(int slot) {
        return runtime.motionSet().laserMotion.parentTransitionCountdown(slot);
    }

    int laserSpeedX(int slot) {
        return runtime.motionSet().laserMotion.speedX(slot);
    }

    int laserSpeedY(int slot) {
        return runtime.motionSet().laserMotion.speedY(slot);
    }

    public List<TransientVfxRequest> transientVfxRequests() {
        return List.copyOf(runtime.events().transientVfxRequests);
    }

    public List<BoomerangObjectRequest> boomerangObjectRequests() {
        return List.copyOf(runtime.events().boomerangObjectRequests);
    }

    public List<MagicRodObjectRequest> magicRodObjectRequests() {
        return List.copyOf(runtime.events().magicRodObjectRequests);
    }

    public List<MagicPowderObjectRequest> magicPowderObjectRequests() {
        return List.copyOf(runtime.events().magicPowderObjectRequests);
    }

    public int physicsFlags(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readEnemyPhysicsFlags(slot);
    }

    int tektiteSpeedZ(int slot) {
        return runtime.motionSet().tektiteMotion.speedZ(slot);
    }

    int leeverState(int slot) {
        return runtime.motionSet().leeverMotion.state(slot);
    }

    int leeverTransitionCountdown(int slot) {
        return runtime.motionSet().leeverMotion.transitionCountdown(slot);
    }

    int leeverSpeedX(int slot) {
        return runtime.motionSet().leeverMotion.speedX(slot);
    }

    int leeverSpeedY(int slot) {
        return runtime.motionSet().leeverMotion.speedY(slot);
    }

    int antiFairySpeedX(int slot) {
        return runtime.motionSet().antiFairyMotion.speedX(slot);
    }

    int antiFairySpeedY(int slot) {
        return runtime.motionSet().antiFairyMotion.speedY(slot);
    }

    int starSpeedX(int slot) {
        return runtime.motionSet().starMotion.speedX(slot);
    }

    int starSpeedY(int slot) {
        return runtime.motionSet().starMotion.speedY(slot);
    }

    int blooperState(int slot) {
        return runtime.motionSet().blooperMotion.state(slot);
    }

    int blooperTransitionCountdown(int slot) {
        return runtime.motionSet().blooperMotion.transitionCountdown(slot);
    }

    int blooperDirection(int slot) {
        return runtime.motionSet().blooperMotion.direction(slot);
    }

    int blooperSpeedX(int slot) {
        return runtime.motionSet().blooperMotion.speedX(slot);
    }

    int blooperSpeedY(int slot) {
        return runtime.motionSet().blooperMotion.speedY(slot);
    }

    int blooperPrivateCountdown3(int slot) {
        return runtime.motionSet().blooperMotion.privateCountdown3(slot);
    }

    int pincerState(int slot) {
        return runtime.motionSet().pincerMotion.state(slot);
    }

    int pincerHoleX(int slot) {
        return runtime.motionSet().pincerMotion.holeX(slot);
    }

    int pincerHoleY(int slot) {
        return runtime.motionSet().pincerMotion.holeY(slot);
    }

    int pincerLungeVariant(int slot) {
        return runtime.motionSet().pincerMotion.lungeVariant(slot);
    }

    int pincerSpeedX(int slot) {
        return runtime.motionSet().pincerMotion.speedX(slot);
    }

    int pincerSpeedY(int slot) {
        return runtime.motionSet().pincerMotion.speedY(slot);
    }

    List<PincerBodyOam.Entry> pincerBodyOam() {
        return List.copyOf(runtime.slotProperties().readPincerBodyOam());
    }

    int sparkPrivateState1(int slot) {
        return runtime.motionSet().sparkMotion.privateState1(slot);
    }

    int sparkPrivateState2(int slot) {
        return runtime.motionSet().sparkMotion.privateState2(slot);
    }

    int sparkTransitionCountdown(int slot) {
        return runtime.motionSet().sparkMotion.transitionCountdown(slot);
    }

    int sparkSpeedX(int slot) {
        return runtime.motionSet().sparkMotion.speedX(slot);
    }

    int sparkSpeedY(int slot) {
        return runtime.motionSet().sparkMotion.speedY(slot);
    }

    int zolState(int slot) {
        return runtime.motionSet().zolGelMotion.state(slot);
    }

    int zolTransitionCountdown(int slot) {
        return runtime.motionSet().zolGelMotion.transitionCountdown(slot);
    }

    int zolSpeedX(int slot) {
        return runtime.motionSet().zolGelMotion.speedX(slot);
    }

    int zolSpeedY(int slot) {
        return runtime.motionSet().zolGelMotion.speedY(slot);
    }

    int zolSpeedZ(int slot) {
        return runtime.motionSet().zolGelMotion.speedZ(slot);
    }

    int zolPrivateCountdown1(int slot) {
        return runtime.motionSet().zolGelMotion.privateCountdown1(slot);
    }

    int hidingZolState(int slot) {
        return runtime.motionSet().hidingZolMotion.state(slot);
    }

    int hidingZolTransitionCountdown(int slot) {
        return runtime.motionSet().hidingZolMotion.transitionCountdown(slot);
    }

    int hidingZolPrivateState1(int slot) {
        return runtime.motionSet().hidingZolMotion.privateState1(slot);
    }

    int hidingZolSpeedX(int slot) {
        return runtime.motionSet().hidingZolMotion.speedX(slot);
    }

    int hidingZolSpeedY(int slot) {
        return runtime.motionSet().hidingZolMotion.speedY(slot);
    }

    int hidingZolSpeedZ(int slot) {
        return runtime.motionSet().hidingZolMotion.speedZ(slot);
    }

    int spikeTrapState(int slot) {
        return runtime.motionSet().spikeTrapMotion.state(slot);
    }

    int spikeTrapTransitionCountdown(int slot) {
        return runtime.motionSet().spikeTrapMotion.transitionCountdown(slot);
    }

    int spikeTrapDirection(int slot) {
        return runtime.motionSet().spikeTrapMotion.direction(slot);
    }

    int spikeTrapSpeedX(int slot) {
        return runtime.motionSet().spikeTrapMotion.speedX(slot);
    }

    int spikeTrapSpeedY(int slot) {
        return runtime.motionSet().spikeTrapMotion.speedY(slot);
    }

    int spikeTrapPrivateState1(int slot) {
        return runtime.motionSet().spikeTrapMotion.privateState1(slot);
    }

    int spikeTrapPrivateState2(int slot) {
        return runtime.motionSet().spikeTrapMotion.privateState2(slot);
    }

    int waterTektiteState(int slot) {
        return runtime.motionSet().waterTektiteMotion.state(slot);
    }

    int waterTektiteTransitionCountdown(int slot) {
        return runtime.motionSet().waterTektiteMotion.transitionCountdown(slot);
    }

    int waterTektiteSpeedX(int slot) {
        return runtime.motionSet().waterTektiteMotion.speedX(slot);
    }

    int waterTektiteSpeedY(int slot) {
        return runtime.motionSet().waterTektiteMotion.speedY(slot);
    }

    int waterTektitePrivateState1(int slot) {
        return runtime.motionSet().waterTektiteMotion.privateState1(slot);
    }

    int waterTektitePrivateState2(int slot) {
        return runtime.motionSet().waterTektiteMotion.privateState2(slot);
    }

    int stalfosState(int slot) {
        return runtime.motionSet().stalfosAggressiveMotion.state(slot);
    }

    int stalfosTransitionCountdown(int slot) {
        return runtime.motionSet().stalfosAggressiveMotion.transitionCountdown(slot);
    }

    int stalfosSpeedX(int slot) {
        return runtime.motionSet().stalfosAggressiveMotion.speedX(slot);
    }

    int stalfosSpeedY(int slot) {
        return runtime.motionSet().stalfosAggressiveMotion.speedY(slot);
    }

    int stalfosSpeedZ(int slot) {
        return runtime.motionSet().stalfosAggressiveMotion.speedZ(slot);
    }

    int evasivePrivateState1(int slot) {
        return runtime.motionSet().stalfosEvasiveMotion.privateState1(slot);
    }

    int evasivePrivateCountdown1(int slot) {
        return runtime.motionSet().stalfosEvasiveMotion.privateCountdown1(slot);
    }

    int evasiveInertia(int slot) {
        return runtime.motionSet().stalfosEvasiveMotion.inertia(slot);
    }

    int evasiveSpeedX(int slot) {
        return runtime.motionSet().stalfosEvasiveMotion.speedX(slot);
    }

    int evasiveSpeedY(int slot) {
        return runtime.motionSet().stalfosEvasiveMotion.speedY(slot);
    }

    int evasiveSpeedZ(int slot) {
        return runtime.motionSet().stalfosEvasiveMotion.speedZ(slot);
    }

    int gibdoState(int slot) {
        return runtime.motionSet().gibdoMotion.state(slot);
    }

    int gibdoSpeedX(int slot) {
        return runtime.motionSet().gibdoMotion.speedX(slot);
    }

    int gibdoSpeedY(int slot) {
        return runtime.motionSet().gibdoMotion.speedY(slot);
    }

    int goombaState(int slot) {
        return runtime.motionSet().goombaMotion.state(slot);
    }

    int goombaTransitionCountdown(int slot) {
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int goombaSpeedX(int slot) {
        return runtime.motionSet().goombaMotion.speedX(slot);
    }

    int goombaSpeedY(int slot) {
        return runtime.motionSet().goombaMotion.speedY(slot);
    }

    int snakeState(int slot) {
        return runtime.motionSet().snakeMotion.state(slot);
    }

    int snakeTransitionCountdown(int slot) {
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int snakePrivateCountdown1(int slot) {
        return runtime.motionSet().snakeMotion.privateCountdown1(slot);
    }

    int snakeSpeedX(int slot) {
        return runtime.motionSet().snakeMotion.speedX(slot);
    }

    int snakeSpeedY(int slot) {
        return runtime.motionSet().snakeMotion.speedY(slot);
    }

    int polsVoiceState(int slot) {
        return runtime.motionSet().polsVoiceMotion.state(slot);
    }

    int polsVoiceTransitionCountdown(int slot) {
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int polsVoiceSpeedX(int slot) {
        return runtime.motionSet().polsVoiceMotion.speedX(slot);
    }

    int polsVoiceSpeedY(int slot) {
        return runtime.motionSet().polsVoiceMotion.speedY(slot);
    }

    int polsVoiceSpeedZ(int slot) {
        return runtime.motionSet().polsVoiceMotion.speedZ(slot);
    }

    int spikedBeetleState(int slot) {
        return runtime.motionSet().spikedBeetleMotion.state(slot);
    }

    int spikedBeetleTransitionCountdown(int slot) {
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int spikedBeetleSpeedX(int slot) {
        return runtime.motionSet().spikedBeetleMotion.speedX(slot);
    }

    int spikedBeetleSpeedY(int slot) {
        return runtime.motionSet().spikedBeetleMotion.speedY(slot);
    }

    int wizrobeState(int slot) {
        return runtime.motionSet().wizrobeMotion.state(slot);
    }

    int wizrobeDirection(int slot) {
        return runtime.motionSet().wizrobeMotion.direction(slot);
    }

    int wizrobePrivateState1(int slot) {
        return runtime.motionSet().wizrobeMotion.privateState1(slot);
    }

    int wizrobePrivateCountdown1(int slot) {
        return runtime.motionSet().wizrobeMotion.privateCountdown1(slot);
    }

    int wizrobeNextSpriteVariant(int slot) {
        return runtime.motionSet().wizrobeMotion.nextSpriteVariant(slot);
    }

    int wizrobeProjectileSpeedX(int slot) {
        return runtime.motionSet().wizrobeProjectileMotion.speedX(slot);
    }

    int wizrobeProjectileSpeedY(int slot) {
        return runtime.motionSet().wizrobeProjectileMotion.speedY(slot);
    }

    int wizrobeProjectileDirection(int slot) {
        return runtime.motionSet().wizrobeProjectileMotion.direction(slot);
    }

    int peaHatState(int slot) {
        return runtime.motionSet().peaHatMotion.state(slot);
    }

    int peaHatSlowTransitionCountdown(int slot) {
        return runtime.motionSet().peaHatMotion.slowTransitionCountdown(slot);
    }

    int peaHatPrivateState1(int slot) {
        return runtime.motionSet().peaHatMotion.privateState1(slot);
    }

    int peaHatPrivateState2(int slot) {
        return runtime.motionSet().peaHatMotion.privateState2(slot);
    }

    int peaHatPrivateState3(int slot) {
        return runtime.motionSet().peaHatMotion.privateState3(slot);
    }

    int peaHatPrivateState4(int slot) {
        return runtime.motionSet().peaHatMotion.privateState4(slot);
    }

    int peaHatSpeedX(int slot) {
        return runtime.motionSet().peaHatMotion.speedX(slot);
    }

    int peaHatSpeedY(int slot) {
        return runtime.motionSet().peaHatMotion.speedY(slot);
    }

    int armosState(int slot) {
        return runtime.motionSet().armosMotion.state(slot);
    }

    int armosTransitionCountdown(int slot) {
        return runtime.motionSet().armosMotion.transitionCountdown(slot);
    }

    int armosSpeedX(int slot) {
        return runtime.motionSet().armosMotion.speedX(slot);
    }

    int armosSpeedY(int slot) {
        return runtime.motionSet().armosMotion.speedY(slot);
    }

    int ghiniTransitionCountdown(int slot) {
        return runtime.motionSet().ghiniMotion.transitionCountdown(slot);
    }

    int ghiniPrivateCountdown1(int slot) {
        return runtime.motionSet().ghiniMotion.privateCountdown1(slot);
    }

    int ghiniTargetXDirection(int slot) {
        return runtime.motionSet().ghiniMotion.targetXDirection(slot);
    }

    int ghiniTargetYDirection(int slot) {
        return runtime.motionSet().ghiniMotion.targetYDirection(slot);
    }

    int ghiniSpeedX(int slot) {
        return runtime.motionSet().ghiniMotion.speedX(slot);
    }

    int ghiniSpeedY(int slot) {
        return runtime.motionSet().ghiniMotion.speedY(slot);
    }

    int hardHatSpeedX(int slot) {
        return runtime.motionSet().hardHatMotion.speedX(slot);
    }

    int hardHatSpeedY(int slot) {
        return runtime.motionSet().hardHatMotion.speedY(slot);
    }

    int butterflyPrivateStateX(int slot) {
        return runtime.motionSet().butterflyMotion.privateStateX(slot);
    }

    int butterflyPrivateStateY(int slot) {
        return runtime.motionSet().butterflyMotion.privateStateY(slot);
    }

    public int transitionCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int dogState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (!runtime.roomInspection().isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_DOG)) {
            throw new IllegalArgumentException("Entity slot does not contain a dog: " + slot);
        }
        return runtime.motionSet().dogMotion.state(slot);
    }

    int witchRatState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (!runtime.roomInspection().isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.motionSet().witchRatMotion.state(slot);
    }

    int witchRatTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (!runtime.roomInspection().isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int witchRatSpeedZ(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (!runtime.roomInspection().isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.motionSet().witchRatMotion.speedZ(slot);
    }

    int stunnedCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.combatTables().readEnemyStunnedCountdown(slot);
    }

    int dyingCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.combatTables().readDyingCountdown(slot);
    }

    boolean powerRecoilDeath(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.combatTables().readPowerRecoilDeath(slot);
    }

    public int enemyHealth(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.combatTables().readEnemyHealth(slot);
    }

    int enemyFlashCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.combatTables().readEnemyFlashCountdown(slot);
    }

    public int enemyIgnoreHitsCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.combatTables().readEnemyIgnoreHitsCountdown(slot);
    }

    int enemyRecoilSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motionSet().enemyRecoilMotion.recoilSpeedX(slot);
    }

    int enemyRecoilSpeedYForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motionSet().enemyRecoilMotion.recoilSpeedY(slot);
    }

    public int ledgeTransitionTimer(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.liftThrow().readLedgeTransitionTimer(slot) & 0xFF;
    }

    public int fallingTargetX(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readFallingTargetX(slot);
    }

    int fallingTargetY(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readFallingTargetY(slot);
    }

    int fallingVisualYOffset(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readFallingVisualYOffset(slot);
    }

    int musicalNoteInertiaForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.projectiles().readMusicalNoteInertia(slot);
    }

    int hitboxFlagsForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.combatTables().readEnemyHitboxFlags(slot);
    }

    int enemyRecoilSpeedX(int slot) {
        return runtime.motionSet().enemyRecoilMotion.recoilSpeedX(slot);
    }

    int enemyRecoilSpeedY(int slot) {
        return runtime.motionSet().enemyRecoilMotion.recoilSpeedY(slot);
    }

    boolean enemyRecoilActive(int slot) {
        return runtime.motionSet().enemyRecoilMotion.isActive(slot);
    }

    public boolean polsVoiceBalladOcarinaTrigger() {
        return runtime.linkState().linkPlayingOcarinaCountdown == 0x01
                && (runtime.linkState().ocarinaSongFlags & 0x04) != 0
                && runtime.linkState().selectedSongIndex == 0;
    }

    boolean shouldDisappear(RoomEntity entity) {
        return runtime.physicsTables().readSlowTimerInitialized(entity.slot())
                && isCommonDroppableType(entity.type())
                && entity.type() != EntityRomConstants.ENTITY_HIDING_SLIME_KEY
                // DroppableDisappearIfNeeded is after the hidden-state helper in
                // every shared drop handler. A hidden item therefore must not be
                // unloaded merely because its global fade timer reached zero.
                && runtime.bossRewards().readDroppablePrivateState3(entity.slot()) == 0
                && runtime.physicsTables().readSlowTransitionCountdown(entity.slot()) < 0x1C;
    }

    boolean isDisabledFollower(int type) {
        return switch (type) {
            case FollowingNpcEntitySpawner.ENTITY_ROOSTER -> !runtime.motionSet()
                    .followingNpcMotion
                    .readFollowingNpcState()
                    .roosterFollowing();
            case FollowingNpcEntitySpawner.ENTITY_GHOST -> runtime.motionSet()
                            .followingNpcMotion
                            .readFollowingNpcState()
                            .ghostFollowingState()
                    != 1;
            case FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE -> !runtime.motionSet()
                    .followingNpcMotion
                    .readFollowingNpcState()
                    .marinFollowing();
            case FollowingNpcEntitySpawner.ENTITY_BOW_WOW -> !runtime.motionSet()
                    .followingNpcMotion
                    .readFollowingNpcState()
                    .bowWowFollowing();
            default -> false;
        };
    }

    boolean entityHandlerInteractive(boolean creditsGameplay) {
        if (!creditsGameplay
                && (!runtime.roomVariables().readGameplayWorld()
                        || runtime.roomVariables().readTransitionSequenceCounter() != 0x04)) {
            return false;
        }
        return !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !runtime.inspection().witchGotItemPresentationActive()
                && !runtime.roomVariables().readRoomTransitionActive();
    }

    boolean entityTimersInteractive() {
        return !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && runtime.linkState().linkPlayingOcarinaCountdown == 0;
    }
}
