package linksawakening.app.gameplay;

import linksawakening.equipment.Sword;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.state.Direction;
import linksawakening.state.PowerUp;
import linksawakening.state.Tunic;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.room.EntityCombatInputs;
import linksawakening.world.room.SwordBox;
import linksawakening.world.transition.Warp;

final class CombatPhase {
    Sword.CollisionBox tick(GameplaySimulation context, boolean dialogBlocksGameplay) {
        Sword.CollisionBox swordBoxForEntityTick = Sword.CollisionBox.inactive();
        if (!context.scrollController().isActive()
                && !context.transitionController().isInputBlocked()
                && !context.inventoryController().shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {
            Sword sword = context.equipmentController().activeSword();
            swordBoxForEntityTick = sword == null
                    ? Sword.CollisionBox.inactive()
                    : sword.enemyCollisionBox(
                            context.link().position().romEntityX(),
                            context.link().position().romSwordCollisionY(),
                            context.link().animation().direction(),
                            context.playerState().equipmentLevels().runningWithPegasusBoots());
            EnemyAttackContext attackContext = new EnemyAttackContext(
                    context.playerState().equipmentLevels().swordLevel(),
                    context.playerState().equipmentLevels().tunicType() == Tunic.RED.romValue(),
                    context.playerState().equipmentLevels().activePowerUp() == PowerUp.PIECE_OF_POWER.romValue(),
                    sword != null && sword.spinAttackActive(),
                    context.playerState().equipmentLevels().runningWithPegasusBoots(),
                    context.link().equipment().isUsingShield(),
                    Direction.fromJava(context.link().animation().direction()).romIndex(),
                    context.link().animation().romEntityZ());
            var combatEvents = context.roomSession()
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            context.frameCounter(),
                            context.link().position().romEntityX(),
                            context.link().position().romEntityY(),
                            context.link().airborne().isAirborne(),
                            context.roomSession().rooms().mapCategory() == Warp.CATEGORY_SIDESCROLL
                                    ? context.link().position().romSpeedY()
                                    : context.link().airborne().zVelocity(),
                            !context.debugNoClip(),
                            new SwordBox(
                                    swordBoxForEntityTick.active(),
                                    swordBoxForEntityTick.x(),
                                    swordBoxForEntityTick.width(),
                                    swordBoxForEntityTick.y(),
                                    swordBoxForEntityTick.height()),
                            attackContext));
            EnemyCombatEventConsumer.consume(combatEvents, context.gameplaySoundSink(), context.transientVfxSystem());
            for (int eventIndex = 0; eventIndex < combatEvents.size(); eventIndex++) {
                EntityCombatEvent event = combatEvents.get(eventIndex);

                applyEvent(context, event);
            }
        }

        return swordBoxForEntityTick;
    }

    private static void applyEvent(GameplaySimulation context, EntityCombatEvent event) {
        if (event.linkDamage() > 0
                && context.playerState().vitals().invincibilityCounter() == 0
                && (!context.link().status().isCollisionDamageImmune())) {
            int previousPowerUp = context.playerState().equipmentLevels().activePowerUp();
            context.playerState().vitals().applyRomEnemyDamage(event.linkDamage());
            if (previousPowerUp != PowerUp.NONE.romValue()
                    && context.playerState().equipmentLevels().activePowerUp() == PowerUp.NONE.romValue()) {
                context.musicDirector().restoreMusicAfterPowerUpEnds(context.roomSession());
            }
            context.roomSession().entityState().resetGuardianAcornCounterOnHurt();
            if (event.linkCollisionResponse().active()) {
                EntityCombatEvent.LinkCollisionResponse response = event.linkCollisionResponse();
                if (!context.roomSession()
                        .entityState()
                        .deferContactRecoil(
                                event.slot(),
                                context.link().position().romEntityX(),
                                context.link().position().romEntityY())) {
                    context.link().position().applyRomSpeed(response.speedX(), response.speedY());
                }
                context.link().status().setCollisionIgnoreFrames(response.ignoreCollisionCountdown());
                if (response.invincibilityCountdown() != 0) {
                    context.playerState().vitals().setInvincibilityCounter(response.invincibilityCountdown());
                }
                context.playerState().equipmentLevels().setRunningWithPegasusBoots(false);
                // ApplyLinkCollisionWithEnemy writes WAVE_SFX_LINK_HURT
                // before the shared Link response helper.
                context.gameplaySoundSink().play(GameplaySoundEvent.LINK_HURT);
            }
        }
        if (event.linkDamage() == 0 && event.linkCollisionResponse().active()) {
            var response = event.linkCollisionResponse();
            context.link().position().applyRomSpeed(response.speedX(), response.speedY());
            context.link().status().setCollisionIgnoreFrames(response.ignoreCollisionCountdown());
            context.playerState().equipmentLevels().setRunningWithPegasusBoots(false);
        }
        if (event.linkAction() == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN) {
            context.link().airborne().bounceFromGoomba();
        } else if (event.linkAction() == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING) {
            context.link().position().applyRomSpeed(0, 0xF0);
        }
    }
}
