package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.HinoxMotion;

/** ROM sword and contact combat resolution, sharing the owning room slot table. */
final class EntityCombatResolver {
    private final EntityCombatResolverServices runtime;
    private final boolean[] deferredLeeverContact = new boolean[RoomEntity.SLOT_COUNT];
    private final int[] contactLinkX = new int[RoomEntity.SLOT_COUNT];
    private final int[] contactLinkY = new int[RoomEntity.SLOT_COUNT];

    EntityCombatResolver(EntityCombatResolverServices runtime) {
        this.runtime = runtime;
    }

    boolean deferContactRecoil(int slot, int linkX, int linkY) {
        RoomEntity entity = runtime.slots()[slot];
        if (entity.type() != EntityRomConstants.ENTITY_LEEVER || entity.status() != EntityStatus.ACTIVE) return false;
        deferredLeeverContact[slot] = true;
        contactLinkX[slot] = linkX;
        contactLinkY[slot] = linkY;
        return true;
    }

    void clearDeferredContactRecoil() {
        java.util.Arrays.fill(deferredLeeverContact, false);
    }

    void finishLeeverContactRecoil(RoomEntity moved) {
        int slot = moved.slot();
        if (!deferredLeeverContact[slot]) return;
        deferredLeeverContact[slot] = false;
        // $04:7F01 moves before $7F49 calls the shared collision handler.
        // Its hitbox uses cached hActiveEntityPosX/Y, but $03:7565 reads
        // the updated position tables when selecting Link's recoil vector.
        var vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                moved.x(), moved.y(), moved.z(), contactLinkX[slot], contactLinkY[slot], 0x14);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        slot,
                        moved.type(),
                        0,
                        false,
                        0,
                        -1,
                        EntityCombatEvent.SoundChannel.NONE,
                        -1,
                        EntityCombatEvent.SoundChannel.NONE,
                        -1,
                        null,
                        EntityCombatEvent.LinkAction.NONE,
                        new EntityCombatEvent.LinkCollisionResponse(vector.x() & 255, vector.y() & 255, 0x10)));
    }

    List<EntityCombatEvent> resolveCombat(EnemyCombatFrame inputs) {
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
        EnemyAttackContext attackContext = inputs.attackContext();
        EntitySlotValidation.validateByte(linkVerticalVelocity, "Link vertical velocity");
        clearDeferredContactRecoil();
        if (attackContext == null) {
            throw new IllegalArgumentException("Enemy attack context cannot be null");
        }
        runtime.linkState().runningWithPegasusBoots = attackContext.pegasusBoots();
        List<EntityCombatEvent> events = runtime.events().outbox.channel(EntityCombatEvent.class, "resolvedCombat");
        events.clear();
        for (int index = runtime.slots().length - 1; index >= 0; index--) {
            RoomEntity entity = runtime.slots()[index];
            if (entity.loaded() && entity.type() == 0x5B) {
                runtime.slimeEye()
                        .resolveCombat(new SlimeEyeCombatFrame(
                                entity,
                                frameCounter,
                                linkEntityX,
                                linkEntityY,
                                linkAirborne,
                                linkInteractive,
                                swordCollisionActive,
                                swordX,
                                swordWidth,
                                swordY,
                                swordHeight,
                                attackContext,
                                events));
                continue;
            }
            if (!entity.loaded()
                    || entity.status() != EntityStatus.ACTIVE
                    || !RoomEntityCombatRules.supportsEnemyCollision(entity.type())) {
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_HINOX
                    && runtime.motions()
                                    .get(HinoxMotion.class, HinoxMotion::new)
                                    .state(entity.slot())
                            == HinoxMotion.STATE_GRAB) {
                // HinoxState4Handler does not call DefaultEnemyDamageCollisionHandler.
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_THREE_OF_A_KIND
                    && runtime.motions()
                                    .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                                    .state(entity.slot())
                            == 2) {
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR && linkAirborne) {
                // RollingBonesBarEntityHandler returns before the default
                // collision handler whenever hLinkPositionZ is non-zero.
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_PIRANHA_PLANT
                    && runtime.motions()
                                    .get(PiranhaMotion.class, PiranhaMotion::new)
                                    .state(entity.slot())
                            == 0) {
                // PiranhaPlantState0Handler keeps the plant hidden and does
                // not call DefaultEnemyDamageCollisionHandler.
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_ZOMBIE
                    && !runtime.motions()
                            .get(ZombieMotion.class, ZombieMotion::new)
                            .allowsEnemyCollision(entity.slot())) {
                // ZombieState2Handler is the only Zombie branch that calls
                // DefaultEnemyDamageCollisionHandler.
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_BUZZ_BLOB && !linkInteractive) {
                // BuzzBlobEntityHandler returns before its default enemy
                // collision call when Link is non-interactive.
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_SAND_CRAB && !linkInteractive) {
                // SandCrabEntityHandler returns before its default enemy
                // collision call when Link is non-interactive.
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_FISH
                    && !runtime.motions().get(FishMotion.class, FishMotion::new).allowsEnemyCollision(entity.slot())) {
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_CROW
                    && !runtime.motions().get(CrowMotion.class, CrowMotion::new).allowsEnemyCollision(entity.slot())) {
                continue;
            }
            boolean booBuddyAllowsLinkCollision = entity.type() != EntityRomConstants.ENTITY_BOO_BUDDY
                    || runtime.motions()
                            .get(BooBuddyMotion.class, BooBuddyMotion::new)
                            .allowsEnemyCollision(
                                    entity.slot(),
                                    runtime.motions()
                                            .get(BooBuddyMotion.class, BooBuddyMotion::new)
                                            .readBooBuddyTriggerCount(),
                                    runtime.physicsTables().readEnemyTransitionCountdown(entity.slot()),
                                    swordCollisionActive);
            boolean booBuddyAllowsSwordCollision = entity.type() != EntityRomConstants.ENTITY_BOO_BUDDY
                    || runtime.motions()
                            .get(BooBuddyMotion.class, BooBuddyMotion::new)
                            .allowsSwordCollision(
                                    entity.slot(),
                                    runtime.motions()
                                            .get(BooBuddyMotion.class, BooBuddyMotion::new)
                                            .readBooBuddyTriggerCount());
            if (!booBuddyAllowsLinkCollision && !booBuddyAllowsSwordCollision) {
                continue;
            }
            if (isGhiniType(entity.type())
                    && (runtime.motions()
                                    .get(GhiniMotion.class, GhiniMotion::new)
                                    .hidden(entity.slot())
                            || entity.spriteVariant() < 0)) {
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE
                    && !runtime.motions()
                            .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                            .allowsCombat(entity.slot())) {
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_ARMOS_STATUE
                    && !runtime.motions()
                            .get(ArmosMotion.class, ArmosMotion::new)
                            .isActive(entity.slot())) {
                continue;
            }
            // $15:4EFA skips DefaultEnemyDamageCollisionHandler only for
            // the jump's initial $08 transition countdown, not all airtime.
            if (entity.type() == EntityRomConstants.ENTITY_STALFOS_EVASIVE
                    && runtime.motions()
                                    .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                                    .privateState1(entity.slot())
                            == 0
                    && runtime.physicsTables().readEnemyTransitionCountdown(entity.slot()) != 0) {
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_LEEVER
                    && !runtime.motions()
                            .get(LeeverMotion.class, LeeverMotion::new)
                            .isChasing(entity.slot())) {
                continue;
            }
            boolean peaHatGrounded = entity.type() != EntityRomConstants.ENTITY_PEAHAT
                    || runtime.motions()
                            .get(PeaHatMotion.class, PeaHatMotion::new)
                            .isGrounded(entity);
            if (entity.type() == EntityRomConstants.ENTITY_PAIRODD
                    && !runtime.motions()
                            .get(PairoddMotion.class, PairoddMotion::new)
                            .allowsEnemyCollision(entity.slot())) {
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_WIZROBE
                    && runtime.motions()
                                    .get(WizrobeMotion.class, WizrobeMotion::new)
                                    .state(entity.slot())
                            != 3) {
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_POKEY
                    && runtime.motions()
                            .get(PokeyMotion.class, PokeyMotion::new)
                            .isSegment(entity.slot())
                    && runtime.physicsTables().readEnemyTransitionCountdown(entity.slot()) != 0) {
                // The detached branch calls DefaultEnemyDamageCollisionHandler
                // only after its transition countdown reaches zero.
                continue;
            }
            if (entity.type() == EntityRomConstants.ENTITY_WINGED_OCTOROK
                    && (runtime.motions()
                                            .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                                            .state(entity.slot())
                                    == 2
                            || runtime.slotProperties().readWingedStateTwoThisFrame(entity.slot()))) {
                // WingedOctorokEntityHandler returns directly from its jump
                // state instead of calling DefaultEnemyDamageCollisionHandler.
                continue;
            }
            if (isZolGelType(entity.type())
                    && runtime.motions()
                            .get(ZolGelMotion.class, ZolGelMotion::new)
                            .skipsEnemyCollision(entity.slot())) {
                continue;
            }
            boolean hidingZolSwordCollision = entity.type() != EntityRomConstants.ENTITY_HIDING_ZOL
                    || runtime.motions()
                            .get(HidingZolMotion.class, HidingZolMotion::new)
                            .allowsSwordCollision(entity.slot());
            boolean hidingZolLinkCollision = entity.type() != EntityRomConstants.ENTITY_HIDING_ZOL
                    || runtime.motions()
                            .get(HidingZolMotion.class, HidingZolMotion::new)
                            .allowsLinkCollision(entity.slot());
            if (!hidingZolSwordCollision && !hidingZolLinkCollision) {
                continue;
            }
            // AnimateEntities decrements flash before dispatching Rolling Bones.
            // Main's combat pass precedes tick(), so read that impending value.
            int collisionFlashCountdown = Math.max(
                    0,
                    runtime.combatTables().readEnemyFlashCountdown(entity.slot())
                            - (entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES ? 1 : 0));
            if ((collisionFlashCountdown > 0 && entity.type() != EntityRomConstants.ENTITY_MOLDORM)
                    || runtime.combatTables().readEnemyIgnoreHitsCountdown(entity.slot()) > 0) {
                continue;
            }
            if ((entity.type() == EntityRomConstants.ENTITY_TIMER_BOMBITE
                            && runtime.motions()
                                            .get(BombiteMotion.class, BombiteMotion::new)
                                            .state(entity.slot())
                                    == 1)
                    || (entity.type() == EntityRomConstants.ENTITY_BOUNCING_BOMBITE
                            && runtime.bombInteractions().readBombPrivateCountdown1(entity.slot()) > 0)) {
                continue;
            }

            boolean goombaStomp = runtime.roomInspection()
                    .canGoombaStomp(
                            entity,
                            linkEntityX,
                            linkEntityY,
                            linkAirborne,
                            linkVerticalVelocity,
                            linkInteractive,
                            frameCounter);
            boolean linkCollision = entity.type() != EntityRomConstants.ENTITY_LIKE_LIKE
                    && !linkAirborne
                    && linkInteractive
                    && booBuddyAllowsLinkCollision
                    && RoomEntityCombatRules.collisionCadenceMatches(frameCounter, entity.slot())
                    && peaHatGrounded
                    && hidingZolLinkCollision
                    && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY);
            boolean swordHit = swordCollisionActive
                    && booBuddyAllowsSwordCollision
                    && !runtime.slotProperties().readWingedSwordAttackThisFrame(entity.slot())
                    && (entity.type() != EntityRomConstants.ENTITY_LIKE_LIKE
                            || runtime.motions()
                                            .get(LikeLikeMotion.class, LikeLikeMotion::new)
                                            .state(entity.slot())
                                    == 0)
                    && hidingZolSwordCollision
                    && runtime.backgroundMechanics()
                            .moldormOrHeadOverlapsSword(entity, swordX, swordWidth, swordY, swordHeight);
            // $20:4B4A supplies the shield rectangle to the same collision
            // pass as the sword, but $03:6EA5 dispatches it separately.
            if (entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE
                    && linkInteractive
                    && attackContext.usingShield()
                    && !swordCollisionActive
                    && runtime.combatTables().readEnemyIgnoreHitsCountdown(entity.slot()) == 0
                    && (runtime.combatTables().readEnemyFlashCountdown(entity.slot()) == 0
                            || runtime.combatTables().readEnemyFlashCountdown(entity.slot()) >= 0x18)) {
                int direction = attackContext.romDirection();
                int shieldX = attackContext.shieldX(linkEntityX);
                int shieldY = attackContext.shieldY(linkEntityY);
                if (shieldX != 0
                        && RoomEntityCombatRules.overlapsSword(
                                entity, shieldX, attackContext.shieldWidth(), shieldY, attackContext.shieldHeight())) {
                    if (runtime.motions()
                                    .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                                    .state(entity.slot())
                            != 3) {
                        runtime.motions()
                                .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                                .flipFromShield(entity.slot(), direction);
                        runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 0xFF);
                    }
                    // func_003_6F5C clears the ignore-hits countdown after
                    // the shared shield bump; it does not damage the shell.
                    runtime.combatTables().writeEnemyIgnoreHitsCountdown(entity.slot(), 0);
                    runtime.motions()
                            .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                            .clear(entity.slot());
                    EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                            entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x12);
                    events.add(new EntityCombatEvent(
                            entity.slot(),
                            entity.type(),
                            0,
                            false,
                            0,
                            -1,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x09,
                            EntityCombatEvent.SoundChannel.NONE,
                            -1,
                            null,
                            new EntityCombatEvent.LinkCollisionResponse(vector.x() & 0xFF, vector.y() & 0xFF, 0x0C)));
                    continue;
                }
            }
            boolean moldormTailSwordHit = swordHit
                    && entity.type() == EntityRomConstants.ENTITY_MOLDORM
                    && !RoomEntityCombatRules.overlapsSword(entity, swordX, swordWidth, swordY, swordHeight);
            if (!linkCollision && !swordHit) {
                if (!goombaStomp) {
                    continue;
                }
            }

            if (goombaStomp) {
                runtime.motions().get(GoombaMotion.class, GoombaMotion::new).enterStomp(entity.slot());
                runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 0x30);
                EntityCombatEvent.LinkAction linkAction =
                        runtime.physicsTables().readGroundInteractionSideScrolling()
                                ? EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING
                                : EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN;
                events.add(new EntityCombatEvent(
                        entity.slot(),
                        entity.type(),
                        0,
                        false,
                        0,
                        -1,
                        EntityCombatEvent.SoundChannel.WAVE,
                        0x0E,
                        EntityCombatEvent.SoundChannel.NONE,
                        -1,
                        null,
                        linkAction));
                continue;
            }

            EntityCombatEvent.SoundChannel soundChannel = EntityCombatEvent.SoundChannel.NONE;
            int soundId = -1;
            EntityCombatEvent.SoundChannel secondarySoundChannel = EntityCombatEvent.SoundChannel.NONE;
            int secondarySoundId = -1;
            int enemyDamage = 0;
            int enemySpecialAction = -1;
            EntityCombatEvent.SwordPokeVfx swordPokeVfx = null;
            boolean peaHatSwordClink = entity.type() == EntityRomConstants.ENTITY_PEAHAT && !peaHatGrounded;
            boolean spikedBeetleSwordClink = entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE
                    && (runtime.options1(entity.slot()) & 0x40) != 0;
            boolean maskedMimicSwordClink = entity.type() == EntityRomConstants.ENTITY_MASKED_MIMIC_GORIYA
                    && (runtime.options1(entity.slot()) & 0x40) != 0;
            boolean rollingBonesBarSwordClink = entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR;
            boolean moblinKingSwordClink = entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING
                    && runtime.motions()
                                    .get(MoblinKingMotion.class, MoblinKingMotion::new)
                                    .state(entity.slot())
                            != 5;
            if (entity.type() == EntityRomConstants.ENTITY_CRYSTAL_SWITCH) {
                if (swordHit) {
                    // The crystal handler uses the normal enemy-hit path for
                    // the flash/ignore timers, but its preceding health write
                    // prevents the generic damage path from killing it.
                    runtime.combatTables()
                            .writeEnemyIgnoreHitsCountdown(entity.slot(), attackContext.powerRecoil() ? 0x20 : 0x0A);
                    runtime.combatTables().writeEnemyFlashCountdown(entity.slot(), 0x18);
                    runtime.motions()
                            .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                            .clear(entity.slot());
                    soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                    soundId = 0x03;
                }
            } else if (swordHit
                    && entity.type() == EntityRomConstants.ENTITY_IRON_MASK
                    && runtime.motions()
                                    .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                                    .maskRemovalState(entity.slot())
                            == 0
                    && runtime.linkState().lastRomLinkDirection
                            != runtime.motions()
                                    .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                    .direction(entity.slot())) {
                // DefaultEnemyDamageCollisionHandler's masked Iron Mask branch
                // only accepts a sword hit from the direction the mask faces.
                // A rear hit pushes Link and the mask apart, then uses the
                // shared sword-poke presentation without applying damage.
                swordPokeVfx = new EntityCombatEvent.SwordPokeVfx(byteValue(swordX - 0x08), byteValue(swordY - 0x08));
                runtime.combatTables().writeEnemyIgnoreHitsCountdown(entity.slot(), 0x10);
                runtime.motions()
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .configure(entity.slot(), entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x10);
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = 0x07;
            } else if (swordHit
                    && RoomEntityCombatRules.swordPokeForSwordCollision(
                            entity.type(),
                            peaHatSwordClink
                                    || spikedBeetleSwordClink
                                    || maskedMimicSwordClink
                                    || rollingBonesBarSwordClink
                                    || moblinKingSwordClink)
                    && !moldormTailSwordHit) {
                // EnemyCollidedWithSword's ENTITY_OPT1_SWORD_CLINK_OFF path
                // calls label_D07/label_D15: no damage or normal recoil,
                // sixteen ignored-hit frames, then the sword-poke VFX and
                // jingle writes.
                swordPokeVfx = new EntityCombatEvent.SwordPokeVfx(byteValue(swordX - 0x08), byteValue(swordY - 0x08));
                runtime.combatTables().writeEnemyIgnoreHitsCountdown(entity.slot(), 0x10);
                runtime.motions()
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .clear(entity.slot());
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = 0x07;
            } else if (swordHit && entity.type() == EntityRomConstants.ENTITY_BOUNCING_BOMBITE) {
                // EnemyCollidedWithSword's Bombite branch is not a damage hit:
                // it enters state-$02, reverses the vector returned by
                // GetVectorTowardsLink, and starts the private lit window.
                runtime.motions()
                        .get(BombiteMotion.class, BombiteMotion::new)
                        .enterBouncingLitFromSword(entity.slot(), entity, linkEntityX, linkEntityY);
                runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 0x40);
                runtime.bombInteractions().writeBombPrivateCountdown1(entity.slot(), 0x08);
                runtime.motions()
                        .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                        .clear(entity.slot());
            } else if (swordHit) {
                RomEnemyCombatTables.SwordDamageResult swordResult = runtime.enemyCombatTables() == null
                        ? null
                        : runtime.enemyCombatTables()
                                .resolveSwordDamage(
                                        entity.type(),
                                        runtime.combatTables().readEnemyHealthGroup(entity.slot()),
                                        attackContext);
                int swordDamage = swordResult == null
                        ? RoomEntityCombatRules.basicSwordDamage(entity.type())
                        : swordResult.numericDamage();
                enemyDamage = swordDamage;
                if (swordResult != null) {
                    enemySpecialAction = swordResult.specialAction();
                }
                boolean swordResultApplied = swordResult == null ? swordDamage > 0 : !swordResult.ignored();
                if (usesSharedRecoil(entity.type()) || entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
                    // EnemyCollidedWithSword applies the default `$30` recoil
                    // before ApplySwordDamagesToEnemy changes health.
                    runtime.motions()
                            .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                            .configure(
                                    entity.slot(), entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x30);
                }
                // ConfigureEntityRecoil reaches StartIgnoringHitsForEntity
                // before ApplySwordDamagesToEnemy, including lethal hits.
                runtime.combatTables()
                        .writeEnemyIgnoreHitsCountdown(entity.slot(), attackContext.powerRecoil() ? 0x20 : 0x0A);
                if (swordDamage > 0) {
                    // $06:6CEB checks existing recoil before $6CEE applies a new hit.
                    // Main resolves combat before ticking handlers, so preserve that phase.
                    if (entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES)
                        runtime.motions()
                                .get(RollingBonesMotion.class, RollingBonesMotion::new)
                                .setSwordHitFrame(entity.slot(), (frameCounter & 0xFF) + 1);
                    runtime.combatTables()
                            .writeEnemyHealth(
                                    entity.slot(),
                                    Math.max(0, runtime.combatTables().readEnemyHealth(entity.slot()) - swordDamage));
                    if (entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
                        runtime.motions()
                                .get(MoldormMotion.class, MoldormMotion::new)
                                .onSwordHit(entity.slot());
                        runtime.combatTables().writeEnemyFlashCountdown(entity.slot(), 0x28);
                        runtime.combatTables().writeEnemyIgnoreHitsCountdown(entity.slot(), 0);
                    }
                }
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = swordResultApplied ? 0x03 : 0x09;
                if (enemySpecialAction == 0xFE) {
                    // ApplySwordDamagesToEnemy's burning branch starts the
                    // shared $60 transition and emits NOISE_SFX_BURSTING_FLAME.
                    runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 0x60);
                    runtime.combatTables().writeEnemyStunnedCountdown(entity.slot(), 0);
                    runtime.combatTables().writeEnemyFlashCountdown(entity.slot(), 0);
                    runtime.combatTables().writeEnemyIgnoreHitsCountdown(entity.slot(), 0x0A);
                    runtime.motions()
                            .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                            .clear(entity.slot());
                    runtime.slots()[entity.slot()] = withStatus(entity, EntityStatus.BURNING);
                    secondarySoundChannel = EntityCombatEvent.SoundChannel.NOISE;
                    secondarySoundId = 0x12;
                } else if (enemySpecialAction == 0xFF) {
                    // EntityBecomeStunned uses private countdown 2 and clears
                    // vertical speed; the runtime has no separate Z velocity
                    // for this status, so the countdown is the authoritative
                    // stun state here.
                    runtime.physicsTables().writeEnemyTransitionCountdown(entity.slot(), 0);
                    runtime.combatTables().writeEnemyStunnedCountdown(entity.slot(), 0xFF);
                    runtime.combatTables().writeEnemyFlashCountdown(entity.slot(), 0);
                    runtime.combatTables().writeEnemyIgnoreHitsCountdown(entity.slot(), 0x0A);
                    runtime.motions()
                            .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                            .clear(entity.slot());
                    runtime.slots()[entity.slot()] = withStatus(entity, EntityStatus.STUNNED);
                } else if (swordDamage > 0 && runtime.combatTables().readEnemyHealth(entity.slot()) == 0) {
                    // ApplySwordDamagesToEnemy falls through on death to
                    // $03:73B6-73E4. Moldorm already received $28/$C8 above;
                    // ordinary lethal hits also flash for $18. The recoil
                    // caller retains its $20 ignore/$40 death override.
                    // Final Nightmare ($E6), form 3, also uses $28/$C8
                    // ($03:73BF-73C8); wire this when its form handler exists.
                    if (entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
                        runtime.combatTables().writeEnemyFlashCountdown(entity.slot(), 0x18);
                    }
                    runtime.combatTables()
                            .writeDyingCountdown(
                                    entity.slot(),
                                    entity.type() == EntityRomConstants.ENTITY_MOLDORM
                                            ? 0
                                            : (attackContext.powerRecoil() ? 0x40 : 0x2F));
                    if (entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
                        runtime.bossRewards().writeBossDeathProducerState(entity.slot(), 0);
                        runtime.motions()
                                .get(MoblinKingMotion.class, MoblinKingMotion::new)
                                .setLethalHitFrame(entity.slot(), (frameCounter & 255) + 1);
                    }
                    if (entity.type() == EntityRomConstants.ENTITY_MOLDORM) {
                        runtime.bossRewards().writeBossDeathProducerState(entity.slot(), 0);
                        runtime.motions()
                                .get(MoldormMotion.class, MoldormMotion::new)
                                .setDestructionTailState(entity.slot(), 0);
                    }
                    if (entity.type() == 0x12) {
                        runtime.bossRewards()
                                .cascadeGhiniDeath(
                                        entity,
                                        runtime.frameContext(frameCounter).randomByteSupplier());
                    }
                    runtime.combatTables().writePowerRecoilDeath(entity.slot(), attackContext.powerRecoil());
                    runtime.slots()[entity.slot()] = withDeathPresentation(
                            withStatus(entity, EntityStatus.DYING),
                            -1,
                            runtime.combatTables().readPowerRecoilDeath(entity.slot()));
                } else if (swordDamage > 0) {
                    // jr_003_73B6 and StartIgnoringHitsForEntity: a normal
                    // sword hit flashes for $18 frames and suppresses the
                    // next $0A collision passes.
                    if (entity.type() != EntityRomConstants.ENTITY_MOLDORM) {
                        runtime.combatTables().writeEnemyFlashCountdown(entity.slot(), 0x18);
                    }
                    runtime.combatTables()
                            .writeEnemyIgnoreHitsCountdown(
                                    entity.slot(),
                                    entity.type() == EntityRomConstants.ENTITY_MOLDORM
                                            ? 0
                                            : attackContext.powerRecoil() ? 0x20 : 0x0A);
                    if (isZolGelType(entity.type())) {
                        runtime.motions()
                                .get(ZolGelMotion.class, ZolGelMotion::new)
                                .onSwordHit(entity.slot());
                    }
                    if (entity.type() == EntityRomConstants.ENTITY_ZOL && runtime.spriteHandlers() != null) {
                        entity = withDefinition(
                                entity, runtime.spriteHandlers().forZolSlimeEye(), entity.spriteVariant());
                        runtime.slots()[entity.slot()] = entity;
                    }
                }
            }
            if (linkCollision && entity.type() == EntityRomConstants.ENTITY_GEL) {
                runtime.motions().get(ZolGelMotion.class, ZolGelMotion::new).onLinkCollision(entity.slot());
            }
            // ApplyLinkCollisionWithEnemy's Gel branch ($03:6D3D) returns
            // immediately after starting the cling, before damage/recoil.
            int linkDamage = linkCollision && entity.type() != EntityRomConstants.ENTITY_GEL
                    ? runtime.contactDamage(entity.slot())
                    : 0;
            if ((runtime.physicsTables().readEnemyPhysicsFlags(entity.slot())
                            & EntityRomConstants.ENTITY_PHYSICS_HARMLESS)
                    != 0) {
                // func_003_6CC0 keeps CheckLinkCollisionWithEnemy's carry
                // result for harmless entities but suppresses Link damage.
                linkDamage = 0;
            }
            EntityCombatEvent.LinkCollisionResponse linkCollisionResponse =
                    EntityCombatEvent.LinkCollisionResponse.NONE;
            // AnimateRoamingEnemy calls the body collision before the sword
            // pass ($03:585B/$6E28). Hitting a plain roaming enemy does not
            // cancel the recoil that its body contact already wrote.
            boolean bodyRecoilBeforeSword = entity.type() == EntityRomConstants.ENTITY_OCTOROK
                    || entity.type() == EntityRomConstants.ENTITY_MOBLIN;
            if (linkDamage > 0
                    && (!swordHit || bodyRecoilBeforeSword)
                    && RoomEntityCombatRules.usesStandardLinkContactResponse(entity.type())) {
                if (entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR) {
                    // $03:6DE9-6DED / $03:6FA7-6FB8: horizontal only.
                    int speedX = ((linkEntityX - entity.x()) & 0xFF) < 0x80 ? 0x18 : 0xE8;
                    linkCollisionResponse = new EntityCombatEvent.LinkCollisionResponse(speedX, 0, 0x10);
                } else {
                    // $03:6DFA-6E02 selects the magnitude of Link's recoil.
                    int speed = entity.type() == EntityRomConstants.ENTITY_MOLDORM ? 0x18 : 0x14;
                    EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                            entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, speed);
                    linkCollisionResponse =
                            new EntityCombatEvent.LinkCollisionResponse(vector.x() & 0xFF, vector.y() & 0xFF, 0x10);
                }
            }
            if (linkDamage > 0
                    && !swordHit
                    && entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING
                    && runtime.motions()
                                    .get(MoblinKingMotion.class, MoblinKingMotion::new)
                                    .state(entity.slot())
                            == 9
                    && runtime.physicsTables().readEnemyTransitionCountdown(entity.slot()) == 0x60) {
                linkCollisionResponse = new EntityCombatEvent.LinkCollisionResponse(
                        runtime.motions()
                                .get(MoblinKingMotion.class, MoblinKingMotion::new)
                                .speedX(entity.slot()),
                        0,
                        0x28,
                        0x40);
            }
            events.add(new EntityCombatEvent(
                    entity.slot(),
                    entity.type(),
                    linkDamage,
                    swordHit,
                    enemyDamage,
                    enemySpecialAction,
                    soundChannel,
                    soundId,
                    secondarySoundChannel,
                    secondarySoundId,
                    swordPokeVfx,
                    linkCollisionResponse));
        }
        return runtime.events().outbox.drain(EntityCombatEvent.class, "resolvedCombat");
    }
}
