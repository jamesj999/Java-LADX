package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;

/** Focused behavior using the state owned by EntityBackgroundMechanics. */
final class EntityMotionCollisions {
    static int verticalSpeedZ(EntityBackgroundMechanics owner, RoomEntity entity) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (runtime.bossRewards().readEnemyDropActive(slot)) {
            return runtime.motions()
                    .get(EnemyDropMotion.class, EnemyDropMotion::new)
                    .speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_TEKTITE) {
            return runtime.motions()
                    .get(TektiteMotion.class, TektiteMotion::new)
                    .speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_FISH) {
            return runtime.motions().get(FishMotion.class, FishMotion::new).speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_CROW) {
            return runtime.motions().get(CrowMotion.class, CrowMotion::new).speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_CUCCO) {
            return runtime.motions().get(CuccoMotion.class, CuccoMotion::new).speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return runtime.motions()
                    .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                    .speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_WINGED_OCTOROK) {
            return runtime.motions()
                    .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                    .speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_STALFOS_AGGRESSIVE) {
            return runtime.motions()
                    .get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new)
                    .speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_STALFOS_EVASIVE) {
            return runtime.motions()
                    .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                    .speedZ(slot);
        }
        if (isZolGelType(entity.type())) {
            return runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_HIDING_ZOL) {
            return runtime.motions()
                    .get(HidingZolMotion.class, HidingZolMotion::new)
                    .speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_POLS_VOICE) {
            return runtime.motions()
                    .get(PolsVoiceMotion.class, PolsVoiceMotion::new)
                    .speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE) {
            return runtime.motions()
                    .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                    .speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DOG) {
            return runtime.motions().get(DogMotion.class, DogMotion::new).speedZ(slot);
        }
        if (isEnemyProjectileType(entity.type())) {
            return runtime.motions()
                    .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                    .speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_ARROW) {
            return runtime.motions()
                    .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                    .speedZ(slot);
        }
        if (ColorShellMotion.isColorShellType(entity.type())) {
            return runtime.motions()
                    .get(ColorShellMotion.class, ColorShellMotion::new)
                    .speedZ(slot);
        }
        return 0;
    }

    static void resetHookshotChainAfterLinkPush(EntityBackgroundMechanics owner) {
        var runtime = owner.runtime;
        for (int slot = 0; slot < runtime.slots().length; slot++) {
            if (!runtime.slots()[slot].loaded()
                    || runtime.slots()[slot].type() != EntityRomConstants.ENTITY_HOOKSHOT_CHAIN
                    || !runtime.motions()
                            .get(HookshotChainMotion.class, HookshotChainMotion::new)
                            .active(slot)) {
                continue;
            }
            HookshotChainMotion.State state = runtime.motions()
                    .get(HookshotChainMotion.class, HookshotChainMotion::new)
                    .state(slot);
            runtime.motions()
                    .get(HookshotChainMotion.class, HookshotChainMotion::new)
                    .setState(
                            slot,
                            new HookshotChainMotion.State(
                                    state.x(),
                                    state.y(),
                                    state.z(),
                                    state.direction(),
                                    state.speedX(),
                                    state.speedY(),
                                    state.transitionCountdown(),
                                    state.speedXAccumulator(),
                                    state.speedYAccumulator(),
                                    0,
                                    state.wallCollisionPending()));
            return;
        }
    }

    static EnemyRecoilMotion.Update applyEnemyRecoilIfNeeded(
            EntityBackgroundMechanics owner, RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (!runtime.motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .isActive(slot)) {
            return new EnemyRecoilMotion.Update(entity, false);
        }
        if (runtime.combatTables().readEnemyIgnoreHitsCountdown(slot) == 0) {
            runtime.motions()
                    .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                    .clear(slot);
            return new EnemyRecoilMotion.Update(entity, false);
        }

        if (isRoamingEnemyType(entity.type())) {
            runtime.motions()
                    .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                    .beginRecoil(slot);
        }
        // ApplyRecoilIfNeeded_03/06 decrements the shared countdown
        // immediately before applying one fixed-point recoil step.
        runtime.combatTables().postEnemyIgnoreHitsCountdown(slot, -1);
        EnemyRecoilMotion.Update update = runtime.motions()
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .advance(entity, backgroundCollision, isRoamingEnemyType(entity.type()));
        if (update.blocked() && isRoamingEnemyType(entity.type())) {
            // StopEntityRecoilOnCollision clears the ignore-hits countdown.
            runtime.combatTables().writeEnemyIgnoreHitsCountdown(slot, 0);
        }
        return update;
    }
}
