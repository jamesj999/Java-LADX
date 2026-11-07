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

/** Read-only views over ROM slot state for gameplay and diagnostics. */
final class EntityEnemyInspection {
    private final RoomEntityRuntime runtime;
    EntityEnemyInspection(RoomEntityRuntime runtime) { this.runtime = runtime; }

    int keeseState(int slot) {
        return runtime.keeseMotion.state(slot);
    }

    int keeseTransitionCountdown(int slot) {
        return runtime.keeseMotion.transitionCountdown(slot);
    }

    int keeseAngle(int slot) {
        return runtime.keeseMotion.angle(slot);
    }

    int keeseSpeedX(int slot) {
        return runtime.keeseMotion.speedX(slot);
    }

    int keeseSpeedY(int slot) {
        return runtime.keeseMotion.speedY(slot);
    }

    int octorokState(int slot) {
        return runtime.roamingEnemyMotion.state(slot);
    }

    int octorokTransitionCountdown(int slot) {
        return runtime.roamingEnemyMotion.transitionCountdown(slot);
    }

    int octorokDirection(int slot) {
        return runtime.roamingEnemyMotion.direction(slot);
    }

    int octorokSpeedX(int slot) {
        return runtime.roamingEnemyMotion.speedX(slot);
    }

    int octorokSpeedY(int slot) {
        return runtime.roamingEnemyMotion.speedY(slot);
    }

    int ironMaskState(int slot) {
        return runtime.roamingEnemyMotion.state(slot);
    }

    int ironMaskTransitionCountdown(int slot) {
        return runtime.roamingEnemyMotion.transitionCountdown(slot);
    }

    int ironMaskDirection(int slot) {
        return runtime.roamingEnemyMotion.direction(slot);
    }

    int ironMaskSpeedX(int slot) {
        return runtime.roamingEnemyMotion.speedX(slot);
    }

    int ironMaskSpeedY(int slot) {
        return runtime.roamingEnemyMotion.speedY(slot);
    }

    int ironMaskPrivateState2(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.ironMaskPrivateState2[slot];
    }

    int moblinSwordState(int slot) {
        return runtime.moblinSwordMotion.state(slot);
    }

    int moblinSwordTransitionCountdown(int slot) {
        return runtime.moblinSwordMotion.transitionCountdown(slot);
    }

    int moblinSwordPrivateCountdown1(int slot) {
        return runtime.moblinSwordMotion.privateCountdown1(slot);
    }

    int moblinSwordPrivateState3(int slot) {
        return runtime.moblinSwordMotion.privateState3(slot);
    }

    int moblinSwordDirection(int slot) {
        return runtime.moblinSwordMotion.direction(slot);
    }

    int moblinSwordSpeedX(int slot) {
        return runtime.moblinSwordMotion.speedX(slot);
    }

    int swordMoblinAlertingSoundCounter() {
        return runtime.swordMoblinAlertingSoundCounter;
    }

    int tektiteState(int slot) {
        return runtime.tektiteMotion.state(slot);
    }

    int tektiteTransitionCountdown(int slot) {
        return runtime.tektiteMotion.transitionCountdown(slot);
    }

    int tektiteSpeedX(int slot) {
        return runtime.tektiteMotion.speedX(slot);
    }

    int tektiteSpeedY(int slot) {
        return runtime.tektiteMotion.speedY(slot);
    }

    int pairoddState(int slot) {
        return runtime.pairoddMotion.state(slot);
    }

    int pairoddTransitionCountdown(int slot) {
        return runtime.pairoddMotion.transitionCountdown(slot);
    }

    int pairoddDirection(int slot) {
        return runtime.pairoddMotion.direction(slot);
    }

    int pairoddProjectileSpeedX(int slot) {
        return runtime.pairoddProjectileMotion.speedX(slot);
    }

    int pairoddProjectileSpeedY(int slot) {
        return runtime.pairoddProjectileMotion.speedY(slot);
    }

    int pairoddProjectileDirection(int slot) {
        return runtime.pairoddProjectileMotion.direction(slot);
    }

    int enemyProjectileSpeedX(int slot) {
        return runtime.enemyProjectileMotion.speedX(slot);
    }

    int enemyProjectileSpeedY(int slot) {
        return runtime.enemyProjectileMotion.speedY(slot);
    }

    int enemyProjectileTransitionCountdown(int slot) {
        return runtime.enemyProjectileMotion.transitionCountdown(slot);
    }

    int laserDirection(int slot) {
        return runtime.laserMotion.direction(slot);
    }

    int laserParentTransitionCountdown(int slot) {
        return runtime.laserMotion.parentTransitionCountdown(slot);
    }

    int laserSpeedX(int slot) {
        return runtime.laserMotion.speedX(slot);
    }

    int laserSpeedY(int slot) {
        return runtime.laserMotion.speedY(slot);
    }

    List<TransientVfxRequest> transientVfxRequests() {
        return List.copyOf(runtime.transientVfxRequests);
    }

    List<BoomerangObjectRequest> boomerangObjectRequests() {
        return List.copyOf(runtime.boomerangObjectRequests);
    }

    List<MagicRodObjectRequest> magicRodObjectRequests() {
        return List.copyOf(runtime.magicRodObjectRequests);
    }

    List<MagicPowderObjectRequest> magicPowderObjectRequests() {
        return List.copyOf(runtime.magicPowderObjectRequests);
    }

    int physicsFlags(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.enemyPhysicsFlags[slot];
    }

    int tektiteSpeedZ(int slot) {
        return runtime.tektiteMotion.speedZ(slot);
    }

    int leeverState(int slot) {
        return runtime.leeverMotion.state(slot);
    }

    int leeverTransitionCountdown(int slot) {
        return runtime.leeverMotion.transitionCountdown(slot);
    }

    int leeverSpeedX(int slot) {
        return runtime.leeverMotion.speedX(slot);
    }

    int leeverSpeedY(int slot) {
        return runtime.leeverMotion.speedY(slot);
    }

    int antiFairySpeedX(int slot) {
        return runtime.antiFairyMotion.speedX(slot);
    }

    int antiFairySpeedY(int slot) {
        return runtime.antiFairyMotion.speedY(slot);
    }

    int starSpeedX(int slot) {
        return runtime.starMotion.speedX(slot);
    }

    int starSpeedY(int slot) {
        return runtime.starMotion.speedY(slot);
    }

    int blooperState(int slot) {
        return runtime.blooperMotion.state(slot);
    }

    int blooperTransitionCountdown(int slot) {
        return runtime.blooperMotion.transitionCountdown(slot);
    }

    int blooperDirection(int slot) {
        return runtime.blooperMotion.direction(slot);
    }

    int blooperSpeedX(int slot) {
        return runtime.blooperMotion.speedX(slot);
    }

    int blooperSpeedY(int slot) {
        return runtime.blooperMotion.speedY(slot);
    }

    int blooperPrivateCountdown3(int slot) {
        return runtime.blooperMotion.privateCountdown3(slot);
    }

    int pincerState(int slot) {
        return runtime.pincerMotion.state(slot);
    }

    int pincerHoleX(int slot) {
        return runtime.pincerMotion.holeX(slot);
    }

    int pincerHoleY(int slot) {
        return runtime.pincerMotion.holeY(slot);
    }

    int pincerLungeVariant(int slot) {
        return runtime.pincerMotion.lungeVariant(slot);
    }

    int pincerSpeedX(int slot) {
        return runtime.pincerMotion.speedX(slot);
    }

    int pincerSpeedY(int slot) {
        return runtime.pincerMotion.speedY(slot);
    }

    List<PincerBodyOam.Entry> pincerBodyOam() {
        return List.copyOf(runtime.pincerBodyOam);
    }

    int sparkPrivateState1(int slot) {
        return runtime.sparkMotion.privateState1(slot);
    }

    int sparkPrivateState2(int slot) {
        return runtime.sparkMotion.privateState2(slot);
    }

    int sparkTransitionCountdown(int slot) {
        return runtime.sparkMotion.transitionCountdown(slot);
    }

    int sparkSpeedX(int slot) {
        return runtime.sparkMotion.speedX(slot);
    }

    int sparkSpeedY(int slot) {
        return runtime.sparkMotion.speedY(slot);
    }

    int zolState(int slot) {
        return runtime.zolGelMotion.state(slot);
    }

    int zolTransitionCountdown(int slot) {
        return runtime.zolGelMotion.transitionCountdown(slot);
    }

    int zolSpeedX(int slot) {
        return runtime.zolGelMotion.speedX(slot);
    }

    int zolSpeedY(int slot) {
        return runtime.zolGelMotion.speedY(slot);
    }

    int zolSpeedZ(int slot) {
        return runtime.zolGelMotion.speedZ(slot);
    }

    int zolPrivateCountdown1(int slot) {
        return runtime.zolGelMotion.privateCountdown1(slot);
    }

    int hidingZolState(int slot) {
        return runtime.hidingZolMotion.state(slot);
    }

    int hidingZolTransitionCountdown(int slot) {
        return runtime.hidingZolMotion.transitionCountdown(slot);
    }

    int hidingZolPrivateState1(int slot) {
        return runtime.hidingZolMotion.privateState1(slot);
    }

    int hidingZolSpeedX(int slot) {
        return runtime.hidingZolMotion.speedX(slot);
    }

    int hidingZolSpeedY(int slot) {
        return runtime.hidingZolMotion.speedY(slot);
    }

    int hidingZolSpeedZ(int slot) {
        return runtime.hidingZolMotion.speedZ(slot);
    }

    int spikeTrapState(int slot) {
        return runtime.spikeTrapMotion.state(slot);
    }

    int spikeTrapTransitionCountdown(int slot) {
        return runtime.spikeTrapMotion.transitionCountdown(slot);
    }

    int spikeTrapDirection(int slot) {
        return runtime.spikeTrapMotion.direction(slot);
    }

    int spikeTrapSpeedX(int slot) {
        return runtime.spikeTrapMotion.speedX(slot);
    }

    int spikeTrapSpeedY(int slot) {
        return runtime.spikeTrapMotion.speedY(slot);
    }

    int spikeTrapPrivateState1(int slot) {
        return runtime.spikeTrapMotion.privateState1(slot);
    }

    int spikeTrapPrivateState2(int slot) {
        return runtime.spikeTrapMotion.privateState2(slot);
    }

    int waterTektiteState(int slot) {
        return runtime.waterTektiteMotion.state(slot);
    }

    int waterTektiteTransitionCountdown(int slot) {
        return runtime.waterTektiteMotion.transitionCountdown(slot);
    }

    int waterTektiteSpeedX(int slot) {
        return runtime.waterTektiteMotion.speedX(slot);
    }

    int waterTektiteSpeedY(int slot) {
        return runtime.waterTektiteMotion.speedY(slot);
    }

    int waterTektitePrivateState1(int slot) {
        return runtime.waterTektiteMotion.privateState1(slot);
    }

    int waterTektitePrivateState2(int slot) {
        return runtime.waterTektiteMotion.privateState2(slot);
    }

    int stalfosState(int slot) {
        return runtime.stalfosAggressiveMotion.state(slot);
    }

    int stalfosTransitionCountdown(int slot) {
        return runtime.stalfosAggressiveMotion.transitionCountdown(slot);
    }

    int stalfosSpeedX(int slot) {
        return runtime.stalfosAggressiveMotion.speedX(slot);
    }

    int stalfosSpeedY(int slot) {
        return runtime.stalfosAggressiveMotion.speedY(slot);
    }

    int stalfosSpeedZ(int slot) {
        return runtime.stalfosAggressiveMotion.speedZ(slot);
    }

    int evasivePrivateState1(int slot) {
        return runtime.stalfosEvasiveMotion.privateState1(slot);
    }

    int evasivePrivateCountdown1(int slot) {
        return runtime.stalfosEvasiveMotion.privateCountdown1(slot);
    }

    int evasiveInertia(int slot) {
        return runtime.stalfosEvasiveMotion.inertia(slot);
    }

    int evasiveSpeedX(int slot) {
        return runtime.stalfosEvasiveMotion.speedX(slot);
    }

    int evasiveSpeedY(int slot) {
        return runtime.stalfosEvasiveMotion.speedY(slot);
    }

    int evasiveSpeedZ(int slot) {
        return runtime.stalfosEvasiveMotion.speedZ(slot);
    }

    int gibdoState(int slot) {
        return runtime.gibdoMotion.state(slot);
    }

    int gibdoSpeedX(int slot) {
        return runtime.gibdoMotion.speedX(slot);
    }

    int gibdoSpeedY(int slot) {
        return runtime.gibdoMotion.speedY(slot);
    }

    int goombaState(int slot) {
        return runtime.goombaMotion.state(slot);
    }

    int goombaTransitionCountdown(int slot) {
        return runtime.enemyTransitionCountdown[slot];
    }

    int goombaSpeedX(int slot) {
        return runtime.goombaMotion.speedX(slot);
    }

    int goombaSpeedY(int slot) {
        return runtime.goombaMotion.speedY(slot);
    }

    int snakeState(int slot) {
        return runtime.snakeMotion.state(slot);
    }

    int snakeTransitionCountdown(int slot) {
        return runtime.enemyTransitionCountdown[slot];
    }

    int snakePrivateCountdown1(int slot) {
        return runtime.snakeMotion.privateCountdown1(slot);
    }

    int snakeSpeedX(int slot) {
        return runtime.snakeMotion.speedX(slot);
    }

    int snakeSpeedY(int slot) {
        return runtime.snakeMotion.speedY(slot);
    }

    int polsVoiceState(int slot) {
        return runtime.polsVoiceMotion.state(slot);
    }

    int polsVoiceTransitionCountdown(int slot) {
        return runtime.enemyTransitionCountdown[slot];
    }

    int polsVoiceSpeedX(int slot) {
        return runtime.polsVoiceMotion.speedX(slot);
    }

    int polsVoiceSpeedY(int slot) {
        return runtime.polsVoiceMotion.speedY(slot);
    }

    int polsVoiceSpeedZ(int slot) {
        return runtime.polsVoiceMotion.speedZ(slot);
    }

    int spikedBeetleState(int slot) {
        return runtime.spikedBeetleMotion.state(slot);
    }

    int spikedBeetleTransitionCountdown(int slot) {
        return runtime.enemyTransitionCountdown[slot];
    }

    int spikedBeetleSpeedX(int slot) {
        return runtime.spikedBeetleMotion.speedX(slot);
    }

    int spikedBeetleSpeedY(int slot) {
        return runtime.spikedBeetleMotion.speedY(slot);
    }

    int wizrobeState(int slot) {
        return runtime.wizrobeMotion.state(slot);
    }

    int wizrobeDirection(int slot) {
        return runtime.wizrobeMotion.direction(slot);
    }

    int wizrobePrivateState1(int slot) {
        return runtime.wizrobeMotion.privateState1(slot);
    }

    int wizrobePrivateCountdown1(int slot) {
        return runtime.wizrobeMotion.privateCountdown1(slot);
    }

    int wizrobeNextSpriteVariant(int slot) {
        return runtime.wizrobeMotion.nextSpriteVariant(slot);
    }

    int wizrobeProjectileSpeedX(int slot) {
        return runtime.wizrobeProjectileMotion.speedX(slot);
    }

    int wizrobeProjectileSpeedY(int slot) {
        return runtime.wizrobeProjectileMotion.speedY(slot);
    }

    int wizrobeProjectileDirection(int slot) {
        return runtime.wizrobeProjectileMotion.direction(slot);
    }

    int peaHatState(int slot) {
        return runtime.peaHatMotion.state(slot);
    }

    int peaHatSlowTransitionCountdown(int slot) {
        return runtime.peaHatMotion.slowTransitionCountdown(slot);
    }

    int peaHatPrivateState1(int slot) {
        return runtime.peaHatMotion.privateState1(slot);
    }

    int peaHatPrivateState2(int slot) {
        return runtime.peaHatMotion.privateState2(slot);
    }

    int peaHatPrivateState3(int slot) {
        return runtime.peaHatMotion.privateState3(slot);
    }

    int peaHatPrivateState4(int slot) {
        return runtime.peaHatMotion.privateState4(slot);
    }

    int peaHatSpeedX(int slot) {
        return runtime.peaHatMotion.speedX(slot);
    }

    int peaHatSpeedY(int slot) {
        return runtime.peaHatMotion.speedY(slot);
    }

    int armosState(int slot) {
        return runtime.armosMotion.state(slot);
    }

    int armosTransitionCountdown(int slot) {
        return runtime.armosMotion.transitionCountdown(slot);
    }

    int armosSpeedX(int slot) {
        return runtime.armosMotion.speedX(slot);
    }

    int armosSpeedY(int slot) {
        return runtime.armosMotion.speedY(slot);
    }

    int armosKnightState(int slot) {
        return runtime.armosKnightMotion.state(slot);
    }

    int armosKnightTransitionCountdown(int slot) {
        return transitionCountdown(slot);
    }

    int armosKnightSpeedX(int slot) {
        return runtime.armosKnightMotion.speedX(slot);
    }

    int armosKnightSpeedY(int slot) {
        return runtime.armosKnightMotion.speedY(slot);
    }

    int armosKnightSpeedZ(int slot) {
        return runtime.armosKnightMotion.speedZ(slot);
    }

    int armosKnightPrivateCountdown1(int slot) {
        return runtime.armosKnightMotion.privateCountdown1(slot);
    }

    int armosKnightInertia(int slot) {
        return runtime.armosKnightMotion.inertia(slot);
    }

    int ghiniTransitionCountdown(int slot) {
        return runtime.ghiniMotion.transitionCountdown(slot);
    }

    int ghiniPrivateCountdown1(int slot) {
        return runtime.ghiniMotion.privateCountdown1(slot);
    }

    int ghiniTargetXDirection(int slot) {
        return runtime.ghiniMotion.targetXDirection(slot);
    }

    int ghiniTargetYDirection(int slot) {
        return runtime.ghiniMotion.targetYDirection(slot);
    }

    int ghiniSpeedX(int slot) {
        return runtime.ghiniMotion.speedX(slot);
    }

    int ghiniSpeedY(int slot) {
        return runtime.ghiniMotion.speedY(slot);
    }

    int hardHatSpeedX(int slot) {
        return runtime.hardHatMotion.speedX(slot);
    }

    int hardHatSpeedY(int slot) {
        return runtime.hardHatMotion.speedY(slot);
    }

    int butterflyPrivateStateX(int slot) {
        return runtime.butterflyMotion.privateStateX(slot);
    }

    int butterflyPrivateStateY(int slot) {
        return runtime.butterflyMotion.privateStateY(slot);
    }

    int transitionCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.enemyTransitionCountdown[slot];
    }

    int dogState(int slot) {
        runtime.validateEntitySlot(slot);
        if (!runtime.isLoadedEntityOfType(slot, runtime.ENTITY_DOG)) {
            throw new IllegalArgumentException("Entity slot does not contain a dog: " + slot);
        }
        return runtime.dogMotion.state(slot);
    }

    int witchRatState(int slot) {
        runtime.validateEntitySlot(slot);
        if (!runtime.isLoadedEntityOfType(slot, runtime.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.witchRatMotion.state(slot);
    }

    int witchRatTransitionCountdown(int slot) {
        runtime.validateEntitySlot(slot);
        if (!runtime.isLoadedEntityOfType(slot, runtime.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.enemyTransitionCountdown[slot];
    }

    int witchRatSpeedZ(int slot) {
        runtime.validateEntitySlot(slot);
        if (!runtime.isLoadedEntityOfType(slot, runtime.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.witchRatMotion.speedZ(slot);
    }

    int stunnedCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.enemyStunnedCountdown[slot];
    }

    int dyingCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.dyingCountdown[slot];
    }

    boolean powerRecoilDeath(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.powerRecoilDeath[slot];
    }

    int enemyHealth(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.enemyHealth[slot];
    }

    int enemyFlashCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.enemyFlashCountdown[slot];
    }

    int enemyIgnoreHitsCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.enemyIgnoreHitsCountdown[slot];
    }

    int enemyRecoilSpeedXForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.enemyRecoilMotion.recoilSpeedX(slot);
    }

    int enemyRecoilSpeedYForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.enemyRecoilMotion.recoilSpeedY(slot);
    }

    int ledgeTransitionTimer(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.ledgeTransitionTimer[slot] & 0xFF;
    }

    int fallingTargetX(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.fallingTargetX[slot];
    }

    int fallingTargetY(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.fallingTargetY[slot];
    }

    int fallingVisualYOffset(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.fallingVisualYOffset[slot];
    }

    int musicalNoteInertiaForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.musicalNoteInertia[slot];
    }

    int hitboxFlagsForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.enemyHitboxFlags[slot];
    }

    int geniePrivateState1ForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.geniePrivateState1[slot] & 0xFF;
    }

    int genieActiveStateForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.genieActiveState[slot] & 0xFF;
    }

    int geniePrivateState3ForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.geniePrivateState3[slot] & 0xFF;
    }

    int geniePrivateCountdown1ForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.geniePrivateCountdown1[slot] & 0xFF;
    }

    int geniePrivateCountdown2ForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.geniePrivateCountdown2[slot] & 0xFF;
    }

    int genieSpeedXForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.genieSpeedX[slot] & 0xFF;
    }

    int genieSpeedZForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.genieSpeedZ[slot] & 0xFF;
    }

    int geniePrivateState4ForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.geniePrivateState4[slot] & 0xFF;
    }

    int rollingBonesBarStateForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.rollingBonesMotion.barState(slot);
    }

    int rollingBonesBarSpeedXForTest(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.rollingBonesMotion.barSpeedX(slot);
    }

    int keyDropTransitionCountdown(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int colorShellState(int slot) {
        return runtime.colorShellMotion.state(slot);
    }

    int bombiteState(int slot) {
        return runtime.bombiteMotion.state(slot);
    }

    int moldormHeadSpriteVariant(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.moldormMotion.headSpriteVariant(slot);
    }

    int moldormTransitionCountdown(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int moldormPrivateCountdown2(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.moldormMotion.privateCountdown2(slot);
    }

    int moldormDestructionState(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.bossDeathProducerState[slot];
    }

    int moldormInertia(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.moldormMotion.inertia(slot);
    }

    int bombitePrivateCountdown1(int slot) {
        return runtime.bombPrivateCountdown1[slot];
    }

    int bombiteSpeedX(int slot) {
        return runtime.bombiteMotion.speedX(slot);
    }

    int bombiteSpeedY(int slot) {
        return runtime.bombiteMotion.speedY(slot);
    }

    int colorShellPhysicsFlags(int slot) {
        return runtime.colorShellMotion.physicsFlags(slot);
    }

    int secretSeashellPrivateState3(int slot) {
        return runtime.secretSeashellMotion.privateState3(slot);
    }

    int secretSeashellPrivateState4(int slot) {
        return runtime.secretSeashellMotion.privateState4(slot);
    }

    int secretSeashellPrivateCountdown1(int slot) {
        return runtime.secretSeashellMotion.privateCountdown1(slot);
    }

    int secretSeashellSlowTransitionCountdown(int slot) {
        return runtime.secretSeashellMotion.slowTransitionCountdown(slot);
    }

    int secretSeashellSpeedX(int slot) {
        return runtime.secretSeashellMotion.speedX(slot);
    }

    int secretSeashellSpeedY(int slot) {
        return runtime.secretSeashellMotion.speedY(slot);
    }

    int secretSeashellSpeedZ(int slot) {
        return runtime.secretSeashellMotion.speedZ(slot);
    }

    int fishState(int slot) {
        return runtime.fishMotion.state(slot);
    }

    int fishSpeedX(int slot) {
        return runtime.fishMotion.speedX(slot);
    }

    int fishSpeedZ(int slot) {
        return runtime.fishMotion.speedZ(slot);
    }

    int fishTransitionCountdown(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int crowState(int slot) {
        return runtime.crowMotion.state(slot);
    }

    int crowSpeedX(int slot) {
        return runtime.crowMotion.speedX(slot);
    }

    int crowSpeedY(int slot) {
        return runtime.crowMotion.speedY(slot);
    }

    int crowSpeedZ(int slot) {
        return runtime.crowMotion.speedZ(slot);
    }

    int crowTransitionCountdown(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int booBuddyState(int slot) {
        return runtime.booBuddyMotion.state(slot);
    }

    int booBuddySpeedX(int slot) {
        return runtime.booBuddyMotion.speedX(slot);
    }

    int booBuddySpeedY(int slot) {
        return runtime.booBuddyMotion.speedY(slot);
    }

    int booBuddyTransitionCountdown(int slot) {
        runtime.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int booBuddyTriggerCount() {
        return runtime.booBuddyTriggerCount;
    }

    int enemyRecoilSpeedX(int slot) {
        return runtime.enemyRecoilMotion.recoilSpeedX(slot);
    }

    int enemyRecoilSpeedY(int slot) {
        return runtime.enemyRecoilMotion.recoilSpeedY(slot);
    }

    boolean enemyRecoilActive(int slot) {
        return runtime.enemyRecoilMotion.isActive(slot);
    }

    boolean polsVoiceBalladOcarinaTrigger() {
        return runtime.linkPlayingOcarinaCountdown == 0x01
            && (runtime.ocarinaSongFlags & 0x04) != 0
            && runtime.selectedSongIndex == 0;
    }

    boolean shouldDisappear(RoomEntity entity) {
        return runtime.slowTimerInitialized[entity.slot()]
            && isCommonDroppableType(entity.type())
            && entity.type() != runtime.ENTITY_HIDING_SLIME_KEY
            // DroppableDisappearIfNeeded is after the hidden-state helper in
            // every shared drop handler. A hidden item therefore must not be
            // unloaded merely because its global fade timer reached zero.
            && runtime.droppablePrivateState3[entity.slot()] == 0
            && runtime.slowTransitionCountdown[entity.slot()] < 0x1C;
    }

    boolean isDisabledFollower(int type) {
        return switch (type) {
            case FollowingNpcEntitySpawner.ENTITY_ROOSTER -> !runtime.followingNpcState.roosterFollowing();
            case FollowingNpcEntitySpawner.ENTITY_GHOST ->
                runtime.followingNpcState.ghostFollowingState() != 1;
            case FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE ->
                !runtime.followingNpcState.marinFollowing();
            case FollowingNpcEntitySpawner.ENTITY_BOW_WOW ->
                !runtime.followingNpcState.bowWowFollowing();
            default -> false;
        };
    }

    boolean entityHandlerInteractive(boolean creditsGameplay) {
        if (!creditsGameplay
            && (!runtime.gameplayWorld || runtime.transitionSequenceCounter != 0x04)) {
            return false;
        }
        return !runtime.dialogActive
            && !runtime.inventoryAppearing
            && !runtime.witchGotItemPresentationActive()
            && !runtime.roomTransitionActive;
    }

    boolean entityTimersInteractive() {
        return !runtime.dialogActive && !runtime.inventoryAppearing && runtime.linkPlayingOcarinaCountdown == 0;
    }
}
