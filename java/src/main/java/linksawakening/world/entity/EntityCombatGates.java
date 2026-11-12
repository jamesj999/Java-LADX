package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.HinoxMotion;

/** Ordered combatgates operations for a single slot. */
final class EntityCombatGates {
    static boolean acceptInitialStatus(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        if (slot.entity.loaded() && slot.entity.type() == 0x5B) {
            runtime.slimeEye()
                    .resolveCombat(new SlimeEyeCombatFrame(
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
        if (slot.entity.type() == EntityRomConstants.ENTITY_HINOX
                && runtime.motions().get(HinoxMotion.class, HinoxMotion::new).state(slot.entity.slot())
                        == HinoxMotion.STATE_GRAB) {
            // HinoxState4Handler does not call DefaultEnemyDamageCollisionHandler.
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_THREE_OF_A_KIND
                && runtime.motions()
                                .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                                .state(slot.entity.slot())
                        == 2) {
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES_BAR && frame.linkAirborne()) {
            // RollingBonesBarEntityHandler returns before the default
            // collision handler whenever hLinkPositionZ is non-zero.
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_PIRANHA_PLANT
                && runtime.motions()
                                .get(PiranhaMotion.class, PiranhaMotion::new)
                                .state(slot.entity.slot())
                        == 0) {
            // PiranhaPlantState0Handler keeps the plant hidden and does
            // not call DefaultEnemyDamageCollisionHandler.
            return false;
        }
        return true;
    }

    static boolean acceptOrdinaryCollision(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        if (slot.entity.type() == EntityRomConstants.ENTITY_ZOMBIE
                && !runtime.motions()
                        .get(ZombieMotion.class, ZombieMotion::new)
                        .allowsEnemyCollision(slot.entity.slot())) {
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
        if (slot.entity.type() == EntityRomConstants.ENTITY_FISH
                && !runtime.motions().get(FishMotion.class, FishMotion::new).allowsEnemyCollision(slot.entity.slot())) {
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_CROW
                && !runtime.motions().get(CrowMotion.class, CrowMotion::new).allowsEnemyCollision(slot.entity.slot())) {
            return false;
        }
        return true;
    }

    static boolean acceptHandlerCollision(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.booBuddyAllowsLinkCollision = slot.entity.type() != EntityRomConstants.ENTITY_BOO_BUDDY
                || runtime.motions()
                        .get(BooBuddyMotion.class, BooBuddyMotion::new)
                        .allowsEnemyCollision(
                                slot.entity.slot(),
                                runtime.motions()
                                        .get(BooBuddyMotion.class, BooBuddyMotion::new)
                                        .readBooBuddyTriggerCount(),
                                runtime.physicsTables().readEnemyTransitionCountdown(slot.entity.slot()),
                                frame.swordCollisionActive());
        slot.booBuddyAllowsSwordCollision = slot.entity.type() != EntityRomConstants.ENTITY_BOO_BUDDY
                || runtime.motions()
                        .get(BooBuddyMotion.class, BooBuddyMotion::new)
                        .allowsSwordCollision(
                                slot.entity.slot(),
                                runtime.motions()
                                        .get(BooBuddyMotion.class, BooBuddyMotion::new)
                                        .readBooBuddyTriggerCount());
        if (!slot.booBuddyAllowsLinkCollision && !slot.booBuddyAllowsSwordCollision) {
            return false;
        }
        if (isGhiniType(slot.entity.type())
                && (runtime.motions().get(GhiniMotion.class, GhiniMotion::new).hidden(slot.entity.slot())
                        || slot.entity.spriteVariant() < 0)) {
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_SPIKED_BEETLE
                && !runtime.motions()
                        .get(SpikedBeetleMotion.class, SpikedBeetleMotion::new)
                        .allowsCombat(slot.entity.slot())) {
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_ARMOS_STATUE
                && !runtime.motions().get(ArmosMotion.class, ArmosMotion::new).isActive(slot.entity.slot())) {
            return false;
        }
        // $15:4EFA skips DefaultEnemyDamageCollisionHandler only for
        // the jump's initial $08 transition countdown, not all airtime.
        if (slot.entity.type() == EntityRomConstants.ENTITY_STALFOS_EVASIVE
                && runtime.motions()
                                .get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                                .privateState1(slot.entity.slot())
                        == 0
                && runtime.physicsTables().readEnemyTransitionCountdown(slot.entity.slot()) != 0) {
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_LEEVER
                && !runtime.motions().get(LeeverMotion.class, LeeverMotion::new).isChasing(slot.entity.slot())) {
            return false;
        }
        return true;
    }

    static boolean acceptMovementState(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.peaHatGrounded = slot.entity.type() != EntityRomConstants.ENTITY_PEAHAT
                || runtime.motions().get(PeaHatMotion.class, PeaHatMotion::new).isGrounded(slot.entity);
        if (slot.entity.type() == EntityRomConstants.ENTITY_PAIRODD
                && !runtime.motions()
                        .get(PairoddMotion.class, PairoddMotion::new)
                        .allowsEnemyCollision(slot.entity.slot())) {
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_WIZROBE
                && runtime.motions()
                                .get(WizrobeMotion.class, WizrobeMotion::new)
                                .state(slot.entity.slot())
                        != 3) {
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_POKEY
                && runtime.motions().get(PokeyMotion.class, PokeyMotion::new).isSegment(slot.entity.slot())
                && runtime.physicsTables().readEnemyTransitionCountdown(slot.entity.slot()) != 0) {
            // The detached branch calls DefaultEnemyDamageCollisionHandler
            // only after its transition countdown reaches zero.
            return false;
        }
        if (slot.entity.type() == EntityRomConstants.ENTITY_WINGED_OCTOROK
                && (runtime.motions()
                                        .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                                        .state(slot.entity.slot())
                                == 2
                        || runtime.slotProperties().readWingedStateTwoThisFrame(slot.entity.slot()))) {
            // WingedOctorokEntityHandler returns directly from its jump
            // state instead of calling DefaultEnemyDamageCollisionHandler.
            return false;
        }
        if (isZolGelType(slot.entity.type())
                && runtime.motions()
                        .get(ZolGelMotion.class, ZolGelMotion::new)
                        .skipsEnemyCollision(slot.entity.slot())) {
            return false;
        }
        return true;
    }

    static boolean acceptDamageCountdowns(EntityCombatSlot slot) {
        var runtime = slot.runtime;
        var frame = slot.inputs;

        slot.hidingZolSwordCollision = slot.entity.type() != EntityRomConstants.ENTITY_HIDING_ZOL
                || runtime.motions()
                        .get(HidingZolMotion.class, HidingZolMotion::new)
                        .allowsSwordCollision(slot.entity.slot());
        slot.hidingZolLinkCollision = slot.entity.type() != EntityRomConstants.ENTITY_HIDING_ZOL
                || runtime.motions()
                        .get(HidingZolMotion.class, HidingZolMotion::new)
                        .allowsLinkCollision(slot.entity.slot());
        if (!slot.hidingZolSwordCollision && !slot.hidingZolLinkCollision) {
            return false;
        }
        // AnimateEntities decrements flash before dispatching Rolling Bones.
        // Main's combat pass precedes tick(), so read that impending value.
        slot.collisionFlashCountdown = Math.max(
                0,
                runtime.combatTables().readEnemyFlashCountdown(slot.entity.slot())
                        - (slot.entity.type() == EntityRomConstants.ENTITY_ROLLING_BONES ? 1 : 0));
        if ((slot.collisionFlashCountdown > 0 && slot.entity.type() != EntityRomConstants.ENTITY_MOLDORM)
                || runtime.combatTables().readEnemyIgnoreHitsCountdown(slot.entity.slot()) > 0) {
            return false;
        }
        if ((slot.entity.type() == EntityRomConstants.ENTITY_TIMER_BOMBITE
                        && runtime.motions()
                                        .get(BombiteMotion.class, BombiteMotion::new)
                                        .state(slot.entity.slot())
                                == 1)
                || (slot.entity.type() == EntityRomConstants.ENTITY_BOUNCING_BOMBITE
                        && runtime.bombInteractions().readBombPrivateCountdown1(slot.entity.slot()) > 0)) {
            return false;
        }
        return true;
    }
}
