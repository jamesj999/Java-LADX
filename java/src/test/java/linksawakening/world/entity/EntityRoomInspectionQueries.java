package linksawakening.world.entity;

import java.util.List;
import linksawakening.world.entity.motion.BoomerangMotion;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.BushCrawlerMotion;
import linksawakening.world.entity.motion.EnemyDropMotion;
import linksawakening.world.entity.motion.FairyMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.LikeLikeMotion;
import linksawakening.world.entity.motion.MagicRodFireballMotion;
import linksawakening.world.entity.motion.MoblinKingMotion;
import linksawakening.world.entity.motion.PlayerArrowMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.SwordBeamMotion;
import linksawakening.world.entity.motion.WingedOctorokMotion;

/** Test-side queries over the production inspector's exact service bindings. */
final class EntityRoomInspectionQueries {
    private final EntityRoomInspection runtime;

    EntityRoomInspectionQueries(EntityRoomInspection source) {

        this.runtime = source;
    }

    List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests() {
        return List.copyOf(runtime.events().projectileLaunchRequests());
    }

    int activePlayerArrowCount() {
        int count = 0;
        for (int entitySlotIndex = 0; entitySlotIndex < runtime.slots().size(); entitySlotIndex++) {
            RoomEntity entity = runtime.slots().get(entitySlotIndex);
            {
                if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ARROW) {
                    count++;
                }
            }
        }
        return count;
    }

    int activeBoomerangSlot() {
        for (int entitySlotIndex = 0; entitySlotIndex < runtime.slots().size(); entitySlotIndex++) {
            RoomEntity entity = runtime.slots().get(entitySlotIndex);
            {
                if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOOMERANG) {
                    return entity.slot();
                }
            }
        }
        return -1;
    }

    int boomerangDirection(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .direction(slot);
    }

    int boomerangSpeedX(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .speedX(slot);
    }

    int boomerangSpeedY(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .speedY(slot);
    }

    int boomerangTransitionCountdown(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .transitionCountdown(slot);
    }

    int boomerangState(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .state(slot);
    }

    int swordBeamState(int slot) {
        return runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .state(slot);
    }

    int magicRodFireballFireTransitionCountdown(int slot) {
        return runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .fireTransitionCountdown(slot);
    }

    int magicPowderTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int magicPowderState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.projectiles().readMagicPowderState(slot);
    }

    int magicPowderSlowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readSlowTransitionCountdown(slot);
    }

    int swordBeamSpeedX(int slot) {
        return runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .speedX(slot);
    }

    int swordBeamSpeedY(int slot) {
        return runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .speedY(slot);
    }

    int playerArrowDirection(int slot) {
        return runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .direction(slot);
    }

    int playerArrowSpeedX(int slot) {
        return runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .speedX(slot);
    }

    int playerArrowSpeedY(int slot) {
        return runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .speedY(slot);
    }

    int playerArrowTransitionCountdown(int slot) {
        return runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .transitionCountdown(slot);
    }

    int hookshotTransitionCountdown(int slot) {
        return runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .transitionCountdown(slot);
    }

    int bowWowTargetSlot(int slot) {
        return runtime.motions().get(BowWowMotion.class, BowWowMotion::new).targetSlot(slot);
    }

    int bowWowBiteCountdown(int slot) {
        return runtime.motions().get(BowWowMotion.class, BowWowMotion::new).biteCountdown(slot);
    }

    int bowWowTransitionCountdown(int slot) {
        return runtime.motions().get(BowWowMotion.class, BowWowMotion::new).transitionCountdown(slot);
    }

    int entityInertia(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEntityInertia(slot);
    }

    int pokeyHeadAttached(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(PokeyMotion.class, PokeyMotion::new).headAttached(slot);
    }

    int pokeySpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(PokeyMotion.class, PokeyMotion::new).speedX(slot);
    }

    int pokeySpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(PokeyMotion.class, PokeyMotion::new).speedY(slot);
    }

    int pokeyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int hookshotEntityState(int slot) {
        HookshotChainMotion.State state = runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .state(slot);
        return state == null ? 0 : state.entityState();
    }

    boolean hookshotWallCollisionPending(int slot) {
        HookshotChainMotion.State state = runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .state(slot);
        return state != null && state.wallCollisionPending();
    }

    List<WingedOctorokOam.Entry> wingedOctorokOam() {
        return runtime.slotProperties().readWingedOctorokOam();
    }

    int wingedOctorokState(int slot) {
        return runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .state(slot);
    }

    int wingedOctorokSpeedX(int slot) {
        return runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .speedX(slot);
    }

    int wingedOctorokSpeedY(int slot) {
        return runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .speedY(slot);
    }

    int wingedOctorokSpeedZ(int slot) {
        return runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .speedZ(slot);
    }

    int bushCrawlerState(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .state(slot);
    }

    int bushCrawlerDisguiseMode(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .disguiseMode(slot);
    }

    int bushCrawlerCrawlHeading(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .crawlHeading(slot);
    }

    int bushCrawlerSpeedX(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .speedX(slot);
    }

    int bushCrawlerSpeedY(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .speedY(slot);
    }

    int moblinKingState(int slot) {
        return runtime.motions()
                .get(MoblinKingMotion.class, MoblinKingMotion::new)
                .state(slot);
    }

    int likeLikeState(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).state(slot);
    }

    int likeLikeSwallowedShieldLevel(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).swallowedShieldLevel(slot);
    }

    int likeLikeInertia(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).inertia(slot);
    }

    int likeLikeWalkState(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).walkState(slot);
    }

    int likeLikeWalkSpeedY(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).walkSpeedY(slot);
    }

    int slowTransitionCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots().size()) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readSlowTransitionCountdown(slot);
    }

    int killCount() {
        return runtime.bossRewards().readKillCount();
    }

    int dropPrivateCountdown1(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readDropPrivateCountdown1(slot);
    }

    int dropPrivateCountdown3(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readDropPrivateCountdown3(slot);
    }

    int droppablePrivateState3(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readDroppablePrivateState3(slot);
    }

    int droppablePrivateState4(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readDroppablePrivateState4(slot);
    }

    int dropSpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .speedY(slot);
    }

    int fairySpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(FairyMotion.class, FairyMotion::new).speedX(slot);
    }

    int fairySpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(FairyMotion.class, FairyMotion::new).speedY(slot);
    }

    public boolean chestPlayerLevelsKnown() {
        return runtime.pickups().readChestPlayerLevelsKnown();
    }
}
