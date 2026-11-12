package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.ZolGelMotion;

/** Source-ordered persistent slot reset; distinct reset paths remain distinct. */
final class EntityPersistentSlotReset {
    static int clearEntity(EntitySlotLifecycleServices runtime, int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        clearBossState(runtime, slot);
        RoomEntity entity = runtime.slots()[slot];
        clearPrivateState(runtime, slot);
        if (!entity.loaded()) {
            return 0;
        }
        clearSlotTables(runtime, slot);
        clearWeaponState(runtime, slot, entity);
        clearRoamingMotions(runtime, slot);
        clearRemainingMotions(runtime, slot);
        if (runtime.liftThrow().readLiftedEntitySlot() == slot) {
            runtime.liftThrow().writeLiftedEntitySlot(-1);
            runtime.liftThrow().writeLiftedCarryState(0);
            runtime.liftThrow().writeLiftedEffectiveDirection(0);
        }
        runtime.slots()[slot] = RoomEntity.disabled(slot);
        runtime.slotProperties().updatePincerBodyOam();
        return entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() < 8 ? 1 << entity.sourceLoadOrder() : 0;
    }

    private static void clearBossState(EntitySlotLifecycleServices runtime, int slot) {
        runtime.slimeEye().clear(slot);
        runtime.pickups().resetEnemyDropState(slot);
        runtime.motions()
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .clear(slot);
        runtime.motions().get(SideViewPotMotion.class, SideViewPotMotion::new).clear(slot);
        runtime.motions().get(GenieMotion.class, GenieMotion::new).clear(slot);
        if (runtime.genie().genieControllerSlot() == slot) {
            runtime.genie().setGenieControllerSlot(-1);
        }
        runtime.genie().setGenieActiveState(slot, 0);
        runtime.genie().setGeniePrivateState1(slot, 0);
        runtime.genie().setGeniePrivateState2(slot, 0);
        runtime.genie().setGeniePrivateState3(slot, 0);
        runtime.genie().setGeniePrivateState4(slot, 0);
        runtime.genie().setGeniePrivateCountdown1(slot, 0);
        runtime.genie().setGeniePrivateCountdown2(slot, 0);
        runtime.genie().setGenieDirection(slot, 0);
        runtime.genie().setGenieDeathInitialized(slot, false);
        runtime.genie().setGenieSpeedX(slot, 0);
        runtime.genie().setGenieSpeedY(slot, 0);
        runtime.genie().setGenieSpeedZ(slot, 0);
        runtime.genie().setGenieSpeedXAccumulator(slot, 0);
        runtime.genie().setGenieSpeedYAccumulator(slot, 0);
        runtime.genie().setGenieSpeedZAccumulator(slot, 0);
    }

    private static void clearPrivateState(EntitySlotLifecycleServices runtime, int slot) {
        runtime.setBombFinalPresentationPending(slot, false);
        runtime.combatTables().writeEnemyHitboxFlags(slot, 0);
        runtime.bombInteractions().writePlayerArrowBombArrow(slot, false);
        runtime.bombInteractions().writeBombPrivateCountdown1(slot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown3(slot, 0);
        runtime.projectiles().writeMagicPowderState(slot, 0);
        runtime.projectiles().writeMagicPowderPrivateState4(slot, 0);
        runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .clear(slot);
        runtime.physicsTables().writeEntityPrivateState4(slot, 0);
        runtime.physicsTables().writeEntityInertia(slot, 0);
        runtime.projectiles().writeMusicalNoteInertia(slot, 0);
        runtime.projectiles().writeMusicalNoteSpeedX(slot, 0);
        runtime.projectiles().writeMusicalNoteSpeedY(slot, 0);
        runtime.physicsTables().writeEntityUnknownJ(slot, 0);
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskRemovalState(slot, 0);
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .setMaskSourceHookshotSlot(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashCountdown(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashSourceVariant(slot, 0);
        runtime.liftThrow().writeLiftableRockSmashActive(slot, false);
        if (runtime.bombInteractions().readLatestDroppedBombEntityIndex() == slot) {
            runtime.bombInteractions().writeLatestDroppedBombEntityIndex(-1);
        }
        if (runtime.projectiles().readLatestShotArrowEntityIndex() == slot) {
            runtime.projectiles().writeLatestShotArrowEntityIndex(-1);
        }
        runtime.motions().get(BombiteMotion.class, BombiteMotion::new).clear(slot);
        runtime.motions().get(BlooperMotion.class, BlooperMotion::new).clear(slot);
        runtime.motions().get(PincerMotion.class, PincerMotion::new).clear(slot);
        runtime.motions().get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.motions().get(PokeyMotion.class, PokeyMotion::new).clear(slot);
        runtime.motions().get(ZoraMotion.class, ZoraMotion::new).clear(slot);
        runtime.motions().get(ZombieMotion.class, ZombieMotion::new).clear(slot);
        runtime.motions().get(BuzzBlobMotion.class, BuzzBlobMotion::new).clear(slot);
        runtime.motions().get(SandCrabMotion.class, SandCrabMotion::new).clear(slot);
        runtime.motions().get(DogMotion.class, DogMotion::new).clear(slot);
        runtime.motions().get(UrchinMotion.class, UrchinMotion::new).clear(slot);
        runtime.motions().get(WitchRatMotion.class, WitchRatMotion::new).clear(slot);
        runtime.motions().get(MimicMotion.class, MimicMotion::new).clear(slot);
        runtime.motions().get(MaskedMimicMotion.class, MaskedMimicMotion::new).clear(slot);
        runtime.motions().get(MiniMoldormMotion.class, MiniMoldormMotion::new).clear(slot);
        runtime.motions().get(MoldormMotion.class, MoldormMotion::new).clear(slot);
    }

    private static void clearSlotTables(EntitySlotLifecycleServices runtime, int slot) {
        runtime.physicsTables().writeSlowTransitionCountdown(slot, 0);
        runtime.physicsTables().writeSlowTimerInitialized(slot, false);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(slot, 0);
        runtime.combatTables().writeDyingCountdown(slot, 0);
        runtime.combatTables().writePowerRecoilDeath(slot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, 0);
        runtime.combatTables().writeEnemyHealth(slot, 0);
        runtime.combatTables().writeEnemyHealthGroup(slot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
        runtime.pickups().writeChestSpeedY(slot, 0);
        runtime.pickups().writeChestSpeedYAccumulator(slot, 0);
        runtime.pickups().writeChestInertia(slot, 0);
        runtime.pickups().writeChestItemBySlot(slot, 0);
        runtime.story().writeOwlPrivateCountdown1(slot, 0);
        runtime.physicsTables().writeEntityGroundStatus(slot, 0);
        runtime.physicsTables().writeFallingTargetX(slot, 0);
        runtime.physicsTables().writeFallingTargetY(slot, 0);
        runtime.physicsTables().writeFallingSpeedX(slot, 0);
        runtime.physicsTables().writeFallingSpeedY(slot, 0);
        runtime.physicsTables().writeFallingSpeedXAccumulator(slot, 0);
        runtime.physicsTables().writeFallingSpeedYAccumulator(slot, 0);
        runtime.physicsTables().writeFallingVisualYOffset(slot, 0);
        runtime.liftThrow().writeLiftedPhase(slot, 0);
        runtime.liftThrow().writeLiftedSourceDirection(slot, 0);
        runtime.liftThrow().writeLiftedStateInitialized(slot, false);
        runtime.liftThrow().writeThrownDirection(slot, 0xFF);
        runtime.bombInteractions().writeBombDirection(slot, 0xFF);
        runtime.bombInteractions().writeBombPrivateState4(slot, 0);
        runtime.projectiles().writeMagicPowderState(slot, 0);
        runtime.projectiles().writeMagicPowderPrivateState4(slot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown1(slot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown3(slot, 0);
        runtime.bombInteractions().writePlacedBombMotionInitialized(slot, false);
        runtime.liftThrow().writeLedgeTransitionTimer(slot, 0);
        runtime.liftThrow().writeThrownMotionInitialized(slot, false);
    }

    private static void clearWeaponState(EntitySlotLifecycleServices runtime, int slot, RoomEntity entity) {
        runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .clear(slot);
        runtime.motions()
                .get(HookshotBridgeMotion.class, HookshotBridgeMotion::new)
                .clear(slot);
        runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).clear(slot);
        runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .clear(slot);
        runtime.motions().get(SwordBeamMotion.class, SwordBeamMotion::new).clear(slot);
        if (entity.type() == EntityRomConstants.ENTITY_HOOKSHOT_CHAIN) {
            runtime.slotProperties().writeHookshotChainOam(List.of());
        }
    }

    private static void clearRoamingMotions(EntitySlotLifecycleServices runtime, int slot) {
        runtime.physicsTables().writeBaseEntityFlipAttribute(slot, 0);
        runtime.physicsTables().writeEntityOptions1Override(slot, -1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(slot);
        runtime.motions().get(ColorShellMotion.class, ColorShellMotion::new).clear(slot);
        runtime.motions().get(ButterflyMotion.class, ButterflyMotion::new).clear(slot);
        runtime.motions().get(KeeseMotion.class, KeeseMotion::new).clear(slot);
        runtime.motions().get(RoamingEnemyMotion.class, RoamingEnemyMotion::new).clear(slot);
        runtime.motions()
                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                .clear(slot);
        runtime.motions().get(MoblinSwordMotion.class, MoblinSwordMotion::new).clear(slot);
        runtime.motions().get(PlayerArrowMotion.class, PlayerArrowMotion::new).clear(slot);
        runtime.motions().get(TektiteMotion.class, TektiteMotion::new).clear(slot);
        runtime.motions().get(LeeverMotion.class, LeeverMotion::new).clear(slot);
        runtime.motions().get(AntiFairyMotion.class, AntiFairyMotion::new).clear(slot);
        runtime.motions().get(SparkMotion.class, SparkMotion::new).clear(slot);
        runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).clear(slot);
        runtime.motions().get(PokeyMotion.class, PokeyMotion::new).clear(slot);
        runtime.motions().get(HidingZolMotion.class, HidingZolMotion::new).clear(slot);
        runtime.motions().get(StarMotion.class, StarMotion::new).clear(slot);
        runtime.motions().get(BlooperMotion.class, BlooperMotion::new).clear(slot);
        runtime.motions().get(PincerMotion.class, PincerMotion::new).clear(slot);
        runtime.motions().get(SpikeTrapMotion.class, SpikeTrapMotion::new).clear(slot);
        runtime.motions().get(PairoddMotion.class, PairoddMotion::new).clear(slot);
        runtime.motions()
                .get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .clear(slot);
        runtime.motions()
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .clear(slot);
        runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .clear(slot);
        runtime.motions().get(BushCrawlerMotion.class, BushCrawlerMotion::new).clear(slot);
        runtime.motions().get(CuccoMotion.class, CuccoMotion::new).clear(slot);
        runtime.motions().get(GiantGopongaMotion.class, GiantGopongaMotion::new).clear(slot);
        runtime.motions()
                .get(GopongaProjectileMotion.class, GopongaProjectileMotion::new)
                .clear(slot);
        runtime.motions().get(ZoraMotion.class, ZoraMotion::new).clear(slot);
        runtime.motions().get(ZombieMotion.class, ZombieMotion::new).clear(slot);
        runtime.motions().get(BuzzBlobMotion.class, BuzzBlobMotion::new).clear(slot);
        runtime.motions().get(SandCrabMotion.class, SandCrabMotion::new).clear(slot);
        runtime.motions().get(DogMotion.class, DogMotion::new).clear(slot);
        runtime.motions().get(UrchinMotion.class, UrchinMotion::new).clear(slot);
        runtime.motions().get(WitchRatMotion.class, WitchRatMotion::new).clear(slot);
    }

    private static void clearRemainingMotions(EntitySlotLifecycleServices runtime, int slot) {
        runtime.slotProperties().writeWingedSwordAttackThisFrame(slot, false);
        runtime.slotProperties().writeWingedStateTwoThisFrame(slot, false);
        runtime.motions().get(LaserMotion.class, LaserMotion::new).clear(slot);
        runtime.motions().get(WaterTektiteMotion.class, WaterTektiteMotion::new).clear(slot);
        runtime.motions().get(FishMotion.class, FishMotion::new).clear(slot);
        runtime.motions().get(CrowMotion.class, CrowMotion::new).clear(slot);
        runtime.motions().get(BooBuddyMotion.class, BooBuddyMotion::new).clear(slot);
        runtime.motions()
                .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                .clear(slot);
        runtime.motions()
                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                .clear(slot);
        runtime.motions().get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new).clear(slot);
        runtime.motions().get(GibdoMotion.class, GibdoMotion::new).clear(slot);
        runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).clear(slot);
        runtime.motions().get(GoombaMotion.class, GoombaMotion::new).clear(slot);
        runtime.motions().get(SnakeMotion.class, SnakeMotion::new).clear(slot);
        runtime.motions().get(WizrobeMotion.class, WizrobeMotion::new).clear(slot);
        runtime.motions()
                .get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .clear(slot);
        runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).clear(slot);
        runtime.motions().get(ArmosMotion.class, ArmosMotion::new).clear(slot);
        runtime.motions().get(GhiniMotion.class, GhiniMotion::new).clear(slot);
        runtime.motions().get(HardHatMotion.class, HardHatMotion::new).clear(slot);
        runtime.motions().get(MadBomberMotion.class, MadBomberMotion::new).clear(slot);
        runtime.motions().get(FollowingNpcMotion.class, FollowingNpcMotion::new).clear(slot);
        runtime.motions().get(BowWowMotion.class, BowWowMotion::new).clear(slot);
        runtime.motions().get(MoblinKingMotion.class, MoblinKingMotion::new).clear(slot);
        runtime.motions().get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
    }
}
