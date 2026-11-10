package linksawakening.app.gameplay;



import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.Warp;

import static linksawakening.world.room.RoomConstants.*;

import static linksawakening.app.gameplay.GameplaySimulation.*;

final class CombatPhase {
    Sword.CollisionBox tick(GameplaySimulation context, boolean dialogBlocksGameplay) {
        var roomSession = context.roomSession();
        var transitionController = context.transitionController();
        var debugNoClip = context.debugNoClip();
        var frameCounter = context.frameCounter();
        var scrollController = context.scrollController();
        var playerState = context.playerState();
        var inventoryController = context.inventoryController();
        var gameplaySoundSink = context.gameplaySoundSink();
        var transientVfxSystem = context.transientVfxSystem();
        var equipmentController = context.equipmentController();
        var link = context.link();
        var direction = context.direction();
            Sword.CollisionBox swordBoxForEntityTick = Sword.CollisionBox.inactive();
            if (link != null && roomSession != null
                && !scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {
                Sword sword = equipmentController.activeSword();
                swordBoxForEntityTick = sword == null
                    ? Sword.CollisionBox.inactive()
                    : sword.enemyCollisionBox(link.romEntityX(), link.romSwordCollisionY(),
                        link.direction(), playerState.runningWithPegasusBoots());
                EnemyAttackContext attackContext = new EnemyAttackContext(
                    playerState.swordLevel(),
                    playerState.tunicType() == PlayerState.TUNIC_RED,
                    playerState.activePowerUp() == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER,
                    sword != null && sword.spinAttackActive(),
                    playerState.runningWithPegasusBoots(), link.isUsingShield(),
                    direction.applyAsInt(link.direction()), link.romEntityZ());
                var combatEvents = roomSession.resolveEntityCombat(
                    frameCounter, link.romEntityX(), link.romEntityY(),
                    link.isAirborne(),
                    roomSession.mapCategory() == Warp.CATEGORY_SIDESCROLL
                        ? link.romSpeedY() : link.zVelocity(), !debugNoClip,
                    swordBoxForEntityTick.active(), swordBoxForEntityTick.x(),
                    swordBoxForEntityTick.width(), swordBoxForEntityTick.y(),
                    swordBoxForEntityTick.height(), attackContext);
                EnemyCombatEventConsumer.consume(combatEvents, gameplaySoundSink, transientVfxSystem);
                for (EntityCombatEvent event : combatEvents) {
                    if (event.linkDamage() > 0 && playerState.invincibilityCounter() == 0
                        && (link == null || !link.isCollisionDamageImmune())) {
                        playerState.applyRomEnemyDamage(event.linkDamage());
                        roomSession.resetGuardianAcornCounterOnHurt();
                        if (event.linkCollisionResponse().active()) {
                            EntityCombatEvent.LinkCollisionResponse response =
                                event.linkCollisionResponse();
                            if (!roomSession.deferContactRecoil(event.slot(), link.romEntityX(), link.romEntityY())) {
                                link.applyRomSpeed(response.speedX(), response.speedY());
                            }
                            link.setCollisionIgnoreFrames(response.ignoreCollisionCountdown());
                            if (response.invincibilityCountdown() != 0) {
                                playerState.setInvincibilityCounter(
                                    response.invincibilityCountdown());
                            }
                            playerState.setRunningWithPegasusBoots(false);
                            // ApplyLinkCollisionWithEnemy writes WAVE_SFX_LINK_HURT
                            // before the shared Link response helper.
                            gameplaySoundSink.play(GameplaySoundEvent.LINK_HURT);
                        }
                    }
                    if (event.linkDamage() == 0 && event.linkCollisionResponse().active()) {
                        var response = event.linkCollisionResponse();
                        link.applyRomSpeed(response.speedX(), response.speedY());
                        link.setCollisionIgnoreFrames(response.ignoreCollisionCountdown());
                        playerState.setRunningWithPegasusBoots(false);
                    }
                    if (event.linkAction()
                        == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN) {
                        link.bounceFromGoomba();
                    } else if (event.linkAction()
                        == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING) {
                        link.applyRomSpeed(0, 0xF0);
                    }
                }
            }

        return swordBoxForEntityTick;
    }
}
