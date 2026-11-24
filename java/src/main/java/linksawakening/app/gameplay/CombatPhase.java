package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.state.GameplaySounds;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.state.PowerUp;
import linksawakening.state.Tunic;
import linksawakening.ui.InventoryController;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Applies sword, shield and contact combat before entity ticks. Retained for one simulation. */
public final class CombatPhase {
    private final EquipmentController equipmentController;
    private final FrameInputCache frameInputs;
    private final GameplaySounds sounds;
    private final InventoryController inventoryController;
    private final Link playerLink;
    private final MusicDirector musicDirector;
    private final PlayerState playerState;
    private final RoomSession rooms;
    private final ScrollController scrollController;
    private final TransientVfxSystem transientVfxSystem;
    private final TransitionController transitionController;

    private final Sword.CollisionBox NO_SWORD = Sword.CollisionBox.inactive();
    private int bladeClashCountdown;
    /** hLinkCountdown after a Goomba stomp ($03:6D1B), decremented before contact ($00:10DF). */
    private int goombaBounceCountdown;

    private boolean wasRunningWithPegasusBoots;

    public CombatPhase(Inputs inputs) {
        this.equipmentController = Objects.requireNonNull(inputs.equipmentController);
        this.frameInputs = Objects.requireNonNull(inputs.frameInputs);
        this.sounds = Objects.requireNonNull(inputs.gameplaySoundSink);
        this.inventoryController = Objects.requireNonNull(inputs.inventoryController);
        this.playerLink = Objects.requireNonNull(inputs.link);
        this.musicDirector = Objects.requireNonNull(inputs.musicDirector);
        this.playerState = Objects.requireNonNull(inputs.playerState);
        this.rooms = Objects.requireNonNull(inputs.roomSession);
        this.scrollController = Objects.requireNonNull(inputs.scrollController);
        this.transientVfxSystem = Objects.requireNonNull(inputs.transientVfxSystem);
        this.transitionController = Objects.requireNonNull(inputs.transitionController);
    }

    public Sword.CollisionBox tick(boolean dialogBlocksGameplay, int swordCollisionY) {
        if (bladeClashCountdown > 0) bladeClashCountdown--;
        if (goombaBounceCountdown > 0) goombaBounceCountdown--;
        Sword.CollisionBox swordBoxForEntityTick = NO_SWORD;
        if (!this.scrollController.isActive()
                && !this.transitionController.isInputBlocked()
                && !this.inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {
            Sword sword = this.equipmentController.activeSword();
            boolean runningWithBoots = this.playerState.equipmentLevels().runningWithPegasusBoots();
            if (sword != null && runningWithBoots && !wasRunningWithPegasusBoots) sword.setHitTarget(0);
            wasRunningWithPegasusBoots = runningWithBoots;
            swordBoxForEntityTick = sword == null
                    ? NO_SWORD
                    : sword.enemyCollisionBox(
                            this.playerLink.position().romEntityX(),
                            swordCollisionY,
                            this.playerLink.animation().direction(),
                            this.playerState.equipmentLevels().runningWithPegasusBoots());
            EnemyAttackContext attackContext = this.frameInputs.attack(
                    this.playerState.equipmentLevels().swordLevel(),
                    this.playerState.equipmentLevels().tunicType() == Tunic.RED.romValue(),
                    this.playerState.equipmentLevels().activePowerUp() == PowerUp.PIECE_OF_POWER.romValue(),
                    sword != null && sword.spinAttackActive(),
                    this.playerState.equipmentLevels().runningWithPegasusBoots(),
                    this.playerLink.equipment().isUsingShield(),
                    Direction.fromJava(this.playerLink.animation().direction()).romIndex(),
                    this.playerLink.animation().romEntityZ());
            if (frameInputs.keepsShieldCombatRectangle(attackContext)) swordBoxForEntityTick = NO_SWORD;
            var combatEvents = rooms.entities()
                    .resolveEntityCombat(this.frameInputs.combat(
                            swordBoxForEntityTick,
                            attackContext,
                            bladeClashCountdown > 0,
                            goombaBounceCountdown > 0,
                            sword == null ? 0 : sword.hitTarget(),
                            canTakeContactDamage()));
            EnemyCombatEventConsumer.consume(combatEvents, this.sounds.sink(), this.transientVfxSystem);
            for (int eventIndex = 0; eventIndex < combatEvents.size(); eventIndex++) {
                EntityCombatEvent event = combatEvents.get(eventIndex);

                applyEvent(event);
            }
        }

        return swordBoxForEntityTick;
    }

    private void applyEvent(EntityCombatEvent event) {
        if (event.linkDamage() > 0 && canTakeContactDamage()) {
            int previousPowerUp = this.playerState.equipmentLevels().activePowerUp();
            this.playerState.vitals().applyRomEnemyDamage(event.linkDamage());
            if (previousPowerUp != PowerUp.NONE.romValue()
                    && this.playerState.equipmentLevels().activePowerUp() == PowerUp.NONE.romValue()) {
                this.musicDirector.restoreMusicAfterPowerUpEnds(this.rooms);
            }
            this.rooms.entityState().resetGuardianAcornCounterOnHurt();
            if (event.linkCollisionResponse().active()) {
                EntityCombatEvent.LinkCollisionResponse response = event.linkCollisionResponse();
                if (!rooms.entityState()
                        .deferContactRecoil(
                                event.slot(),
                                this.playerLink.position().romEntityX(),
                                this.playerLink.position().romEntityY())) {
                    this.playerLink.position().applyRomSpeed(response.speedX(), response.speedY());
                }
                this.playerLink.status().setCollisionIgnoreFrames(response.ignoreCollisionCountdown());
                if (response.invincibilityCountdown() != 0) {
                    this.playerState.vitals().setInvincibilityCounter(response.invincibilityCountdown());
                }
                this.playerState.equipmentLevels().setRunningWithPegasusBoots(false);
                // ApplyLinkCollisionWithEnemy writes WAVE_SFX_LINK_HURT
                // before the shared Link response helper.
                this.sounds.sink().play(GameplaySoundEvent.LINK_HURT);
            }
        }
        if (event.linkDamage() == 0 && event.linkCollisionResponse().active()) {
            var response = event.linkCollisionResponse();
            this.playerLink.position().applyRomSpeed(response.speedX(), response.speedY());
            this.playerLink.status().setCollisionIgnoreFrames(response.ignoreCollisionCountdown());
            this.playerState.equipmentLevels().setRunningWithPegasusBoots(false);
        }
        if (event.linkAction() == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN
                || event.linkAction() == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING) {
            goombaBounceCountdown = 2;
            this.playerLink.airborne().bounceFromGoomba();
        } else if (event.linkAction() == EntityCombatEvent.LinkAction.SWORD_MOBLIN_BLADE_CLASH) {
            bladeClashCountdown = 0x0C;
            Sword sword = this.equipmentController.activeSword();
            if (sword != null) sword.onBladeClash();
        } else if (event.linkAction() == EntityCombatEvent.LinkAction.RECORD_SWORD_HIT_TARGET) {
            Sword sword = this.equipmentController.activeSword();
            if (sword != null) sword.setHitTarget(event.slot() + 1);
        }
    }

    private boolean canTakeContactDamage() {
        return this.playerState.vitals().invincibilityCounter() == 0
                && !this.playerLink.status().isCollisionDamageImmune();
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private EquipmentController equipmentController;
        private FrameInputCache frameInputs;
        private GameplaySounds gameplaySoundSink;
        private InventoryController inventoryController;
        private Link link;
        private MusicDirector musicDirector;
        private PlayerState playerState;
        private RoomSession roomSession;
        private ScrollController scrollController;
        private TransientVfxSystem transientVfxSystem;
        private TransitionController transitionController;

        public Inputs equipmentController(EquipmentController value) {
            this.equipmentController = value;
            return this;
        }

        public Inputs frameInputs(FrameInputCache value) {
            this.frameInputs = value;
            return this;
        }

        public Inputs gameplaySoundSink(GameplaySounds value) {
            this.gameplaySoundSink = value;
            return this;
        }

        public Inputs inventoryController(InventoryController value) {
            this.inventoryController = value;
            return this;
        }

        public Inputs link(Link value) {
            this.link = value;
            return this;
        }

        public Inputs musicDirector(MusicDirector value) {
            this.musicDirector = value;
            return this;
        }

        public Inputs playerState(PlayerState value) {
            this.playerState = value;
            return this;
        }

        public Inputs roomSession(RoomSession value) {
            this.roomSession = value;
            return this;
        }

        public Inputs scrollController(ScrollController value) {
            this.scrollController = value;
            return this;
        }

        public Inputs transientVfxSystem(TransientVfxSystem value) {
            this.transientVfxSystem = value;
            return this;
        }

        public Inputs transitionController(TransitionController value) {
            this.transitionController = value;
            return this;
        }
    }
}
