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

/** ROM sword and contact combat resolution, sharing the owning room slot table. */
final class EntityCombatResolver {
    private final RoomEntityRuntime runtime;
    EntityCombatResolver(RoomEntityRuntime runtime) { this.runtime = runtime; }

    List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 int linkVerticalVelocity,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight,
                                                 EnemyAttackContext attackContext) {
        runtime.validateByte(linkVerticalVelocity, "Link vertical velocity");
        if (attackContext == null) {
            throw new IllegalArgumentException("Enemy attack context cannot be null");
        }
        runtime.runningWithPegasusBoots = attackContext.pegasusBoots();
        List<EntityCombatEvent> events = new ArrayList<>();
        for (int index = runtime.slots.length - 1; index >= 0; index--) {
            RoomEntity entity = runtime.slots[index];
            if (!entity.loaded() || entity.status() != EntityStatus.ACTIVE
                || !RoomEntityCombatRules.supportsEnemyCollision(entity.type())) {
                continue;
            }
            if (entity.type() == runtime.ENTITY_HINOX
                && runtime.hinoxMotion.state(entity.slot()) == HinoxMotion.STATE_GRAB) {
                // HinoxState4Handler does not call DefaultEnemyDamageCollisionHandler.
                continue;
            }
            if (entity.type() == runtime.ENTITY_THREE_OF_A_KIND
                && runtime.threeOfAKindMotion.state(entity.slot()) == 2) {
                continue;
            }
            if (entity.type() == runtime.ENTITY_ROLLING_BONES_BAR && linkAirborne) {
                // RollingBonesBarEntityHandler returns before the default
                // collision handler whenever hLinkPositionZ is non-zero.
                continue;
            }
            if (entity.type() == runtime.ENTITY_PIRANHA_PLANT
                && runtime.piranhaMotion.state(entity.slot()) == 0) {
                // PiranhaPlantState0Handler keeps the plant hidden and does
                // not call DefaultEnemyDamageCollisionHandler.
                continue;
            }
            if (entity.type() == runtime.ENTITY_ZOMBIE
                && !runtime.zombieMotion.allowsEnemyCollision(entity.slot())) {
                // ZombieState2Handler is the only Zombie branch that calls
                // DefaultEnemyDamageCollisionHandler.
                continue;
            }
            if (entity.type() == runtime.ENTITY_BUZZ_BLOB && !linkInteractive) {
                // BuzzBlobEntityHandler returns before its default enemy
                // collision call when Link is non-interactive.
                continue;
            }
            if (entity.type() == runtime.ENTITY_SAND_CRAB && !linkInteractive) {
                // SandCrabEntityHandler returns before its default enemy
                // collision call when Link is non-interactive.
                continue;
            }
            if (entity.type() == runtime.ENTITY_FISH
                && !runtime.fishMotion.allowsEnemyCollision(entity.slot())) {
                continue;
            }
            if (entity.type() == runtime.ENTITY_CROW
                && !runtime.crowMotion.allowsEnemyCollision(entity.slot())) {
                continue;
            }
            boolean booBuddyAllowsLinkCollision = entity.type() != runtime.ENTITY_BOO_BUDDY
                || runtime.booBuddyMotion.allowsEnemyCollision(entity.slot(), runtime.booBuddyTriggerCount,
                    runtime.enemyTransitionCountdown[entity.slot()], swordCollisionActive);
            boolean booBuddyAllowsSwordCollision = entity.type() != runtime.ENTITY_BOO_BUDDY
                || runtime.booBuddyMotion.allowsSwordCollision(entity.slot(), runtime.booBuddyTriggerCount);
            if (!booBuddyAllowsLinkCollision && !booBuddyAllowsSwordCollision) {
                continue;
            }
            if (isGhiniType(entity.type())
                && (runtime.ghiniMotion.hidden(entity.slot()) || entity.spriteVariant() < 0)) {
                continue;
            }
            if (entity.type() == runtime.ENTITY_SPIKED_BEETLE
                && !runtime.spikedBeetleMotion.allowsCombat(entity.slot())) {
                continue;
            }
            if (entity.type() == runtime.ENTITY_ARMOS_STATUE
                && !runtime.armosMotion.isActive(entity.slot())) {
                continue;
            }
            // $15:4EFA skips DefaultEnemyDamageCollisionHandler only for
            // the jump's initial $08 transition countdown, not all airtime.
            if (entity.type() == runtime.ENTITY_STALFOS_EVASIVE
                && runtime.stalfosEvasiveMotion.privateState1(entity.slot()) == 0
                && runtime.enemyTransitionCountdown[entity.slot()] != 0) {
                continue;
            }
            if (entity.type() == runtime.ENTITY_LEEVER && !runtime.leeverMotion.isChasing(entity.slot())) {
                continue;
            }
            boolean peaHatGrounded = entity.type() != runtime.ENTITY_PEAHAT
                || runtime.peaHatMotion.isGrounded(entity);
            if (entity.type() == runtime.ENTITY_PAIRODD
                && !runtime.pairoddMotion.allowsEnemyCollision(entity.slot())) {
                continue;
            }
            if (entity.type() == runtime.ENTITY_WIZROBE && runtime.wizrobeMotion.state(entity.slot()) != 3) {
                continue;
            }
            if (entity.type() == runtime.ENTITY_POKEY && runtime.pokeyMotion.isSegment(entity.slot())
                && runtime.enemyTransitionCountdown[entity.slot()] != 0) {
                // The detached branch calls DefaultEnemyDamageCollisionHandler
                // only after its transition countdown reaches zero.
                continue;
            }
            if (entity.type() == runtime.ENTITY_WINGED_OCTOROK
                && (runtime.wingedOctorokMotion.state(entity.slot()) == 2
                    || runtime.wingedStateTwoThisFrame[entity.slot()])) {
                // WingedOctorokEntityHandler returns directly from its jump
                // state instead of calling DefaultEnemyDamageCollisionHandler.
                continue;
            }
            if (isZolGelType(entity.type()) && runtime.zolGelMotion.skipsEnemyCollision(entity.slot())) {
                continue;
            }
            boolean hidingZolSwordCollision = entity.type() != runtime.ENTITY_HIDING_ZOL
                || runtime.hidingZolMotion.allowsSwordCollision(entity.slot());
            boolean hidingZolLinkCollision = entity.type() != runtime.ENTITY_HIDING_ZOL
                || runtime.hidingZolMotion.allowsLinkCollision(entity.slot());
            if (!hidingZolSwordCollision && !hidingZolLinkCollision) {
                continue;
            }
            // AnimateEntities decrements flash before dispatching Rolling Bones.
            // Main's combat pass precedes tick(), so read that impending value.
            int collisionFlashCountdown = Math.max(0, runtime.enemyFlashCountdown[entity.slot()]
                - (entity.type() == runtime.ENTITY_ROLLING_BONES ? 1 : 0));
            if ((collisionFlashCountdown > 0
                    && entity.type() != runtime.ENTITY_MOLDORM)
                || runtime.enemyIgnoreHitsCountdown[entity.slot()] > 0) {
                continue;
            }
            if ((entity.type() == runtime.ENTITY_TIMER_BOMBITE
                    && runtime.bombiteMotion.state(entity.slot()) == 1)
                || (entity.type() == runtime.ENTITY_BOUNCING_BOMBITE
                    && runtime.bombPrivateCountdown1[entity.slot()] > 0)) {
                continue;
            }

            boolean goombaStomp = runtime.canGoombaStomp(entity, linkEntityX, linkEntityY,
                linkAirborne, linkVerticalVelocity, linkInteractive, frameCounter);
            boolean linkCollision = entity.type() != runtime.ENTITY_LIKE_LIKE
                && !linkAirborne && linkInteractive
                && booBuddyAllowsLinkCollision
                && RoomEntityCombatRules.collisionCadenceMatches(frameCounter, entity.slot())
                && peaHatGrounded
                && hidingZolLinkCollision
                && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY);
            boolean swordHit = swordCollisionActive
                && booBuddyAllowsSwordCollision
                && !runtime.wingedSwordAttackThisFrame[entity.slot()]
                && (entity.type() != runtime.ENTITY_LIKE_LIKE
                    || runtime.likeLikeMotion.state(entity.slot()) == 0)
                && hidingZolSwordCollision
                && runtime.moldormOrHeadOverlapsSword(
                    entity, swordX, swordWidth, swordY, swordHeight);
            // $20:4B4A supplies the shield rectangle to the same collision
            // pass as the sword, but $03:6EA5 dispatches it separately.
            if (entity.type() == runtime.ENTITY_SPIKED_BEETLE && linkInteractive
                && attackContext.usingShield() && !swordCollisionActive
                && runtime.enemyIgnoreHitsCountdown[entity.slot()] == 0
                && (runtime.enemyFlashCountdown[entity.slot()] == 0
                    || runtime.enemyFlashCountdown[entity.slot()] >= 0x18)) {
                int direction = attackContext.romDirection();
                int shieldX = attackContext.shieldX(linkEntityX);
                int shieldY = attackContext.shieldY(linkEntityY);
                if (shieldX != 0 && RoomEntityCombatRules.overlapsSword(entity,
                    shieldX, attackContext.shieldWidth(),
                    shieldY, attackContext.shieldHeight())) {
                    if (runtime.spikedBeetleMotion.state(entity.slot()) != 3) {
                        runtime.spikedBeetleMotion.flipFromShield(entity.slot(), direction);
                        runtime.enemyTransitionCountdown[entity.slot()] = 0xFF;
                    }
                    // func_003_6F5C clears the ignore-hits countdown after
                    // the shared shield bump; it does not damage the shell.
                    runtime.enemyIgnoreHitsCountdown[entity.slot()] = 0;
                    runtime.enemyRecoilMotion.clear(entity.slot());
                    EnemyRecoilMotion.Vector vector =
                        EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                            entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x12);
                    events.add(new EntityCombatEvent(entity.slot(), entity.type(), 0,
                        false, 0, -1, EntityCombatEvent.SoundChannel.JINGLE, 0x09,
                        EntityCombatEvent.SoundChannel.NONE, -1, null,
                        new EntityCombatEvent.LinkCollisionResponse(
                            vector.x() & 0xFF, vector.y() & 0xFF, 0x0C)));
                    continue;
                }
            }
            boolean moldormTailSwordHit = swordHit && entity.type() == runtime.ENTITY_MOLDORM
                && !RoomEntityCombatRules.overlapsSword(
                    entity, swordX, swordWidth, swordY, swordHeight);
            if (!linkCollision && !swordHit) {
                if (!goombaStomp) {
                    continue;
                }
            }

            if (goombaStomp) {
                runtime.goombaMotion.enterStomp(entity.slot());
                runtime.enemyTransitionCountdown[entity.slot()] = 0x30;
                EntityCombatEvent.LinkAction linkAction = runtime.groundInteractionSideScrolling
                    ? EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING
                    : EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN;
                events.add(new EntityCombatEvent(
                    entity.slot(), entity.type(), 0, false, 0, -1,
                    EntityCombatEvent.SoundChannel.WAVE, 0x0E,
                    EntityCombatEvent.SoundChannel.NONE, -1, null, linkAction));
                continue;
            }

            EntityCombatEvent.SoundChannel soundChannel = EntityCombatEvent.SoundChannel.NONE;
            int soundId = -1;
            EntityCombatEvent.SoundChannel secondarySoundChannel =
                EntityCombatEvent.SoundChannel.NONE;
            int secondarySoundId = -1;
            int enemyDamage = 0;
            int enemySpecialAction = -1;
            EntityCombatEvent.SwordPokeVfx swordPokeVfx = null;
            boolean peaHatSwordClink = entity.type() == runtime.ENTITY_PEAHAT && !peaHatGrounded;
            boolean spikedBeetleSwordClink = entity.type() == runtime.ENTITY_SPIKED_BEETLE
                && (runtime.options1(entity.slot()) & 0x40) != 0;
            boolean maskedMimicSwordClink = entity.type() == runtime.ENTITY_MASKED_MIMIC_GORIYA
                && (runtime.options1(entity.slot()) & 0x40) != 0;
            boolean rollingBonesBarSwordClink = entity.type() == runtime.ENTITY_ROLLING_BONES_BAR;
            boolean moblinKingSwordClink = entity.type() == runtime.ENTITY_MOBLIN_KING
                && runtime.moblinKingMotion.state(entity.slot()) != 5;
            if (entity.type() == runtime.ENTITY_CRYSTAL_SWITCH) {
                if (swordHit) {
                    // The crystal handler uses the normal enemy-hit path for
                    // the flash/ignore timers, but its preceding health write
                    // prevents the generic damage path from killing it.
                    runtime.enemyIgnoreHitsCountdown[entity.slot()] = attackContext.powerRecoil()
                        ? 0x20 : 0x0A;
                    runtime.enemyFlashCountdown[entity.slot()] = 0x18;
                    runtime.enemyRecoilMotion.clear(entity.slot());
                    soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                    soundId = 0x03;
                }
            } else if (swordHit && entity.type() == runtime.ENTITY_IRON_MASK
                && runtime.ironMaskPrivateState2[entity.slot()] == 0
                && runtime.lastRomLinkDirection != runtime.roamingEnemyMotion.direction(entity.slot())) {
                // DefaultEnemyDamageCollisionHandler's masked Iron Mask branch
                // only accepts a sword hit from the direction the mask faces.
                // A rear hit pushes Link and the mask apart, then uses the
                // shared sword-poke presentation without applying damage.
                swordPokeVfx = new EntityCombatEvent.SwordPokeVfx(
                    byteValue(swordX - 0x08), byteValue(swordY - 0x08));
                runtime.enemyIgnoreHitsCountdown[entity.slot()] = 0x10;
                runtime.enemyRecoilMotion.configure(
                    entity.slot(), entity.x(), entity.y(), entity.z(),
                    linkEntityX, linkEntityY, 0x10);
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = 0x07;
            } else if (swordHit && RoomEntityCombatRules.swordPokeForSwordCollision(
                entity.type(), peaHatSwordClink || spikedBeetleSwordClink
                    || maskedMimicSwordClink
                    || rollingBonesBarSwordClink || moblinKingSwordClink)
                && !moldormTailSwordHit) {
                // EnemyCollidedWithSword's ENTITY_OPT1_SWORD_CLINK_OFF path
                // calls label_D07/label_D15: no damage or normal recoil,
                // sixteen ignored-hit frames, then the sword-poke VFX and
                // jingle writes.
                swordPokeVfx = new EntityCombatEvent.SwordPokeVfx(
                    byteValue(swordX - 0x08), byteValue(swordY - 0x08));
                runtime.enemyIgnoreHitsCountdown[entity.slot()] = 0x10;
                runtime.enemyRecoilMotion.clear(entity.slot());
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = 0x07;
            } else if (swordHit && entity.type() == runtime.ENTITY_BOUNCING_BOMBITE) {
                // EnemyCollidedWithSword's Bombite branch is not a damage hit:
                // it enters state-$02, reverses the vector returned by
                // GetVectorTowardsLink, and starts the private lit window.
                runtime.bombiteMotion.enterBouncingLitFromSword(
                    entity.slot(), entity, linkEntityX, linkEntityY);
                runtime.enemyTransitionCountdown[entity.slot()] = 0x40;
                runtime.bombPrivateCountdown1[entity.slot()] = 0x08;
                runtime.enemyRecoilMotion.clear(entity.slot());
            } else if (swordHit) {
                RomEnemyCombatTables.SwordDamageResult swordResult =
                    runtime.enemyCombatTables == null ? null
                        : runtime.enemyCombatTables.resolveSwordDamage(entity.type(), attackContext);
                int swordDamage = swordResult == null
                    ? RoomEntityCombatRules.basicSwordDamage(entity.type())
                    : swordResult.numericDamage();
                enemyDamage = swordDamage;
                if (swordResult != null) {
                    enemySpecialAction = swordResult.specialAction();
                }
                boolean swordResultApplied = swordResult == null
                    ? swordDamage > 0
                    : !swordResult.ignored();
                if (usesSharedRecoil(entity.type()) || entity.type() == runtime.ENTITY_MOBLIN_KING) {
                    // EnemyCollidedWithSword applies the default `$30` recoil
                    // before ApplySwordDamagesToEnemy changes health.
                    runtime.enemyRecoilMotion.configure(
                        entity.slot(), entity.x(), entity.y(), entity.z(),
                        linkEntityX, linkEntityY, 0x30);
                }
                // ConfigureEntityRecoil reaches StartIgnoringHitsForEntity
                // before ApplySwordDamagesToEnemy, including lethal hits.
                runtime.enemyIgnoreHitsCountdown[entity.slot()] = attackContext.powerRecoil()
                    ? 0x20 : 0x0A;
                if (swordDamage > 0) {
                    // $06:6CEB checks existing recoil before $6CEE applies a new hit.
                    // Main resolves combat before ticking handlers, so preserve that phase.
                    if (entity.type() == runtime.ENTITY_ROLLING_BONES)
                        runtime.rollingBonesSwordHitFrame[entity.slot()] = (frameCounter & 0xFF) + 1;
                    runtime.enemyHealth[entity.slot()] = Math.max(0,
                        runtime.enemyHealth[entity.slot()] - swordDamage);
                    if (entity.type() == runtime.ENTITY_MOLDORM) {
                        runtime.moldormMotion.onSwordHit(entity.slot());
                        runtime.enemyFlashCountdown[entity.slot()] = 0x28;
                        runtime.enemyIgnoreHitsCountdown[entity.slot()] = 0;
                    }
                }
                soundChannel = EntityCombatEvent.SoundChannel.JINGLE;
                soundId = swordResultApplied ? 0x03 : 0x09;
                if (enemySpecialAction == 0xFE) {
                    // ApplySwordDamagesToEnemy's burning branch starts the
                    // shared $60 transition and emits NOISE_SFX_BURSTING_FLAME.
                    runtime.enemyTransitionCountdown[entity.slot()] = 0x60;
                    runtime.enemyStunnedCountdown[entity.slot()] = 0;
                    runtime.enemyFlashCountdown[entity.slot()] = 0;
                    runtime.enemyIgnoreHitsCountdown[entity.slot()] = 0x0A;
                    runtime.enemyRecoilMotion.clear(entity.slot());
                    runtime.slots[entity.slot()] = withStatus(entity, EntityStatus.BURNING);
                    secondarySoundChannel = EntityCombatEvent.SoundChannel.NOISE;
                    secondarySoundId = 0x12;
                } else if (enemySpecialAction == 0xFF) {
                    // EntityBecomeStunned uses private countdown 2 and clears
                    // vertical speed; the runtime has no separate Z velocity
                    // for this status, so the countdown is the authoritative
                    // stun state here.
                    runtime.enemyTransitionCountdown[entity.slot()] = 0;
                    runtime.enemyStunnedCountdown[entity.slot()] = 0xFF;
                    runtime.enemyFlashCountdown[entity.slot()] = 0;
                    runtime.enemyIgnoreHitsCountdown[entity.slot()] = 0x0A;
                    runtime.enemyRecoilMotion.clear(entity.slot());
                    runtime.slots[entity.slot()] = withStatus(entity, EntityStatus.STUNNED);
                } else if (swordDamage > 0 && runtime.enemyHealth[entity.slot()] == 0) {
                    runtime.dyingCountdown[entity.slot()] = entity.type() == runtime.ENTITY_MOLDORM
                        ? 0 : (attackContext.powerRecoil() ? 0x40 : 0x2F);
                    if (entity.type() == runtime.ENTITY_MOBLIN_KING) {
                        runtime.bossDeathProducerState[entity.slot()] = 0;
                        runtime.moblinKingLethalHitFrame[entity.slot()] = (frameCounter & 255) + 1;
                    }
                    if (entity.type() == runtime.ENTITY_MOLDORM) {
                        runtime.bossDeathProducerState[entity.slot()] = 0;
                        runtime.moldormDestructionTailState[entity.slot()] = 0;
                    }
                    runtime.powerRecoilDeath[entity.slot()] = attackContext.powerRecoil();
                    runtime.slots[entity.slot()] = withDeathPresentation(
                        withStatus(entity, EntityStatus.DYING), -1,
                        runtime.powerRecoilDeath[entity.slot()]);
                } else if (swordDamage > 0) {
                    // jr_003_73B6 and StartIgnoringHitsForEntity: a normal
                    // sword hit flashes for $18 frames and suppresses the
                    // next $0A collision passes.
                    if (entity.type() != runtime.ENTITY_MOLDORM) {
                        runtime.enemyFlashCountdown[entity.slot()] = 0x18;
                    }
                    runtime.enemyIgnoreHitsCountdown[entity.slot()] = entity.type() == runtime.ENTITY_MOLDORM
                        ? 0 : attackContext.powerRecoil() ? 0x20 : 0x0A;
                    if (isZolGelType(entity.type())) {
                        runtime.zolGelMotion.onSwordHit(entity.slot());
                    }
                    if (entity.type() == runtime.ENTITY_ZOL && runtime.spriteHandlers != null) {
                        entity = withDefinition(entity, runtime.spriteHandlers.forZolSlimeEye(),
                            entity.spriteVariant());
                        runtime.slots[entity.slot()] = entity;
                    }
                }
            }
            if (linkCollision && entity.type() == runtime.ENTITY_GEL) {
                runtime.zolGelMotion.onLinkCollision(entity.slot());
            }
            // ApplyLinkCollisionWithEnemy's Gel branch ($03:6D3D) returns
            // immediately after starting the cling, before damage/recoil.
            int linkDamage = linkCollision && entity.type() != runtime.ENTITY_GEL
                ? runtime.contactDamage(entity.type()) : 0;
            if ((runtime.enemyPhysicsFlags[entity.slot()] & runtime.ENTITY_PHYSICS_HARMLESS) != 0) {
                // func_003_6CC0 keeps CheckLinkCollisionWithEnemy's carry
                // result for harmless entities but suppresses Link damage.
                linkDamage = 0;
            }
            EntityCombatEvent.LinkCollisionResponse linkCollisionResponse =
                EntityCombatEvent.LinkCollisionResponse.NONE;
            if (linkDamage > 0 && !swordHit
                && RoomEntityCombatRules.usesStandardLinkContactResponse(entity.type())) {
                EnemyRecoilMotion.Vector vector = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                    entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x14);
                linkCollisionResponse = new EntityCombatEvent.LinkCollisionResponse(
                    vector.x() & 0xFF, vector.y() & 0xFF, 0x10);
            }
            if (linkDamage > 0 && !swordHit && entity.type() == runtime.ENTITY_MOBLIN_KING
                && runtime.moblinKingMotion.state(entity.slot()) == 9
                && runtime.enemyTransitionCountdown[entity.slot()] == 0x60) {
                linkCollisionResponse = new EntityCombatEvent.LinkCollisionResponse(
                    runtime.moblinKingMotion.speedX(entity.slot()), 0, 0x28, 0x40);
            }
            events.add(new EntityCombatEvent(
                entity.slot(), entity.type(),
                linkDamage,
                swordHit, enemyDamage, enemySpecialAction, soundChannel, soundId,
                secondarySoundChannel, secondarySoundId, swordPokeVfx,
                linkCollisionResponse));
        }
        return List.copyOf(events);
        }
}
