package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.byteValue;
import static linksawakening.world.entity.EntitySnapshots.withDeathPresentation;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntityTypeRules.isGhiniType;
import static linksawakening.world.entity.EntityTypeRules.isZolGelType;
import static linksawakening.world.entity.EntityTypeRules.usesSharedRecoil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import linksawakening.world.entity.SlimeEyeController.SlimeEyeCombatFrame;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.ArmosMotion;
import linksawakening.world.entity.motion.BombiteMotion;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.CrowMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.FishMotion;
import linksawakening.world.entity.motion.GhiniMotion;
import linksawakening.world.entity.motion.GoombaMotion;
import linksawakening.world.entity.motion.HidingZolMotion;
import linksawakening.world.entity.motion.HinoxMotion;
import linksawakening.world.entity.motion.LeeverMotion;
import linksawakening.world.entity.motion.LikeLikeMotion;
import linksawakening.world.entity.motion.MoblinKingMotion;
import linksawakening.world.entity.motion.MoldormMotion;
import linksawakening.world.entity.motion.PairoddMotion;
import linksawakening.world.entity.motion.PeaHatMotion;
import linksawakening.world.entity.motion.PiranhaMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.RollingBonesMotion;
import linksawakening.world.entity.motion.SpikedBeetleMotion;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;
import linksawakening.world.entity.motion.ThreeOfAKindMotion;
import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;
import linksawakening.world.entity.motion.WingedOctorokMotion;
import linksawakening.world.entity.motion.WizrobeMotion;
import linksawakening.world.entity.motion.ZolGelMotion;
import linksawakening.world.entity.motion.ZombieMotion;

/** DefaultEnemyDamageCollisionHandler and ApplySwordDamagesToEnemy, in ROM collision order. */
public final class EntityCombatResolver {
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityBombInteractions bombInteractions;
    final EntityBossRewards bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityRoomInspection roomInspection;
    final EntitySlotProperties slotProperties;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final RomEnemyCombatTables enemyCombatTables;
    final EntitySlots slots;
    final SlimeEyeController slimeEye;
    final EntityFrameRandom frameRandom;
    private final EntityCombatSlot slotScratch;
    private boolean slotScratchInUse;
    final boolean[] deferredLeeverContact = new boolean[RoomEntity.SLOT_COUNT];
    final int[] contactLinkX = new int[RoomEntity.SLOT_COUNT];
    final int[] contactLinkY = new int[RoomEntity.SLOT_COUNT];

    EntityCombatResolver(
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityBombInteractions bombInteractions,
            EntityBossRewards bossRewards,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityRoomInspection roomInspection,
            EntitySlotProperties slotProperties,
            RomEnemyCombatTables enemyCombatTables,
            EntitySlots slots,
            SlimeEyeController slimeEye,
            EntityFrameRandom frameRandom) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.backgroundMechanics = Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.bombInteractions = Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.events = Objects.requireNonNull(events, "events");
        this.linkState = Objects.requireNonNull(linkState, "linkState");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.roomInspection = Objects.requireNonNull(roomInspection, "roomInspection");
        this.slotProperties = Objects.requireNonNull(slotProperties, "slotProperties");
        this.enemyCombatTables = Objects.requireNonNull(enemyCombatTables, "combat rules");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.slimeEye = Objects.requireNonNull(slimeEye, "slimeEye");
        this.frameRandom = Objects.requireNonNull(frameRandom, "frameRandom");
        slotScratch = new EntityCombatSlot(this);
    }

    EntityCombatSlot acquireSlot() {
        if (slotScratchInUse) return new EntityCombatSlot(this);
        slotScratchInUse = true;
        return slotScratch;
    }

    void releaseSlot(EntityCombatSlot slot) {
        slot.reset(null, null, null);
        if (slot == slotScratch) slotScratchInUse = false;
    }

    public boolean deferContactRecoil(int slot, int linkX, int linkY) {
        EntitySlotValidation.validateEntitySlot(slot);
        EntitySlotValidation.validateByte(linkX, "Link X");
        EntitySlotValidation.validateByte(linkY, "Link Y");
        RoomEntity entity = this.slots.get(slot);
        if (entity.type() != EntityRomConstants.ENTITY_LEEVER || entity.status() != EntityStatus.ACTIVE) return false;
        deferredLeeverContact[slot] = true;
        contactLinkX[slot] = linkX;
        contactLinkY[slot] = linkY;
        return true;
    }

    void clearDeferredContactRecoil() {
        Arrays.fill(deferredLeeverContact, false);
    }

    void finishLeeverContactRecoil(RoomEntity moved) {
        int slot = moved.slot();
        if (!this.deferredLeeverContact[slot]) return;
        this.deferredLeeverContact[slot] = false;
        // $04:7F01 moves before $7F49 calls the shared collision handler.
        // Its hitbox uses cached hActiveEntityPosX/Y, but $03:7565 reads
        // the updated position tables when selecting Link's recoil vector.
        var vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                moved.x(), moved.y(), moved.z(), this.contactLinkX[slot], this.contactLinkY[slot], 0x14);
        this.events.combat(linksawakening.world.entity.event.EntityCombatEvent.builder(slot, moved.type())
                .linkDamage(0)
                .swordHit(false)
                .enemyDamage(0)
                .enemySpecialAction(-1)
                .soundChannel(EntityCombatEvent.SoundChannel.NONE)
                .soundId(-1)
                .secondarySoundChannel(EntityCombatEvent.SoundChannel.NONE)
                .secondarySoundId(-1)
                .swordPokeVfx(null)
                .linkAction(EntityCombatEvent.LinkAction.NONE)
                .linkCollisionResponse(
                        new EntityCombatEvent.LinkCollisionResponse(vector.x() & 255, vector.y() & 255, 0x10))
                .build());
    }

    public List<EntityCombatEvent> resolveCombat(EnemyCombatInputs inputs) {

        int linkVerticalVelocity = inputs.linkVerticalVelocity();

        EnemyAttackContext attackContext = inputs.attackContext();
        EntitySlotValidation.validateByte(linkVerticalVelocity, "Link vertical velocity");
        this.clearDeferredContactRecoil();
        if (attackContext == null) {
            throw new IllegalArgumentException("Enemy attack context cannot be null");
        }
        this.linkState.writeRunningWithPegasusBoots(attackContext.pegasusBoots());
        List<EntityCombatEvent> events = this.events.outbox.channel(EntityCombatEvent.class, "resolvedCombat");
        events.clear();
        EntityCombatSlot slot = this.acquireSlot();
        try {
            for (int index = this.slots.size() - 1; index >= 0; index--) {
                slot.reset(inputs, events, this.slots.get(index));
                resolveSlotCombat(slot);
            }
        } finally {
            this.releaseSlot(slot);
        }
        return this.events.outbox.drain(EntityCombatEvent.class, "resolvedCombat");
    }

    static boolean entityCombatGatesAcceptInitialStatus(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        if (slot.entity.loaded() && slot.entity.type() == EntityRomConstants.ENTITY_SLIME_EYE) {
            runtime.slimeEye.resolveCombat(new SlimeEyeCombatFrame(
                    slot.entity,
                    frame.frameCounter(),
                    frame.linkEntityX(),
                    frame.linkEntityY(),
                    frame.linkAirborne(),
                    frame.linkInteractive(),
                    frame.swordCollisionActive(),
                    frame.swordX(),
                    frame.swordWidth(),
                    frame.swordY(),
                    frame.swordHeight(),
                    frame.attackContext(),
                    slot.events));
            return false;
        }
        if (!slot.entity.loaded()
                || slot.entity.status() != EntityStatus.ACTIVE
                || !RoomEntityCombatRules.supportsEnemyCollision(slot.entity.type())) {
            return false;
        }
        var hinoxMotion = runtime.motions.get(HinoxMotion.class, HinoxMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_HINOX
                && hinoxMotion.state(slot.entity.slot()) == HinoxMotion.STATE_GRAB) {
            // HinoxState4Handler does not call DefaultEnemyDamageCollisionHandler.
            return false;
        }
        var threeOfAKindMotion = runtime.motions.get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_THREE_OF_A_KIND
                && threeOfAKindMotion.state(slot.entity.slot()) == 2) {
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR && frame.linkAirborne()) {
            // RollingBonesBarEntityHandler returns before the default
            // collision handler whenever hLinkPositionZ is non-zero.
            return false;
        }
        var piranhaMotion = runtime.motions.get(PiranhaMotion.class, PiranhaMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_PIRANHA_PLANT
                && piranhaMotion.state(slot.entity.slot()) == 0) {
            // PiranhaPlantState0Handler keeps the plant hidden and does
            // not call DefaultEnemyDamageCollisionHandler.
            return false;
        }
        return true;
    }

    static boolean entityCombatGatesAcceptOrdinaryCollision(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        var zombieMotion = runtime.motions.get(ZombieMotion.class, ZombieMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_ZOMBIE
                && !zombieMotion.allowsEnemyCollision(slot.entity.slot())) {
            // ZombieState2Handler is the only Zombie branch that calls
            // DefaultEnemyDamageCollisionHandler.
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_BUZZ_BLOB && !frame.linkInteractive()) {
            // BuzzBlobEntityHandler returns before its default enemy
            // collision call when Link is non-interactive.
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_SAND_CRAB && !frame.linkInteractive()) {
            // SandCrabEntityHandler returns before its default enemy
            // collision call when Link is non-interactive.
            return false;
        }
        var fishMotion = runtime.motions.get(FishMotion.class, FishMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_FISH
                && !fishMotion.allowsEnemyCollision(slot.entity.slot())) {
            return false;
        }
        var crowMotion = runtime.motions.get(CrowMotion.class, CrowMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_CROW
                && !crowMotion.allowsEnemyCollision(slot.entity.slot())) {
            return false;
        }
        return true;
    }

    static boolean entityCombatGatesAcceptHandlerCollision(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        var booBuddyMotion = runtime.motions.get(BooBuddyMotion.class, BooBuddyMotion::new);
        slot.booBuddyAllowsLinkCollision = slot.entity.type() != EntityRomConstants.ENTITY_BOO_BUDDY
                || booBuddyMotion.allowsEnemyCollision(
                        slot.entity.slot(),
                        booBuddyMotion.readBooBuddyTriggerCount(),
                        runtime.physicsTables.readEnemyTransitionCountdown(slot.entity.slot()),
                        frame.swordCollisionActive());
        slot.booBuddyAllowsSwordCollision = slot.entity.type() != EntityRomConstants.ENTITY_BOO_BUDDY
                || booBuddyMotion.allowsSwordCollision(slot.entity.slot(), booBuddyMotion.readBooBuddyTriggerCount());
        if (!slot.booBuddyAllowsLinkCollision && !slot.booBuddyAllowsSwordCollision) {
            return false;
        }
        var ghiniMotion = runtime.motions.get(GhiniMotion.class, GhiniMotion::new);
        if (isGhiniType(slot.entity.type())
                && (ghiniMotion.hidden(slot.entity.slot()) || slot.entity.spriteVariant() < 0)) {
            return false;
        }
        var spikedBeetleMotion = runtime.motions.get(SpikedBeetleMotion.class, SpikedBeetleMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE
                && !spikedBeetleMotion.allowsCombat(slot.entity.slot())) {
            return false;
        }
        var armosMotion = runtime.motions.get(ArmosMotion.class, ArmosMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_ARMOS_STATUE && !armosMotion.isActive(slot.entity.slot())) {
            return false;
        }
        // $15:4EFA skips DefaultEnemyDamageCollisionHandler only for
        // the jump's initial $08 transition countdown, not all airtime.
        var stalfosEvasiveMotion = runtime.motions.get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_STALFOS_EVASIVE
                && stalfosEvasiveMotion.privateState1(slot.entity.slot()) == 0
                && runtime.physicsTables.readEnemyTransitionCountdown(slot.entity.slot()) != 0) {
            return false;
        }
        var leeverMotion = runtime.motions.get(LeeverMotion.class, LeeverMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_LEEVER && !leeverMotion.isChasing(slot.entity.slot())) {
            return false;
        }
        return true;
    }

    static boolean entityCombatGatesAcceptMovementState(EntityCombatSlot slot) {
        var runtime = slot.runtime;

        var peaHatMotion = runtime.motions.get(PeaHatMotion.class, PeaHatMotion::new);
        slot.peaHatGrounded =
                slot.entity.type() != EntityRomConstants.ENTITY_PEAHAT || peaHatMotion.isGrounded(slot.entity);
        var pairoddMotion = runtime.motions.get(PairoddMotion.class, PairoddMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_PAIRODD
                && !pairoddMotion.allowsEnemyCollision(slot.entity.slot())) {
            return false;
        }
        var wizrobeMotion = runtime.motions.get(WizrobeMotion.class, WizrobeMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_WIZROBE && wizrobeMotion.state(slot.entity.slot()) != 3) {
            return false;
        }
        var pokeyMotion = runtime.motions.get(PokeyMotion.class, PokeyMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_POKEY
                && pokeyMotion.isSegment(slot.entity.slot())
                && runtime.physicsTables.readEnemyTransitionCountdown(slot.entity.slot()) != 0) {
            // The detached branch calls DefaultEnemyDamageCollisionHandler
            // only after its transition countdown reaches zero.
            return false;
        }
        var wingedOctorokMotion = runtime.motions.get(WingedOctorokMotion.class, WingedOctorokMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_WINGED_OCTOROK
                && (wingedOctorokMotion.state(slot.entity.slot()) == 2
                        || runtime.slotProperties.state.readWingedStateTwoThisFrame(slot.entity.slot()))) {
            // WingedOctorokEntityHandler returns directly from its jump
            // state instead of calling DefaultEnemyDamageCollisionHandler.
            return false;
        }
        var zolGelMotion = runtime.motions.get(ZolGelMotion.class, ZolGelMotion::new);
        if (isZolGelType(slot.entity.type()) && zolGelMotion.skipsEnemyCollision(slot.entity.slot())) {
            return false;
        }
        return true;
    }

    static boolean entityCombatGatesAcceptDamageCountdowns(EntityCombatSlot slot) {
        var runtime = slot.runtime;

        var hidingZolMotion = runtime.motions.get(HidingZolMotion.class, HidingZolMotion::new);
        slot.hidingZolSwordCollision = slot.entity.type() != EntityRomConstants.ENTITY_HIDING_ZOL
                || hidingZolMotion.allowsSwordCollision(slot.entity.slot());
        slot.hidingZolLinkCollision = slot.entity.type() != EntityRomConstants.ENTITY_HIDING_ZOL
                || hidingZolMotion.allowsLinkCollision(slot.entity.slot());
        if (!slot.hidingZolSwordCollision && !slot.hidingZolLinkCollision) {
            return false;
        }
        // AnimateEntities decrements flash before dispatching Rolling Bones.
        // Main's combat pass precedes tick(), so read that impending value.
        slot.collisionFlashCountdown = Math.max(
                0,
                runtime.combatTables.readEnemyFlashCountdown(slot.entity.slot())
                        - (slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES ? 1 : 0));
        if ((slot.collisionFlashCountdown > 0 && slot.entity.type() != EntityRomConstants.ENTITY_MOLDORM)
                || runtime.combatTables.readEnemyIgnoreHitsCountdown(slot.entity.slot()) > 0) {
            return false;
        }
        var bombiteMotion = runtime.motions.get(BombiteMotion.class, BombiteMotion::new);
        if ((slot.entity.type() == EntityRomConstants.ENTITY_TIMER_BOMBITE
                        && bombiteMotion.state(slot.entity.slot()) == 1)
                || (slot.entity.type() == EntityRomConstants.ENTITY_BOUNCING_BOMBITE
                        && runtime.bombInteractions.state.readBombPrivateCountdown1(slot.entity.slot()) > 0)) {
            return false;
        }
        return true;
    }

    static boolean entityCombatOverlapProbeBodyAndSword(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.goombaStomp = runtime.roomInspection.canGoombaStomp(
                slot.entity,
                frame.linkEntityX(),
                frame.linkEntityY(),
                frame.linkAirborne(),
                frame.linkVerticalVelocity(),
                frame.linkInteractive(),
                frame.frameCounter());
        slot.linkCollision = slot.entity.type() != EntityRomConstants.ENTITY_LIKE_LIKE
                && !frame.linkAirborne()
                && frame.linkInteractive()
                && slot.booBuddyAllowsLinkCollision
                && RoomEntityCombatRules.collisionCadenceMatches(frame.frameCounter(), slot.entity.slot())
                && slot.peaHatGrounded
                && slot.hidingZolLinkCollision
                && RoomEntityCombatRules.overlapsLink(slot.entity, frame.linkEntityX(), frame.linkEntityY());
        var likeLikeMotion = runtime.motions.get(LikeLikeMotion.class, LikeLikeMotion::new);
        slot.swordHit = frame.swordCollisionActive()
                && slot.booBuddyAllowsSwordCollision
                && !runtime.slotProperties.state.readWingedSwordAttackThisFrame(slot.entity.slot())
                && (slot.entity.type() != EntityRomConstants.ENTITY_LIKE_LIKE
                        || likeLikeMotion.state(slot.entity.slot()) == 0)
                && slot.hidingZolSwordCollision
                && runtime.backgroundMechanics.moldormOrHeadOverlapsSword(
                        slot.entity, frame.swordX(), frame.swordWidth(), frame.swordY(), frame.swordHeight());
        return true;
    }

    static boolean entityCombatOverlapApplyShieldContact(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // $20:4B4A supplies the shield rectangle to the same collision
        // pass as the sword, but $03:6EA5 dispatches it separately.
        if (slot.entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE
                && frame.linkInteractive()
                && frame.attackContext().usingShield()
                && !frame.swordCollisionActive()
                && runtime.combatTables.readEnemyIgnoreHitsCountdown(slot.entity.slot()) == 0
                && (runtime.combatTables.readEnemyFlashCountdown(slot.entity.slot()) == 0
                        || runtime.combatTables.readEnemyFlashCountdown(slot.entity.slot()) >= 0x18)) {
            int direction = frame.attackContext().romDirection();
            int shieldX = frame.attackContext().shieldX(frame.linkEntityX());
            int shieldY = frame.attackContext().shieldY(frame.linkEntityY());
            if (shieldX != 0
                    && RoomEntityCombatRules.overlapsSword(
                            slot.entity,
                            shieldX,
                            frame.attackContext().shieldWidth(),
                            shieldY,
                            frame.attackContext().shieldHeight())) {
                var spikedBeetleMotion = runtime.motions.get(SpikedBeetleMotion.class, SpikedBeetleMotion::new);
                if (spikedBeetleMotion.state(slot.entity.slot()) != 3) {
                    spikedBeetleMotion.flipFromShield(slot.entity.slot(), direction);
                    runtime.physicsTables.writeEnemyTransitionCountdown(slot.entity.slot(), 0xFF);
                }
                // func_003_6F5C clears the ignore-hits countdown after
                // the shared shield bump; it does not damage the shell.
                runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0);
                var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
                enemyRecoilMotion.clear(slot.entity.slot());
                EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        slot.entity.x(),
                        slot.entity.y(),
                        slot.entity.z(),
                        frame.linkEntityX(),
                        frame.linkEntityY(),
                        0x12);
                slot.events.add(linksawakening.world.entity.event.EntityCombatEvent.builder(
                                slot.entity.slot(), slot.entity.type())
                        .linkDamage(0)
                        .swordHit(false)
                        .enemyDamage(0)
                        .enemySpecialAction(-1)
                        .soundChannel(EntityCombatEvent.SoundChannel.JINGLE)
                        .soundId(0x09)
                        .secondarySoundChannel(EntityCombatEvent.SoundChannel.NONE)
                        .secondarySoundId(-1)
                        .swordPokeVfx(null)
                        .linkCollisionResponse(
                                new EntityCombatEvent.LinkCollisionResponse(vector.x() & 0xFF, vector.y() & 0xFF, 0x0C))
                        .build());
                return false;
            }
        }
        return true;
    }

    static boolean entityCombatOverlapAcceptContactOrStomp(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.moldormTailSwordHit = slot.swordHit
                && slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM
                && !RoomEntityCombatRules.overlapsSword(
                        slot.entity, frame.swordX(), frame.swordWidth(), frame.swordY(), frame.swordHeight());
        if (!slot.linkCollision && !slot.swordHit) {
            if (!slot.goombaStomp) {
                return false;
            }
        }

        if (slot.goombaStomp) {
            var goombaMotion = runtime.motions.get(GoombaMotion.class, GoombaMotion::new);
            goombaMotion.enterStomp(slot.entity.slot());
            runtime.physicsTables.writeEnemyTransitionCountdown(slot.entity.slot(), 0x30);
            EntityCombatEvent.LinkAction linkAction = runtime.physicsTables.readGroundInteractionSideScrolling()
                    ? EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING
                    : EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN;
            slot.events.add(
                    linksawakening.world.entity.event.EntityCombatEvent.builder(slot.entity.slot(), slot.entity.type())
                            .linkDamage(0)
                            .swordHit(false)
                            .enemyDamage(0)
                            .enemySpecialAction(-1)
                            .soundChannel(EntityCombatEvent.SoundChannel.WAVE)
                            .soundId(0x0E)
                            .secondarySoundChannel(EntityCombatEvent.SoundChannel.NONE)
                            .secondarySoundId(-1)
                            .swordPokeVfx(null)
                            .linkAction(linkAction)
                            .build());
            return false;
        }
        return true;
    }

    static void entitySwordDamageApply(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.swordResult = runtime.enemyCombatTables.resolveSwordDamage(
                slot.entity.type(),
                runtime.combatTables.readEnemyHealthGroup(slot.entity.slot()),
                frame.attackContext());
        slot.swordDamage = slot.swordResult.numericDamage();
        slot.enemyDamage = slot.swordDamage;
        {
            slot.enemySpecialAction = slot.swordResult.specialAction();
        }
        slot.swordResultApplied = !slot.swordResult.ignored();
        if (usesSharedRecoil(slot.entity.type()) || slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
            // EnemyCollidedWithSword applies the default `$30` recoil
            // before ApplySwordDamagesToEnemy changes health.
            var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
            enemyRecoilMotion.configure(
                    slot.entity.slot(),
                    slot.entity.x(),
                    slot.entity.y(),
                    slot.entity.z(),
                    frame.linkEntityX(),
                    frame.linkEntityY(),
                    0x30);
        }
        // ConfigureEntityRecoil reaches StartIgnoringHitsForEntity
        // before ApplySwordDamagesToEnemy, including lethal hits.
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(
                slot.entity.slot(), frame.attackContext().powerRecoil() ? 0x20 : 0x0A);
        if (slot.swordDamage > 0) {
            // $06:6CEB checks existing recoil before $6CEE applies a new hit.
            // Main resolves combat before ticking handlers, so preserve that phase.
            var rollingBonesMotion = runtime.motions.get(RollingBonesMotion.class, RollingBonesMotion::new);
            if (slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES)
                rollingBonesMotion.setSwordHitFrame(slot.entity.slot(), (frame.frameCounter() & 0xFF) + 1);
            runtime.combatTables.writeEnemyHealth(
                    slot.entity.slot(),
                    Math.max(0, runtime.combatTables.readEnemyHealth(slot.entity.slot()) - slot.swordDamage));
            if (slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
                var moldormMotion = runtime.motions.get(MoldormMotion.class, MoldormMotion::new);
                moldormMotion.onSwordHit(slot.entity.slot());
                runtime.combatTables.writeEnemyFlashCountdown(slot.entity.slot(), 0x28);
                runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0);
            }
        }
        slot.soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
        slot.soundId = slot.swordResultApplied ? 0x03 : 0x09;
        EntityCombatResolver.entitySwordStatusApply(slot);
    }

    static boolean entitySwordImpactPrepare(EntityCombatSlot slot) {
        var runtime = slot.runtime;

        slot.soundChannel = EntityCombatEvent.SoundChannel.NONE;
        slot.soundId = -1;
        slot.secondarySoundChannel = EntityCombatEvent.SoundChannel.NONE;
        slot.secondarySoundId = -1;
        slot.enemyDamage = 0;
        slot.enemySpecialAction = -1;
        slot.swordPokeVfx = null;
        slot.peaHatSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_PEAHAT && !slot.peaHatGrounded;
        slot.spikedBeetleSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE
                && (runtime.slotProperties.options1(slot.entity.slot()) & 0x40) != 0;
        slot.maskedMimicSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA
                && (runtime.slotProperties.options1(slot.entity.slot()) & 0x40) != 0;
        slot.rollingBonesBarSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR;
        var moblinKingMotion = runtime.motions.get(MoblinKingMotion.class, MoblinKingMotion::new);
        slot.moblinKingSwordClink = slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING
                && moblinKingMotion.state(slot.entity.slot()) != 5;
        return true;
    }

    static boolean entitySwordImpactApply(EntityCombatSlot slot) {
        var runtime = slot.runtime;

        var roamingEnemyMotion = runtime.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
        var unmaskedIronMaskMotion = runtime.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
        if (slot.entity.type() == EntityRomConstants.ENTITY_CRYSTAL_SWITCH) {
            entitySwordImpactApplyCrystalSwitch(slot);
        } else if (slot.swordHit
                && slot.entity.type() == EntityRomConstants.ENTITY_IRON_MASK
                && unmaskedIronMaskMotion.maskRemovalState(slot.entity.slot()) == 0
                && runtime.linkState.readLastRomLinkDirection() != roamingEnemyMotion.direction(slot.entity.slot())) {
            entitySwordImpactApplyMaskedIronMask(slot);
        } else if (slot.swordHit
                && RoomEntityCombatRules.swordPokeForSwordCollision(
                        slot.entity.type(),
                        slot.peaHatSwordClink
                                || slot.spikedBeetleSwordClink
                                || slot.maskedMimicSwordClink
                                || slot.rollingBonesBarSwordClink
                                || slot.moblinKingSwordClink)
                && !slot.moldormTailSwordHit) {
            entitySwordImpactApplySwordClink(slot);
        } else if (slot.swordHit && slot.entity.type() == EntityRomConstants.ENTITY_BOUNCING_BOMBITE) {
            entitySwordImpactLightBombite(slot);
        } else if (slot.swordHit) {
            EntityCombatResolver.entitySwordDamageApply(slot);
        }
        return true;
    }

    private static void entitySwordImpactApplyCrystalSwitch(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        if (slot.swordHit) {
            // The crystal handler uses the normal enemy-hit path for
            // the flash/ignore timers, but its preceding health write
            // prevents the generic damage path from killing it.
            runtime.combatTables.writeEnemyIgnoreHitsCountdown(
                    slot.entity.slot(), frame.attackContext().powerRecoil() ? 0x20 : 0x0A);
            runtime.combatTables.writeEnemyFlashCountdown(slot.entity.slot(), 0x18);
            var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
            enemyRecoilMotion.clear(slot.entity.slot());
            slot.soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
            slot.soundId = 0x03;
        }
    }

    private static void entitySwordImpactApplyMaskedIronMask(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // DefaultEnemyDamageCollisionHandler's masked Iron Mask branch
        // only accepts a sword hit from the direction the mask faces.
        // A rear hit pushes Link and the mask apart, then uses the
        // shared sword-poke presentation without applying damage.
        slot.swordPokeVfx =
                new EntityCombatEvent.SwordPokeVfx(byteValue(frame.swordX() - 0x08), byteValue(frame.swordY() - 0x08));
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0x10);
        var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.configure(
                slot.entity.slot(),
                slot.entity.x(),
                slot.entity.y(),
                slot.entity.z(),
                frame.linkEntityX(),
                frame.linkEntityY(),
                0x10);
        slot.soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
        slot.soundId = 0x07;
    }

    private static void entitySwordImpactApplySwordClink(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // EnemyCollidedWithSword's ENTITY_OPT1_SWORD_CLINK_OFF path
        // calls label_D07/label_D15: no damage or normal recoil,
        // sixteen ignored-hit frames, then the sword-poke VFX and
        // jingle writes.
        slot.swordPokeVfx =
                new EntityCombatEvent.SwordPokeVfx(byteValue(frame.swordX() - 0x08), byteValue(frame.swordY() - 0x08));
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0x10);
        var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(slot.entity.slot());
        slot.soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
        slot.soundId = 0x07;
    }

    private static void entitySwordImpactLightBombite(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // EnemyCollidedWithSword's Bombite branch is not a damage hit:
        // it enters state-$02, reverses the vector returned by
        // GetVectorTowardsLink, and starts the private lit window.
        var bombiteMotion = runtime.motions.get(BombiteMotion.class, BombiteMotion::new);
        bombiteMotion.enterBouncingLitFromSword(
                slot.entity.slot(), slot.entity, frame.linkEntityX(), frame.linkEntityY());
        runtime.physicsTables.writeEnemyTransitionCountdown(slot.entity.slot(), 0x40);
        runtime.bombInteractions.state.writeBombPrivateCountdown1(slot.entity.slot(), 0x08);
        var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(slot.entity.slot());
    }

    static void entitySwordStatusApply(EntityCombatSlot slot) {
        var runtime = slot.runtime;

        if (slot.enemySpecialAction == 0xFE) {
            entitySwordStatusBurn(slot);
        } else if (slot.enemySpecialAction == 0xFF) {
            entitySwordStatusStun(slot);
        } else if (slot.swordDamage > 0 && runtime.combatTables.readEnemyHealth(slot.entity.slot()) == 0) {
            entitySwordStatusDie(slot);
        } else if (slot.swordDamage > 0) {
            entitySwordStatusFlashSurvivor(slot);
        }
    }

    private static void entitySwordStatusBurn(EntityCombatSlot slot) {
        var runtime = slot.runtime;

        // ApplySwordDamagesToEnemy's burning branch starts the
        // shared $60 transition and emits NOISE_SFX_BURSTING_FLAME.
        runtime.physicsTables.writeEnemyTransitionCountdown(slot.entity.slot(), 0x60);
        runtime.combatTables.writeEnemyStunnedCountdown(slot.entity.slot(), 0);
        runtime.combatTables.writeEnemyFlashCountdown(slot.entity.slot(), 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0x0A);
        var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(slot.entity.slot());
        runtime.slots.replace(slot.entity.slot(), withStatus(slot.entity, EntityStatus.BURNING));
        slot.secondarySoundChannel = EntityCombatEvent.SoundChannel.NOISE;
        slot.secondarySoundId = 0x12;
    }

    private static void entitySwordStatusStun(EntityCombatSlot slot) {
        var runtime = slot.runtime;

        // EntityBecomeStunned uses private countdown 2 and clears
        // vertical speed; the runtime has no separate Z velocity
        // for this status, so the countdown is the authoritative
        // entitySwordStatusStun state here.
        runtime.physicsTables.writeEnemyTransitionCountdown(slot.entity.slot(), 0);
        runtime.combatTables.writeEnemyStunnedCountdown(slot.entity.slot(), 0xFF);
        runtime.combatTables.writeEnemyFlashCountdown(slot.entity.slot(), 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(slot.entity.slot(), 0x0A);
        var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(slot.entity.slot());
        runtime.slots.replace(slot.entity.slot(), withStatus(slot.entity, EntityStatus.STUNNED));
    }

    private static void entitySwordStatusDie(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // ApplySwordDamagesToEnemy falls through on death to
        // $03:73B6-73E4. Moldorm already received $28/$C8 above;
        // ordinary lethal hits also flash for $18. The recoil
        // caller retains its $20 ignore/$40 death override.
        // Final Nightmare ($E6), form 3, also uses $28/$C8
        // ($03:73BF-73C8); wire this when its form handler exists.
        if (slot.entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
            runtime.combatTables.writeEnemyFlashCountdown(slot.entity.slot(), 0x18);
        }
        runtime.combatTables.writeDyingCountdown(
                slot.entity.slot(),
                slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM
                        ? 0
                        : (frame.attackContext().powerRecoil() ? 0x40 : 0x2F));
        if (slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
            runtime.bossRewards.state.writeBossDeathProducerState(slot.entity.slot(), 0);
            var moblinKingMotion = runtime.motions.get(MoblinKingMotion.class, MoblinKingMotion::new);
            moblinKingMotion.setLethalHitFrame(slot.entity.slot(), (frame.frameCounter() & 255) + 1);
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
            runtime.bossRewards.state.writeBossDeathProducerState(slot.entity.slot(), 0);
            var moldormMotion = runtime.motions.get(MoldormMotion.class, MoldormMotion::new);
            moldormMotion.setDestructionTailState(slot.entity.slot(), 0);
        }
        if (slot.entity.type() == 0x12) {
            runtime.bossRewards.cascadeGhiniDeath(slot.entity, runtime.frameRandom.forFrame(frame.frameCounter()));
        }
        runtime.combatTables.writePowerRecoilDeath(
                slot.entity.slot(), frame.attackContext().powerRecoil());
        runtime.slots.replace(
                slot.entity.slot(),
                withDeathPresentation(
                        withStatus(slot.entity, EntityStatus.DYING),
                        -1,
                        runtime.combatTables.readPowerRecoilDeath(slot.entity.slot())));
    }

    private static void entitySwordStatusFlashSurvivor(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        // jr_003_73B6 and StartIgnoringHitsForEntity: a normal
        // sword hit flashes for $18 frames and suppresses the
        // next $0A collision passes.
        if (slot.entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
            runtime.combatTables.writeEnemyFlashCountdown(slot.entity.slot(), 0x18);
        }
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(
                slot.entity.slot(),
                slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM
                        ? 0
                        : frame.attackContext().powerRecoil() ? 0x20 : 0x0A);
        if (isZolGelType(slot.entity.type())) {
            var zolGelMotion = runtime.motions.get(ZolGelMotion.class, ZolGelMotion::new);
            zolGelMotion.onSwordHit(slot.entity.slot());
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_ZOL) {
            slot.entity = runtime.spriteHandlers.present(
                    slot.entity, runtime.slotProperties.spriteViews().forZolSlimeEye(), slot.entity.spriteVariant());
            runtime.slots.replace(slot.entity.slot(), slot.entity);
        }
    }

    static final class EntityCombatSlot {
        final EntityCombatResolver runtime;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private EnemyCombatInputs inputs;
        private List<linksawakening.world.entity.event.EntityCombatEvent> events;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private boolean booBuddyAllowsLinkCollision;
        private boolean booBuddyAllowsSwordCollision;
        private boolean peaHatGrounded;
        private boolean hidingZolSwordCollision;
        private boolean hidingZolLinkCollision;
        private int collisionFlashCountdown;
        private boolean goombaStomp;
        private boolean linkCollision;
        private boolean swordHit;
        private boolean moldormTailSwordHit;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.event.EntityCombatEvent.SoundChannel soundChannel;
        private int soundId;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.event.EntityCombatEvent.SoundChannel secondarySoundChannel;
        private int secondarySoundId;
        private int enemyDamage;
        private int enemySpecialAction;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.event.EntityCombatEvent.SwordPokeVfx swordPokeVfx;
        private boolean peaHatSwordClink;
        private boolean spikedBeetleSwordClink;
        private boolean maskedMimicSwordClink;
        private boolean rollingBonesBarSwordClink;
        private boolean moblinKingSwordClink;
        private int linkDamage;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.event.EntityCombatEvent.LinkCollisionResponse linkCollisionResponse;
        private boolean bodyRecoilBeforeSword;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RomEnemyCombatTables.SwordDamageResult swordResult;
        private int swordDamage;
        private boolean swordResultApplied;

        EntityCombatSlot(EntityCombatResolver runtime) {
            this.runtime = Objects.requireNonNull(runtime, "runtime");
        }

        void reset(
                EnemyCombatInputs inputs,
                List<linksawakening.world.entity.event.EntityCombatEvent> events,
                RoomEntity entity) {
            this.inputs = inputs;
            this.events = events;
            this.entity = entity;
            this.booBuddyAllowsLinkCollision = false;
            this.booBuddyAllowsSwordCollision = false;
            this.peaHatGrounded = false;
            this.hidingZolSwordCollision = false;
            this.hidingZolLinkCollision = false;
            this.collisionFlashCountdown = 0;
            this.goombaStomp = false;
            this.linkCollision = false;
            this.swordHit = false;
            this.moldormTailSwordHit = false;
            this.soundChannel = null;
            this.soundId = 0;
            this.secondarySoundChannel = null;
            this.secondarySoundId = 0;
            this.enemyDamage = 0;
            this.enemySpecialAction = 0;
            this.swordPokeVfx = null;
            this.peaHatSwordClink = false;
            this.spikedBeetleSwordClink = false;
            this.maskedMimicSwordClink = false;
            this.rollingBonesBarSwordClink = false;
            this.moblinKingSwordClink = false;
            this.linkDamage = 0;
            this.linkCollisionResponse = null;
            this.bodyRecoilBeforeSword = false;
            this.swordResult = null;
            this.swordDamage = 0;
            this.swordResultApplied = false;
        }
    }

    private static boolean resolveSlotCombat(EntityCombatSlot slot) {
        if (!entityCombatGatesAcceptInitialStatus(slot)) return false;
        if (!entityCombatGatesAcceptOrdinaryCollision(slot)) return false;
        if (!entityCombatGatesAcceptHandlerCollision(slot)) return false;
        if (!entityCombatGatesAcceptMovementState(slot)) return false;
        if (!entityCombatGatesAcceptDamageCountdowns(slot)) return false;
        if (!entityCombatOverlapProbeBodyAndSword(slot)) return false;
        if (!entityCombatOverlapApplyShieldContact(slot)) return false;
        if (!entityCombatOverlapAcceptContactOrStomp(slot)) return false;
        if (!entitySwordImpactPrepare(slot)) return false;
        if (!entitySwordImpactApply(slot)) return false;
        if (!prepareLinkContact(slot)) return false;
        if (!applyLinkContactRecoil(slot)) return false;
        if (!emitLinkContactOutcome(slot)) return false;
        return true;
    }

    private static boolean prepareLinkContact(EntityCombatSlot slot) {
        var runtime = slot.runtime;

        if (slot.linkCollision && slot.entity.type() == EntityRomConstants.ENTITY_GEL) {
            var zolGelMotion = runtime.motions.get(ZolGelMotion.class, ZolGelMotion::new);
            zolGelMotion.onLinkCollision(slot.entity.slot());
        }
        // ApplyLinkCollisionWithEnemy's Gel branch ($03:6D3D) returns
        // immediately after starting the cling, before damage/recoil.
        slot.linkDamage = slot.linkCollision && slot.entity.type() != EntityRomConstants.ENTITY_GEL
                ? runtime.slotProperties.contactDamage(slot.entity.slot())
                : 0;
        if ((runtime.physicsTables.readEnemyPhysicsFlags(slot.entity.slot())
                        & EntityRomConstants.ENTITY_PHYSICS_HARMLESS)
                != 0) {
            // func_003_6CC0 keeps CheckLinkCollisionWithEnemy's carry
            // result for harmless entities but suppresses Link damage.
            slot.linkDamage = 0;
        }
        slot.linkCollisionResponse = EntityCombatEvent.LinkCollisionResponse.NONE;
        // AnimateRoamingEnemy calls the body collision before the sword
        // pass ($03:585B/$6E28). Hitting a plain roaming enemy does not
        // cancel the recoil that its body contact already wrote.
        slot.bodyRecoilBeforeSword = slot.entity.type() == EntityRomConstants.ENTITY_OCTOROK
                || slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN;
        return true;
    }

    private static boolean applyLinkContactRecoil(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        if (slot.linkDamage > 0
                && (!slot.swordHit || slot.bodyRecoilBeforeSword)
                && RoomEntityCombatRules.usesStandardLinkContactResponse(slot.entity.type())) {
            if (slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR) {
                // $03:6DE9-6DED / $03:6FA7-6FB8: horizontal only.
                int speedX = ((frame.linkEntityX() - slot.entity.x()) & 0xFF) < 0x80 ? 0x18 : 0xE8;
                slot.linkCollisionResponse = new EntityCombatEvent.LinkCollisionResponse(speedX, 0, 0x10);
            } else {
                // $03:6DFA-6E02 selects the magnitude of Link's recoil.
                int speed = slot.entity.type() == EntityRomConstants.ENTITY_MOLDORM ? 0x18 : 0x14;
                EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                        slot.entity.x(),
                        slot.entity.y(),
                        slot.entity.z(),
                        frame.linkEntityX(),
                        frame.linkEntityY(),
                        speed);
                slot.linkCollisionResponse =
                        new EntityCombatEvent.LinkCollisionResponse(vector.x() & 0xFF, vector.y() & 0xFF, 0x10);
            }
        }
        var moblinKingMotion = runtime.motions.get(MoblinKingMotion.class, MoblinKingMotion::new);
        if (slot.linkDamage > 0
                && !slot.swordHit
                && slot.entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING
                && moblinKingMotion.state(slot.entity.slot()) == 9
                && runtime.physicsTables.readEnemyTransitionCountdown(slot.entity.slot()) == 0x60) {
            slot.linkCollisionResponse = new EntityCombatEvent.LinkCollisionResponse(
                    moblinKingMotion.speedX(slot.entity.slot()), 0, 0x28, 0x40);
        }
        return true;
    }

    private static boolean emitLinkContactOutcome(EntityCombatSlot slot) {
        slot.events.add(
                linksawakening.world.entity.event.EntityCombatEvent.builder(slot.entity.slot(), slot.entity.type())
                        .linkDamage(slot.linkDamage)
                        .swordHit(slot.swordHit)
                        .enemyDamage(slot.enemyDamage)
                        .enemySpecialAction(slot.enemySpecialAction)
                        .soundChannel(slot.soundChannel)
                        .soundId(slot.soundId)
                        .secondarySoundChannel(slot.secondarySoundChannel)
                        .secondarySoundId(slot.secondarySoundId)
                        .swordPokeVfx(slot.swordPokeVfx)
                        .linkCollisionResponse(slot.linkCollisionResponse)
                        .build());
        return true;
    }

    public List<EntityCombatEvent> resolveCombat(EntityAttackContactFrame inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        boolean linkAirborne = inputs.linkAirborne();
        int linkVerticalVelocity = inputs.linkVerticalVelocity();
        boolean linkInteractive = inputs.linkInteractive();
        boolean swordCollisionActive = inputs.swordCollisionActive();
        int swordX = inputs.swordX();
        int swordWidth = inputs.swordWidth();
        int swordY = inputs.swordY();
        int swordHeight = inputs.swordHeight();
        return resolveCombat(new EnemyCombatFrame(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkVerticalVelocity,
                linkInteractive,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                EnemyAttackContext.standard()));
    }

    public List<EntityCombatEvent> resolveCombat(EntitySwordContactFrame inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        boolean linkAirborne = inputs.linkAirborne();
        boolean linkInteractive = inputs.linkInteractive();
        boolean swordCollisionActive = inputs.swordCollisionActive();
        int swordX = inputs.swordX();
        int swordWidth = inputs.swordWidth();
        int swordY = inputs.swordY();
        int swordHeight = inputs.swordHeight();
        return resolveCombat(new EnemyCombatFrame(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                0,
                linkInteractive,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                EnemyAttackContext.standard()));
    }

    public List<EntityCombatEvent> resolveCombat(EntityVerticalContactFrame inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        boolean linkAirborne = inputs.linkAirborne();
        boolean linkInteractive = inputs.linkInteractive();
        boolean swordCollisionActive = inputs.swordCollisionActive();
        int swordX = inputs.swordX();
        int swordWidth = inputs.swordWidth();
        int swordY = inputs.swordY();
        int swordHeight = inputs.swordHeight();
        EnemyAttackContext attackContext = inputs.attackContext();
        return resolveCombat(new EnemyCombatFrame(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                0,
                linkInteractive,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                attackContext));
    }
}
