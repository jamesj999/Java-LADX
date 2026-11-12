package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

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
                            context.link().romEntityX(),
                            context.link().romSwordCollisionY(),
                            context.link().direction(),
                            context.playerState().runningWithPegasusBoots());
            EnemyAttackContext attackContext = new EnemyAttackContext(
                    context.playerState().swordLevel(),
                    context.playerState().tunicType() == Tunic.RED.romValue(),
                    context.playerState().activePowerUp() == PowerUp.PIECE_OF_POWER.romValue(),
                    sword != null && sword.spinAttackActive(),
                    context.playerState().runningWithPegasusBoots(),
                    context.link().isUsingShield(),
                    Direction.fromJava(context.link().direction()).romIndex(),
                    context.link().romEntityZ());
            var combatEvents = context.roomSession()
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            context.frameCounter(),
                            context.link().romEntityX(),
                            context.link().romEntityY(),
                            context.link().isAirborne(),
                            context.roomSession().rooms().mapCategory() == Warp.CATEGORY_SIDESCROLL
                                    ? context.link().romSpeedY()
                                    : context.link().zVelocity(),
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

                if (event.linkDamage() > 0
                        && context.playerState().invincibilityCounter() == 0
                        && (!context.link().isCollisionDamageImmune())) {
                    int previousPowerUp = context.playerState().activePowerUp();
                    context.playerState().applyRomEnemyDamage(event.linkDamage());
                    if (previousPowerUp != PowerUp.NONE.romValue()
                            && context.playerState().activePowerUp() == PowerUp.NONE.romValue()) {
                        context.musicDirector().restoreMusicAfterPowerUpEnds(context.roomSession());
                    }
                    context.roomSession().entityState().resetGuardianAcornCounterOnHurt();
                    if (event.linkCollisionResponse().active()) {
                        EntityCombatEvent.LinkCollisionResponse response = event.linkCollisionResponse();
                        if (!context.roomSession()
                                .entityState()
                                .deferContactRecoil(
                                        event.slot(),
                                        context.link().romEntityX(),
                                        context.link().romEntityY())) {
                            context.link().applyRomSpeed(response.speedX(), response.speedY());
                        }
                        context.link().setCollisionIgnoreFrames(response.ignoreCollisionCountdown());
                        if (response.invincibilityCountdown() != 0) {
                            context.playerState().setInvincibilityCounter(response.invincibilityCountdown());
                        }
                        context.playerState().setRunningWithPegasusBoots(false);
                        // ApplyLinkCollisionWithEnemy writes WAVE_SFX_LINK_HURT
                        // before the shared Link response helper.
                        context.gameplaySoundSink().play(GameplaySoundEvent.LINK_HURT);
                    }
                }
                if (event.linkDamage() == 0 && event.linkCollisionResponse().active()) {
                    var response = event.linkCollisionResponse();
                    context.link().applyRomSpeed(response.speedX(), response.speedY());
                    context.link().setCollisionIgnoreFrames(response.ignoreCollisionCountdown());
                    context.playerState().setRunningWithPegasusBoots(false);
                }
                if (event.linkAction() == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN) {
                    context.link().bounceFromGoomba();
                } else if (event.linkAction() == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING) {
                    context.link().applyRomSpeed(0, 0xF0);
                }
            }
        }

        return swordBoxForEntityTick;
    }
}
