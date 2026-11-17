package linksawakening.app.gameplay.entity;

import java.util.List;
import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.EntityRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.world.room.RoomFrameDrain;
import linksawakening.world.room.RoomSession;

/**
 * Synchronous reward batches; immutable snapshots stay available to retaining clients. One instance is
 * retained for the owning session.
 */
public final class EntityRewardApplier implements RoomFrameDrain<EntityRequest> {
    private final DialogController dialogController;
    private final GameplayMusicController gameplayMusicController;
    private final GPU gpu;
    private final Link playerLink;
    private final MusicDirector musicDirector;
    private final PlayerState playerState;
    private final Rom rom;
    private final RoomSession rooms;

    public EntityRewardApplier(
            DialogController dialogController,
            GameplayMusicController gameplayMusicController,
            GPU gpu,
            Link link,
            MusicDirector musicDirector,
            PlayerState playerState,
            Rom romData,
            RoomSession roomSession) {
        this.dialogController = Objects.requireNonNull(dialogController);
        this.gameplayMusicController = Objects.requireNonNull(gameplayMusicController);
        this.gpu = Objects.requireNonNull(gpu);
        this.playerLink = Objects.requireNonNull(link);
        this.musicDirector = Objects.requireNonNull(musicDirector);
        this.playerState = Objects.requireNonNull(playerState);
        this.rom = Objects.requireNonNull(romData);
        this.rooms = Objects.requireNonNull(roomSession);
    }

    public void applyChestRewards() {
        var results = this.rooms.entityFrameResults();
        int fade = results.presentation().consumePendingMusicFadeOutCountdown();
        if (fade != 0) this.gameplayMusicController.requestFadeOut(fade);
        results.rewards().drainChestRewardEvents(this);
        results.rewards().drainKeyRewardEvents(this);
        results.rewards().drainSlimeKeyRewardEvents(this);
        var heartPieceUpdates = results.rewards().consumeHeartPieceUpdates();
        for (int updateIndex = 0; updateIndex < heartPieceUpdates.size(); updateIndex++) {
            HeartPiecePickupUpdate update = heartPieceUpdates.get(updateIndex);

            this.dialogController.setInteractionLocked(update.interactionLocked());
            if (update.incrementPiece())
                EntityRewardApplier.this
                        .playerState
                        .progression()
                        .setHeartPieces(EntityRewardApplier.this
                                        .playerState
                                        .progression()
                                        .heartPieces()
                                + 1);
            if (update.newContainer()) this.playerState.vitals().completeHeartPieceContainer();
            if (update.tilesCommand() != 0) this.gpu.updateHeartPieceMeterTiles(this.rom, update.tilesCommand());
            if (update.complete())
                this.gpu.updateTradingItemTiles(
                        this.rom, this.playerState.progression().tradeSequenceItem());
        }

        results.rewards().drainHeartContainerRewards(this);
    }

    public void applyProgressRewards() {
        var rewards = this.rooms.entityFrameResults().rewards();
        rewards.drainMadamMeowMeowFullHealRequests(this);
        rewards.drainRichardProgressEvents(this);
        rewards.drainSwordPickupRewards(this);
    }

    public void applyWitchRewards() {
        var rewards = this.rooms.entityFrameResults().rewards();
        rewards.drainToadstoolRewards(this);
        rewards.drainWitchExchangeEvents(this);
        rewards.drainWitchRewardEvents(this);
    }

    @Override
    public void accept(List<? extends EntityRequest> events) {
        for (int i = 0; i < events.size(); i++) apply(events.get(i));
    }

    private void apply(EntityRequest request) {
        switch (request) {
            case ChestRewardEvent r -> EntityRewardApplier.this
                    .playerState
                    .chestRewards()
                    .applyChestReward(r.itemType());
            case KeyRewardEvent r -> EntityRewardApplier.this
                    .playerState
                    .chestRewards()
                    .applyChestReward(r.itemType());
            case SlimeKeyRewardEvent r -> EntityRewardApplier.this
                    .playerState
                    .progression()
                    .setGoldenLeavesCount(r.goldenLeavesCount());
            case HeartContainerRewardEvent r -> {
                this.playerState.vitals().applyHeartContainerReward();
                this.musicDirector.playDirectMusic(0x18);
            }
            case MadamMeowMeowFullHealRequest r -> EntityRewardApplier.this
                    .playerState
                    .vitals()
                    .requestFullHeal();
            case RichardProgressEvent r -> {
                if (r.kind() == RichardProgressEvent.Kind.RETURN_ALL_LEAVES) {
                    this.playerState.progression().setGoldenLeavesCount(r.goldenLeavesCount());
                    this.gpu.replaceSlimeKeyTilesByGoldenLeaf(this.rom);
                }
            }
            case SwordPickupRewardEvent r -> EntityRewardApplier.this
                    .playerState
                    .playerRewards()
                    .applyBeachSwordReward();
            case ToadstoolRewardEvent r -> EntityRewardApplier.this
                    .playerState
                    .playerRewards()
                    .applyToadstoolReward();
            case WitchExchangeEvent r -> {
                this.playerState.playerRewards().beginWitchToadstoolExchange(r.inventorySlot());
                if (r.inventorySlot() == 0) this.playerLink.animation().clearDialogGotItem();
            }
            case WitchRewardEvent r -> EntityRewardApplier.this
                    .playerState
                    .playerRewards()
                    .applyWitchMagicPowderReward();
            default -> throw new IllegalStateException("Unexpected room reward: " + request);
        }
    }
}
