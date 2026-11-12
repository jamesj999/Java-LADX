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
import linksawakening.world.entity.motion.*;

/** Read-only views over ROM slot state for gameplay and diagnostics. */
public final class EntityEnemyInspection {
    private final EntityEnemyInspectionServices runtime;

    EntityEnemyInspection(EntityEnemyInspectionServices runtime) {
        this.runtime = runtime;
    }

    int octorokState(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .state(slot);
    }

    int octorokTransitionCountdown(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .transitionCountdown(slot);
    }

    int octorokDirection(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .direction(slot);
    }

    int octorokSpeedX(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .speedX(slot);
    }

    int octorokSpeedY(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .speedY(slot);
    }

    int ironMaskState(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .state(slot);
    }

    int ironMaskTransitionCountdown(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .transitionCountdown(slot);
    }

    int ironMaskDirection(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .direction(slot);
    }

    int ironMaskSpeedX(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .speedX(slot);
    }

    int ironMaskSpeedY(int slot) {
        return runtime.motions()
                .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .speedY(slot);
    }

    int ironMaskPrivateState2(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .maskRemovalState(slot);
    }

    int moblinSwordState(int slot) {
        return runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .state(slot);
    }

    int moblinSwordTransitionCountdown(int slot) {
        return runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .transitionCountdown(slot);
    }

    int moblinSwordPrivateCountdown1(int slot) {
        return runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .privateCountdown1(slot);
    }

    int moblinSwordPrivateState3(int slot) {
        return runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .privateState3(slot);
    }

    int moblinSwordDirection(int slot) {
        return runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .direction(slot);
    }

    int moblinSwordSpeedX(int slot) {
        return runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .speedX(slot);
    }

    int swordMoblinAlertingSoundCounter() {
        return runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .readSwordMoblinAlertingSoundCounter();
    }

    int tektiteState(int slot) {
        return runtime.motions().get(TektiteMotion.class, TektiteMotion::new).state(slot);
    }

    int tektiteTransitionCountdown(int slot) {
        return runtime.motions().get(TektiteMotion.class, TektiteMotion::new).transitionCountdown(slot);
    }

    int tektiteSpeedX(int slot) {
        return runtime.motions().get(TektiteMotion.class, TektiteMotion::new).speedX(slot);
    }

    int tektiteSpeedY(int slot) {
        return runtime.motions().get(TektiteMotion.class, TektiteMotion::new).speedY(slot);
    }

    int enemyProjectileSpeedX(int slot) {
        return runtime.motions()
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .speedX(slot);
    }

    int enemyProjectileSpeedY(int slot) {
        return runtime.motions()
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .speedY(slot);
    }

    int enemyProjectileTransitionCountdown(int slot) {
        return runtime.motions()
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .transitionCountdown(slot);
    }

    int laserDirection(int slot) {
        return runtime.motions().get(LaserMotion.class, LaserMotion::new).direction(slot);
    }

    int laserParentTransitionCountdown(int slot) {
        return runtime.motions().get(LaserMotion.class, LaserMotion::new).parentTransitionCountdown(slot);
    }

    int laserSpeedX(int slot) {
        return runtime.motions().get(LaserMotion.class, LaserMotion::new).speedX(slot);
    }

    int laserSpeedY(int slot) {
        return runtime.motions().get(LaserMotion.class, LaserMotion::new).speedY(slot);
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
        return runtime.motions().get(TektiteMotion.class, TektiteMotion::new).speedZ(slot);
    }

    int leeverState(int slot) {
        return runtime.motions().get(LeeverMotion.class, LeeverMotion::new).state(slot);
    }

    int leeverTransitionCountdown(int slot) {
        return runtime.motions().get(LeeverMotion.class, LeeverMotion::new).transitionCountdown(slot);
    }

    int leeverSpeedX(int slot) {
        return runtime.motions().get(LeeverMotion.class, LeeverMotion::new).speedX(slot);
    }

    int leeverSpeedY(int slot) {
        return runtime.motions().get(LeeverMotion.class, LeeverMotion::new).speedY(slot);
    }

    int antiFairySpeedX(int slot) {
        return runtime.motions()
                .get(AntiFairyMotion.class, AntiFairyMotion::new)
                .speedX(slot);
    }

    int antiFairySpeedY(int slot) {
        return runtime.motions()
                .get(AntiFairyMotion.class, AntiFairyMotion::new)
                .speedY(slot);
    }

    int starSpeedX(int slot) {
        return runtime.motions().get(StarMotion.class, StarMotion::new).speedX(slot);
    }

    int starSpeedY(int slot) {
        return runtime.motions().get(StarMotion.class, StarMotion::new).speedY(slot);
    }

    int blooperState(int slot) {
        return runtime.motions().get(BlooperMotion.class, BlooperMotion::new).state(slot);
    }

    int blooperTransitionCountdown(int slot) {
        return runtime.motions().get(BlooperMotion.class, BlooperMotion::new).transitionCountdown(slot);
    }

    int blooperDirection(int slot) {
        return runtime.motions().get(BlooperMotion.class, BlooperMotion::new).direction(slot);
    }

    int blooperSpeedX(int slot) {
        return runtime.motions().get(BlooperMotion.class, BlooperMotion::new).speedX(slot);
    }

    int blooperSpeedY(int slot) {
        return runtime.motions().get(BlooperMotion.class, BlooperMotion::new).speedY(slot);
    }

    int blooperPrivateCountdown3(int slot) {
        return runtime.motions().get(BlooperMotion.class, BlooperMotion::new).privateCountdown3(slot);
    }

    int pincerState(int slot) {
        return runtime.motions().get(PincerMotion.class, PincerMotion::new).state(slot);
    }

    int pincerHoleX(int slot) {
        return runtime.motions().get(PincerMotion.class, PincerMotion::new).holeX(slot);
    }

    int pincerHoleY(int slot) {
        return runtime.motions().get(PincerMotion.class, PincerMotion::new).holeY(slot);
    }

    int pincerLungeVariant(int slot) {
        return runtime.motions().get(PincerMotion.class, PincerMotion::new).lungeVariant(slot);
    }

    int pincerSpeedX(int slot) {
        return runtime.motions().get(PincerMotion.class, PincerMotion::new).speedX(slot);
    }

    int pincerSpeedY(int slot) {
        return runtime.motions().get(PincerMotion.class, PincerMotion::new).speedY(slot);
    }

    List<PincerBodyOam.Entry> pincerBodyOam() {
        return List.copyOf(runtime.slotProperties().readPincerBodyOam());
    }

    int sparkPrivateState1(int slot) {
        return runtime.motions().get(SparkMotion.class, SparkMotion::new).privateState1(slot);
    }

    int sparkPrivateState2(int slot) {
        return runtime.motions().get(SparkMotion.class, SparkMotion::new).privateState2(slot);
    }

    int sparkTransitionCountdown(int slot) {
        return runtime.motions().get(SparkMotion.class, SparkMotion::new).transitionCountdown(slot);
    }

    int sparkSpeedX(int slot) {
        return runtime.motions().get(SparkMotion.class, SparkMotion::new).speedX(slot);
    }

    int sparkSpeedY(int slot) {
        return runtime.motions().get(SparkMotion.class, SparkMotion::new).speedY(slot);
    }

    int zolState(int slot) {
        return runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).state(slot);
    }

    int zolTransitionCountdown(int slot) {
        return runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).transitionCountdown(slot);
    }

    int zolSpeedX(int slot) {
        return runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).speedX(slot);
    }

    int zolSpeedY(int slot) {
        return runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).speedY(slot);
    }

    int zolSpeedZ(int slot) {
        return runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).speedZ(slot);
    }

    int zolPrivateCountdown1(int slot) {
        return runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).privateCountdown1(slot);
    }

    int hidingZolState(int slot) {
        return runtime.motions()
                .get(HidingZolMotion.class, HidingZolMotion::new)
                .state(slot);
    }

    int hidingZolTransitionCountdown(int slot) {
        return runtime.motions()
                .get(HidingZolMotion.class, HidingZolMotion::new)
                .transitionCountdown(slot);
    }

    int hidingZolPrivateState1(int slot) {
        return runtime.motions()
                .get(HidingZolMotion.class, HidingZolMotion::new)
                .privateState1(slot);
    }

    int hidingZolSpeedX(int slot) {
        return runtime.motions()
                .get(HidingZolMotion.class, HidingZolMotion::new)
                .speedX(slot);
    }

    int hidingZolSpeedY(int slot) {
        return runtime.motions()
                .get(HidingZolMotion.class, HidingZolMotion::new)
                .speedY(slot);
    }

    int hidingZolSpeedZ(int slot) {
        return runtime.motions()
                .get(HidingZolMotion.class, HidingZolMotion::new)
                .speedZ(slot);
    }

    int spikeTrapState(int slot) {
        return runtime.motions()
                .get(SpikeTrapMotion.class, SpikeTrapMotion::new)
                .state(slot);
    }

    int spikeTrapTransitionCountdown(int slot) {
        return runtime.motions()
                .get(SpikeTrapMotion.class, SpikeTrapMotion::new)
                .transitionCountdown(slot);
    }

    int spikeTrapDirection(int slot) {
        return runtime.motions()
                .get(SpikeTrapMotion.class, SpikeTrapMotion::new)
                .direction(slot);
    }

    int spikeTrapSpeedX(int slot) {
        return runtime.motions()
                .get(SpikeTrapMotion.class, SpikeTrapMotion::new)
                .speedX(slot);
    }

    int spikeTrapSpeedY(int slot) {
        return runtime.motions()
                .get(SpikeTrapMotion.class, SpikeTrapMotion::new)
                .speedY(slot);
    }

    int spikeTrapPrivateState1(int slot) {
        return runtime.motions()
                .get(SpikeTrapMotion.class, SpikeTrapMotion::new)
                .privateState1(slot);
    }

    int spikeTrapPrivateState2(int slot) {
        return runtime.motions()
                .get(SpikeTrapMotion.class, SpikeTrapMotion::new)
                .privateState2(slot);
    }

    int waterTektiteState(int slot) {
        return runtime.motions()
                .get(WaterTektiteMotion.class, WaterTektiteMotion::new)
                .state(slot);
    }

    int waterTektiteTransitionCountdown(int slot) {
        return runtime.motions()
                .get(WaterTektiteMotion.class, WaterTektiteMotion::new)
                .transitionCountdown(slot);
    }

    int waterTektiteSpeedX(int slot) {
        return runtime.motions()
                .get(WaterTektiteMotion.class, WaterTektiteMotion::new)
                .speedX(slot);
    }

    int waterTektiteSpeedY(int slot) {
        return runtime.motions()
                .get(WaterTektiteMotion.class, WaterTektiteMotion::new)
                .speedY(slot);
    }

    int waterTektitePrivateState1(int slot) {
        return runtime.motions()
                .get(WaterTektiteMotion.class, WaterTektiteMotion::new)
                .privateState1(slot);
    }

    int waterTektitePrivateState2(int slot) {
        return runtime.motions()
                .get(WaterTektiteMotion.class, WaterTektiteMotion::new)
                .privateState2(slot);
    }

    int stalfosState(int slot) {
        return runtime.motions()
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .state(slot);
    }

    int stalfosTransitionCountdown(int slot) {
        return runtime.motions()
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .transitionCountdown(slot);
    }

    int stalfosSpeedX(int slot) {
        return runtime.motions()
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .speedX(slot);
    }

    int stalfosSpeedY(int slot) {
        return runtime.motions()
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .speedY(slot);
    }

    int stalfosSpeedZ(int slot) {
        return runtime.motions()
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .speedZ(slot);
    }

    int evasivePrivateState1(int slot) {
        return runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .privateState1(slot);
    }

    int evasivePrivateCountdown1(int slot) {
        return runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .privateCountdown1(slot);
    }

    int evasiveInertia(int slot) {
        return runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .inertia(slot);
    }

    int evasiveSpeedX(int slot) {
        return runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .speedX(slot);
    }

    int evasiveSpeedY(int slot) {
        return runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .speedY(slot);
    }

    int evasiveSpeedZ(int slot) {
        return runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .speedZ(slot);
    }

    int gibdoState(int slot) {
        return runtime.motions().get(GibdoMotion.class, GibdoMotion::new).state(slot);
    }

    int gibdoSpeedX(int slot) {
        return runtime.motions().get(GibdoMotion.class, GibdoMotion::new).speedX(slot);
    }

    int gibdoSpeedY(int slot) {
        return runtime.motions().get(GibdoMotion.class, GibdoMotion::new).speedY(slot);
    }

    int goombaState(int slot) {
        return runtime.motions().get(GoombaMotion.class, GoombaMotion::new).state(slot);
    }

    int goombaTransitionCountdown(int slot) {
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int goombaSpeedX(int slot) {
        return runtime.motions().get(GoombaMotion.class, GoombaMotion::new).speedX(slot);
    }

    int goombaSpeedY(int slot) {
        return runtime.motions().get(GoombaMotion.class, GoombaMotion::new).speedY(slot);
    }

    int snakeState(int slot) {
        return runtime.motions().get(SnakeMotion.class, SnakeMotion::new).state(slot);
    }

    int snakeTransitionCountdown(int slot) {
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int snakePrivateCountdown1(int slot) {
        return runtime.motions().get(SnakeMotion.class, SnakeMotion::new).privateCountdown1(slot);
    }

    int snakeSpeedX(int slot) {
        return runtime.motions().get(SnakeMotion.class, SnakeMotion::new).speedX(slot);
    }

    int snakeSpeedY(int slot) {
        return runtime.motions().get(SnakeMotion.class, SnakeMotion::new).speedY(slot);
    }

    int polsVoiceState(int slot) {
        return runtime.motions()
                .get(PolsVoiceMotion.class, PolsVoiceMotion::new)
                .state(slot);
    }

    int polsVoiceTransitionCountdown(int slot) {
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int polsVoiceSpeedX(int slot) {
        return runtime.motions()
                .get(PolsVoiceMotion.class, PolsVoiceMotion::new)
                .speedX(slot);
    }

    int polsVoiceSpeedY(int slot) {
        return runtime.motions()
                .get(PolsVoiceMotion.class, PolsVoiceMotion::new)
                .speedY(slot);
    }

    int polsVoiceSpeedZ(int slot) {
        return runtime.motions()
                .get(PolsVoiceMotion.class, PolsVoiceMotion::new)
                .speedZ(slot);
    }

    int spikedBeetleState(int slot) {
        return runtime.motions()
                .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                .state(slot);
    }

    int spikedBeetleTransitionCountdown(int slot) {
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int spikedBeetleSpeedX(int slot) {
        return runtime.motions()
                .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                .speedX(slot);
    }

    int spikedBeetleSpeedY(int slot) {
        return runtime.motions()
                .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                .speedY(slot);
    }

    int wizrobeState(int slot) {
        return runtime.motions().get(WizrobeMotion.class, WizrobeMotion::new).state(slot);
    }

    int wizrobeDirection(int slot) {
        return runtime.motions().get(WizrobeMotion.class, WizrobeMotion::new).direction(slot);
    }

    int wizrobePrivateState1(int slot) {
        return runtime.motions().get(WizrobeMotion.class, WizrobeMotion::new).privateState1(slot);
    }

    int wizrobePrivateCountdown1(int slot) {
        return runtime.motions().get(WizrobeMotion.class, WizrobeMotion::new).privateCountdown1(slot);
    }

    int wizrobeNextSpriteVariant(int slot) {
        return runtime.motions().get(WizrobeMotion.class, WizrobeMotion::new).nextSpriteVariant(slot);
    }

    int wizrobeProjectileSpeedX(int slot) {
        return runtime.motions()
                .get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .speedX(slot);
    }

    int wizrobeProjectileSpeedY(int slot) {
        return runtime.motions()
                .get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .speedY(slot);
    }

    int wizrobeProjectileDirection(int slot) {
        return runtime.motions()
                .get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .direction(slot);
    }

    int peaHatState(int slot) {
        return runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).state(slot);
    }

    int peaHatSlowTransitionCountdown(int slot) {
        return runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).slowTransitionCountdown(slot);
    }

    int peaHatPrivateState1(int slot) {
        return runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).privateState1(slot);
    }

    int peaHatPrivateState2(int slot) {
        return runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).privateState2(slot);
    }

    int peaHatPrivateState3(int slot) {
        return runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).privateState3(slot);
    }

    int peaHatPrivateState4(int slot) {
        return runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).privateState4(slot);
    }

    int peaHatSpeedX(int slot) {
        return runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).speedX(slot);
    }

    int peaHatSpeedY(int slot) {
        return runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).speedY(slot);
    }

    int armosState(int slot) {
        return runtime.motions().get(ArmosMotion.class, ArmosMotion::new).state(slot);
    }

    int armosTransitionCountdown(int slot) {
        return runtime.motions().get(ArmosMotion.class, ArmosMotion::new).transitionCountdown(slot);
    }

    int armosSpeedX(int slot) {
        return runtime.motions().get(ArmosMotion.class, ArmosMotion::new).speedX(slot);
    }

    int armosSpeedY(int slot) {
        return runtime.motions().get(ArmosMotion.class, ArmosMotion::new).speedY(slot);
    }

    int ghiniTransitionCountdown(int slot) {
        return runtime.motions().get(GhiniMotion.class, GhiniMotion::new).transitionCountdown(slot);
    }

    int ghiniPrivateCountdown1(int slot) {
        return runtime.motions().get(GhiniMotion.class, GhiniMotion::new).privateCountdown1(slot);
    }

    int ghiniTargetXDirection(int slot) {
        return runtime.motions().get(GhiniMotion.class, GhiniMotion::new).targetXDirection(slot);
    }

    int ghiniTargetYDirection(int slot) {
        return runtime.motions().get(GhiniMotion.class, GhiniMotion::new).targetYDirection(slot);
    }

    int ghiniSpeedX(int slot) {
        return runtime.motions().get(GhiniMotion.class, GhiniMotion::new).speedX(slot);
    }

    int ghiniSpeedY(int slot) {
        return runtime.motions().get(GhiniMotion.class, GhiniMotion::new).speedY(slot);
    }

    int hardHatSpeedX(int slot) {
        return runtime.motions().get(HardHatMotion.class, HardHatMotion::new).speedX(slot);
    }

    int hardHatSpeedY(int slot) {
        return runtime.motions().get(HardHatMotion.class, HardHatMotion::new).speedY(slot);
    }

    int butterflyPrivateStateX(int slot) {
        return runtime.motions()
                .get(ButterflyMotion.class, ButterflyMotion::new)
                .privateStateX(slot);
    }

    int butterflyPrivateStateY(int slot) {
        return runtime.motions()
                .get(ButterflyMotion.class, ButterflyMotion::new)
                .privateStateY(slot);
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
        return runtime.motions().get(DogMotion.class, DogMotion::new).state(slot);
    }

    int witchRatState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        if (!runtime.roomInspection().isLoadedEntityOfType(slot, EntityRomConstants.ENTITY_WITCH_RAT)) {
            throw new IllegalArgumentException("Entity slot does not contain a Witch Rat: " + slot);
        }
        return runtime.motions().get(WitchRatMotion.class, WitchRatMotion::new).state(slot);
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
        return runtime.motions().get(WitchRatMotion.class, WitchRatMotion::new).speedZ(slot);
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

    int enemyRecoilSpeedX(int slot) {
        return runtime.motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .recoilSpeedX(slot);
    }

    int enemyRecoilSpeedY(int slot) {
        return runtime.motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .recoilSpeedY(slot);
    }

    boolean enemyRecoilActive(int slot) {
        return runtime.motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .isActive(slot);
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
            case FollowingNpcEntitySpawner.ENTITY_ROOSTER -> !runtime.motions()
                    .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .readFollowingNpcState()
                    .roosterFollowing();
            case FollowingNpcEntitySpawner.ENTITY_GHOST -> runtime.motions()
                            .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                            .readFollowingNpcState()
                            .ghostFollowingState()
                    != 1;
            case FollowingNpcEntitySpawner.ENTITY_MARIN_AT_THE_SHORE -> !runtime.motions()
                    .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                    .readFollowingNpcState()
                    .marinFollowing();
            case FollowingNpcEntitySpawner.ENTITY_BOW_WOW -> !runtime.motions()
                    .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
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
