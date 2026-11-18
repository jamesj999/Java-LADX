package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.FallingVector;
import static linksawakening.world.entity.EntityMotionMath.vectorTowardsTarget;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntityTypeRules.isEnemyProjectileType;
import static linksawakening.world.entity.EntityTypeRules.isRoamingEnemyType;
import static linksawakening.world.entity.EntityTypeRules.isZolGelType;

import java.util.Objects;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.MoldormSegmentPosition;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.CrowMotion;
import linksawakening.world.entity.motion.CuccoMotion;
import linksawakening.world.entity.motion.DogMotion;
import linksawakening.world.entity.motion.EnemyDropMotion;
import linksawakening.world.entity.motion.EnemyProjectileMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.FishMotion;
import linksawakening.world.entity.motion.HidingZolMotion;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.MoblinSwordMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.PlayerArrowMotion;
import linksawakening.world.entity.motion.PolsVoiceMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.SecretSeashellMotion;
import linksawakening.world.entity.motion.SpikedBeetleMotion;
import linksawakening.world.entity.motion.StalfosAggressiveMotion;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;
import linksawakening.world.entity.motion.TektiteMotion;
import linksawakening.world.entity.motion.WingedOctorokMotion;
import linksawakening.world.entity.motion.ZolGelMotion;

/** ROM background collision, falling, recoil and Link push glue. */
public final class EntityBackgroundMechanics {
    /** Completed EntityFallHandler visit; callers consume only the disabled status. */
    private static final RoomEntity FINISHED_FALL = RoomEntity.disabled(0);

    private final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    private final EntityBossState bossRewards;
    private final EntityCombatTables combatTables;
    private final EntityEnemyInspection enemyInspection;
    private final EntityEventChannels events;
    private final EntityMotionRegistry motions;
    private final EntityPhysicsTables physicsTables;
    private final EntityRoomInspection roomInspection;
    private final EntityRoomVariables roomVariables;
    private final EntitySlots slots;
    private final EntitySpriteViews spriteViews;
    private final EntitySlotLifecycle lifecycle;

    EntityBackgroundMechanics(
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBossState bossRewards,
            EntityEnemyInspection enemyInspection,
            EntityEventChannels events,
            EntityMotionRegistry motions,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlots slots,
            EntitySlotLifecycle lifecycle) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        EntitySpriteViews spriteViews = spriteHandlers;
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.enemyInspection = Objects.requireNonNull(enemyInspection, "enemyInspection");
        this.events = Objects.requireNonNull(events, "events");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.roomInspection = Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = Objects.requireNonNull(roomVariables, "roomVariables");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.spriteViews = Objects.requireNonNull(spriteViews, "spriteViews");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    public RoomEntityBackgroundInteraction interaction() {
        return this.physicsTables.readBackgroundInteraction();
    }

    boolean moldormOrHeadOverlapsSword(RoomEntity entity, int swordX, int swordWidth, int swordY, int swordHeight) {
        if (RoomEntityCombatRules.overlapsSword(entity, swordX, swordWidth, swordY, swordHeight)) {
            return true;
        }
        if (entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
            return false;
        }
        if (this.combatTables.readEnemyFlashCountdown(entity.slot()) != 0) {
            return false;
        }
        MoldormSegmentPosition tail =
                this.motions.get(MoldormMotion.class, MoldormMotion::new).vulnerableTail(entity.slot());
        RoomEntity tailCollisionEntity =
                withPositionAndVariant(entity, tail.x(), (tail.y() + entity.z()) & 0xFF, entity.spriteVariant());
        return RoomEntityCombatRules.overlapsSword(tailCollisionEntity, swordX, swordWidth, swordY, swordHeight);
    }

    public void advanceHookshotBridgeEntity(int index, RoomEntity entity) {
        var hookshotBridgeMotion = this.motions.get(HookshotBridgeMotion.class, HookshotBridgeMotion::new);
        HookshotBridgeMotion.State bridgeState = hookshotBridgeMotion.state(entity.slot());
        if (bridgeState == null) {
            this.lifecycle.clearEntity(entity.slot());
            return;
        }

        HookshotBridgeMotion.ObjectCell cell = HookshotBridgeMotion.objectCell(bridgeState);
        HookshotBridgeMotion.Step step = hookshotBridgeMotion.advance(entity.slot());
        RoomEntity moved =
                withPositionAndVariant(entity, step.state().x(), step.state().y(), entity.spriteVariant());
        RoomEntityObjectSample object = this.physicsTables.readObjectQuery().sampleOrAbsent(moved);
        boolean clear = object.sampled()
                && object.objectId() != EntityRomConstants.OBJECT_HOOKSHOT_BRIDGE_REPLACEMENT
                && object.physicsFlag() == 0;
        this.events.hookshotBridge(
                new HookshotBridgeUpdate(cell.objectLeft(), cell.objectTop(), bridgeState.direction(), !clear));
        if (clear) {
            this.lifecycle.clearEntity(entity.slot());
        } else {
            this.slots.replace(index, moved);
        }
    }

    public void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) {
        if (slot >= 0
                && slot < this.slots.size()
                && this.slots.get(slot).type() == EntityRomConstants.ENTITY_PUSHED_BLOCK
                && this.slots.get(slot).status() == EntityStatus.ACTIVE
                && RoomEntityCombatRules.overlapsLink(this.slots.get(slot), linkEntityX, linkEntityY)) {
            requestLinkPush(this.slots.get(slot), EntityLinkCollisionRules.PUSHED_BLOCK);
        }
    }

    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) {
        return this.roomInspection.gameplayEntityTimersInteractive(creditsGameplay);
    }

    boolean hasNoGroundInteraction(RoomEntity entity) {
        return this.roomInspection.hasNoGroundInteraction(entity);
    }

    boolean beginFalling(
            RoomEntity entity,
            RoomEntityGroundInteraction.PitTransition transition,
            boolean ignoreHitsDecrementedBeforeHandler) {
        int slot = entity.slot();
        int ignoreHitsCountdown = this.combatTables.readEnemyIgnoreHitsCountdown(slot);
        if (ignoreHitsDecrementedBeforeHandler) {
            // The Java runtime keeps a shared combat countdown pass for the
            // already-ported handlers.  In the ROM, the pit branch reads the
            // value before that branch's own decrement; reconstruct that
            // value here so INIT and ACTIVE entities both get one decrement
            // at the same source-level point.
            ignoreHitsCountdown = (ignoreHitsCountdown + 1) & 0xFF;
        }
        if (ignoreHitsCountdown == 0) {
            return false;
        }

        ignoreHitsCountdown--;
        this.combatTables.writeEnemyIgnoreHitsCountdown(slot, ignoreHitsCountdown);
        this.combatTables.writeEnemyFlashCountdown(slot, 0);
        this.physicsTables.writeFallingTargetX(slot, transition.targetX() & 0xFF);
        this.physicsTables.writeFallingTargetY(slot, transition.targetY() & 0xFF);
        this.physicsTables.writeFallingSpeedX(slot, 0);
        this.physicsTables.writeFallingSpeedY(slot, 0);
        this.physicsTables.writeFallingSpeedXAccumulator(slot, 0);
        this.physicsTables.writeFallingSpeedYAccumulator(slot, 0);
        this.physicsTables.writeFallingVisualYOffset(slot, 0);

        boolean longFallingTransition = entity.type() == EntityRomConstants.ENTITY_MOBLIN_SWORD
                || entity.type() == EntityRomConstants.ENTITY_MOBLIN
                || entity.type() == EntityRomConstants.ENTITY_OCTOROK;
        this.physicsTables.writeEnemyTransitionCountdown(slot, longFallingTransition ? 0x6F : 0x48);
        if (!longFallingTransition && ignoreHitsCountdown == 0) {
            this.physicsTables.writeEnemyTransitionCountdown(slot, 0x2F);
            this.events.sound(
                    slot, entity.type(), EntityCombatEvent.SoundChannel.JINGLE, EntityRomConstants.FALLING_JINGLE_ID);
        }
        return true;
    }

    RoomEntity advanceFallingEntity(RoomEntity entity) {
        int slot = entity.slot();
        int transition = this.physicsTables.readEnemyTransitionCountdown(slot) & 0xFF;
        if (transition == 0) {
            return FINISHED_FALL;
        }

        if (transition >= 0x40) {
            // EntityFallHandler calls SetEntityVariantForDirection_03 three
            // times before the family handler. ReturnIfNonInteractive then
            // stops that handler after presentation because the status is
            // still FALLING, so movement and wall collision must not run here.
            this.physicsTables.writeFallingVisualYOffset(slot, 0);
            int variant = entity.spriteVariant();
            if (entity.type() == EntityRomConstants.ENTITY_OCTOROK
                    || entity.type() == EntityRomConstants.ENTITY_MOBLIN) {
                var roamingEnemyMotion = this.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
                variant = roamingEnemyMotion.advancePresentationVariant(slot, 3);
            } else if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_SWORD) {
                var moblinSwordMotion = this.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new);
                variant = moblinSwordMotion.advancePresentationVariant(slot, entity.x(), 3);
            }
            return withPositionAndVariant(entity, entity.x(), entity.y(), variant);
        }

        int phase = (transition >>> 4) & 0x03;
        this.physicsTables.writeFallingVisualYOffset(slot, EntityRomConstants.FALLING_VISUAL_Y_OFFSETS[phase]);
        if (transition == 0x3F) {
            this.events.sound(
                    slot, entity.type(), EntityCombatEvent.SoundChannel.JINGLE, EntityRomConstants.FALLING_JINGLE_ID);
        }

        FallingVector vector = vectorTowardsTarget(
                entity.x(),
                entity.y(),
                entity.z(),
                this.physicsTables.readFallingTargetX(slot),
                this.physicsTables.readFallingTargetY(slot),
                EntityRomConstants.FALLING_VECTOR_LENGTHS[phase]);
        this.physicsTables.writeFallingSpeedX(slot, vector.x());
        this.physicsTables.writeFallingSpeedY(slot, vector.y());
        int x = this.physicsTables.advanceFallingX(entity.x(), this.physicsTables.readFallingSpeedX(slot), slot);
        int y = this.physicsTables.advanceFallingY(entity.y(), this.physicsTables.readFallingSpeedY(slot), slot);
        {
            entity = this.spriteHandlers.present(entity, this.spriteViews.forFallingEntity(), phase);
        }
        return withPositionAndVariant(entity, x, y, phase);
    }

    public int verticalSpeedZ(RoomEntity entity) {
        int slot = entity.slot();
        if (this.bossRewards.readEnemyDropActive(slot)) {
            var enemyDropMotion = this.motions.get(EnemyDropMotion.class, EnemyDropMotion::new);
            return enemyDropMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_TEKTITE) {
            var tektiteMotion = this.motions.get(TektiteMotion.class, TektiteMotion::new);
            return tektiteMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_FISH) {
            var fishMotion = this.motions.get(FishMotion.class, FishMotion::new);
            return fishMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_CROW) {
            var crowMotion = this.motions.get(CrowMotion.class, CrowMotion::new);
            return crowMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_CUCCO) {
            var cuccoMotion = this.motions.get(CuccoMotion.class, CuccoMotion::new);
            return cuccoMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            var secretSeashellMotion = this.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
            return secretSeashellMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_WINGED_OCTOROK) {
            var wingedOctorokMotion = this.motions.get(WingedOctorokMotion.class, WingedOctorokMotion::new);
            return wingedOctorokMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_STALFOS_AGGRESSIVE) {
            var stalfosAggressiveMotion = this.motions.get(StalfosAggressiveMotion.class, StalfosAggressiveMotion::new);
            return stalfosAggressiveMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_STALFOS_EVASIVE) {
            var stalfosEvasiveMotion = this.motions.get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new);
            return stalfosEvasiveMotion.speedZ(slot);
        }
        if (isZolGelType(entity.type())) {
            var zolGelMotion = this.motions.get(ZolGelMotion.class, ZolGelMotion::new);
            return zolGelMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_HIDING_ZOL) {
            var hidingZolMotion = this.motions.get(HidingZolMotion.class, HidingZolMotion::new);
            return hidingZolMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_POLS_VOICE) {
            var polsVoiceMotion = this.motions.get(PolsVoiceMotion.class, PolsVoiceMotion::new);
            return polsVoiceMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE) {
            var spikedBeetleMotion = this.motions.get(SpikedBeetleMotion.class, SpikedBeetleMotion::new);
            return spikedBeetleMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_DOG) {
            var dogMotion = this.motions.get(DogMotion.class, DogMotion::new);
            return dogMotion.speedZ(slot);
        }
        if (isEnemyProjectileType(entity.type())) {
            var enemyProjectileMotion = this.motions.get(EnemyProjectileMotion.class, EnemyProjectileMotion::new);
            return enemyProjectileMotion.speedZ(slot);
        }
        if (entity.type() == EntityRomConstants.ENTITY_ARROW) {
            var playerArrowMotion = this.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new);
            return playerArrowMotion.speedZ(slot);
        }
        if (ColorShellMotion.isColorShellType(entity.type())) {
            var colorShellMotion = this.motions.get(ColorShellMotion.class, ColorShellMotion::new);
            return colorShellMotion.speedZ(slot);
        }
        return 0;
    }

    boolean entityHandlerInteractive(boolean creditsGameplay) {
        return this.enemyInspection.entityHandlerInteractive(creditsGameplay);
    }

    public boolean entityTimersInteractive() {
        return this.enemyInspection.entityTimersInteractive();
    }

    public void requestArmosLinkPush(RoomEntity entity, int linkEntityX, int linkEntityY, int linkMotionState) {
        if (linkMotionState >= EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
                || !RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return;
        }
        requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
    }

    public void requestLinkPush(
            /* Optional source actor; absence emits no push. */ RoomEntity entity,
            /* Optional push outcome; absence emits no push. */ EntityLinkCollisionRules.LinkPushPolicy policy) {
        if (entity == null || policy == null || !policy.restoreFinalPosition()) {
            return;
        }
        this.events.linkFinalPosition(new LinkFinalPositionRequest(
                entity.slot(),
                policy.resetPegasusBoots(),
                policy.resetHookshotChain(),
                policy.markLinkPushing(),
                policy.clearLinkPositionIncrement()));
        if (policy.resetHookshotChain()) {
            resetHookshotChainAfterLinkPush();
        }
    }

    void resetHookshotChainAfterLinkPush() {
        for (int slot = 0; slot < this.slots.size(); slot++) {
            var hookshotChainMotion = this.motions.get(HookshotChainMotion.class, HookshotChainMotion::new);
            if (!this.slots.get(slot).loaded()
                    || this.slots.get(slot).type() != EntityRomConstants.ENTITY_HOOKSHOT_CHAIN
                    || !hookshotChainMotion.active(slot)) {
                continue;
            }
            HookshotChainMotion.State state = hookshotChainMotion.state(slot);
            hookshotChainMotion.setState(
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

    public EnemyRecoilMotion.Update applyEnemyRecoilIfNeeded(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        if (!enemyRecoilMotion.isActive(slot)) {
            return new EnemyRecoilMotion.Update(entity, false);
        }
        if (this.combatTables.readEnemyIgnoreHitsCountdown(slot) == 0) {
            enemyRecoilMotion.clear(slot);
            return new EnemyRecoilMotion.Update(entity, false);
        }

        if (isRoamingEnemyType(entity.type())) {
            var roamingEnemyMotion = this.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
            roamingEnemyMotion.beginRecoil(slot);
        }
        // ApplyRecoilIfNeeded_03/06 decrements the shared countdown
        // immediately before applying one fixed-point recoil step.
        this.combatTables.postEnemyIgnoreHitsCountdown(slot, -1);
        EnemyRecoilMotion.Update update =
                enemyRecoilMotion.advance(entity, backgroundCollision, isRoamingEnemyType(entity.type()));
        if (update.blocked() && isRoamingEnemyType(entity.type())) {
            // StopEntityRecoilOnCollision clears the ignore-hits countdown.
            this.combatTables.writeEnemyIgnoreHitsCountdown(slot, 0);
        }
        return update;
    }

    public boolean groundInteractionSideScrolling() {
        return this.physicsTables.readGroundInteractionSideScrolling();
    }

    public RoomEntityObjectQuery objectQuery() {
        return this.physicsTables.readObjectQuery();
    }

    public RoomEntityGroundInteraction groundInteraction() {
        return this.physicsTables.readGroundInteraction();
    }

    public boolean switchBlockAnimationActive() {
        return this.roomVariables.readSwitchBlockAnimationActive();
    }

    public void requestSwitchBlockAnimation() {
        this.roomVariables.writePendingSwitchBlockAnimationRequest(true);
    }

    public int entityGroundStatus(int slot) {
        return this.physicsTables.readEntityGroundStatus(slot);
    }

    public void setEntityGroundStatus(int slot, int value) {
        this.physicsTables.writeEntityGroundStatus(slot, value);
    }

    public void setFallingVisualYOffset(int slot, int value) {
        this.physicsTables.writeFallingVisualYOffset(slot, value);
    }

    public void initializeKeyQuicksandFall(int slot) {
        this.physicsTables.writeFallingTargetX(slot, 0x50);
        this.physicsTables.writeFallingTargetY(slot, 0x48);
        this.physicsTables.writeFallingSpeedX(slot, 0);
        this.physicsTables.writeFallingSpeedY(slot, 0);
        this.physicsTables.writeFallingSpeedXAccumulator(slot, 0);
        this.physicsTables.writeFallingSpeedYAccumulator(slot, 0);
        this.physicsTables.writeFallingVisualYOffset(slot, 0);
    }
}
