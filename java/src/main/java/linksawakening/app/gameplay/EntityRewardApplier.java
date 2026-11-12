package linksawakening.app.gameplay;

import java.util.List;
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

/** Synchronous reward batches; immutable snapshots stay available to retaining clients. */
final class EntityRewardApplier implements RoomFrameDrain<EntityRequest> {
    private GameplaySimulation current;

    void applyChestRewards(GameplaySimulation c) {
        current = c;
        var results = c.roomSession().entityFrameResults();
        int fade = results.presentation().consumePendingMusicFadeOutCountdown();
        if (fade != 0) c.gameplayMusicController().requestFadeOut(fade);
        results.rewards().drainChestRewardEvents(this);
        results.rewards().drainKeyRewardEvents(this);
        results.rewards().drainSlimeKeyRewardEvents(this);
        var heartPieceUpdates = results.rewards().consumeHeartPieceUpdates();
        for (int updateIndex = 0; updateIndex < heartPieceUpdates.size(); updateIndex++) {
            HeartPiecePickupUpdate update = heartPieceUpdates.get(updateIndex);

            c.dialogController().setInteractionLocked(update.interactionLocked());
            if (update.incrementPiece())
                c.playerState().setHeartPieces(c.playerState().heartPieces() + 1);
            if (update.newContainer()) c.playerState().completeHeartPieceContainer();
            if (update.tilesCommand() != 0) c.gpu().updateHeartPieceMeterTiles(c.romData(), update.tilesCommand());
            if (update.complete())
                c.gpu().updateTradingItemTiles(c.romData(), c.playerState().tradeSequenceItem());
        }

        results.rewards().drainHeartContainerRewards(this);
    }

    void applyProgressRewards(GameplaySimulation c) {
        current = c;
        var rewards = c.roomSession().entityFrameResults().rewards();
        rewards.drainMadamMeowMeowFullHealRequests(this);
        rewards.drainRichardProgressEvents(this);
        rewards.drainSwordPickupRewards(this);
    }

    void applyWitchRewards(GameplaySimulation c) {
        current = c;
        var rewards = c.roomSession().entityFrameResults().rewards();
        rewards.drainToadstoolRewards(this);
        rewards.drainWitchExchangeEvents(this);
        rewards.drainWitchRewardEvents(this);
    }

    public void accept(List<? extends EntityRequest> events) {
        for (int i = 0; i < events.size(); i++) apply(events.get(i));
    }

    private void apply(EntityRequest request) {
        var c = current;
        switch (request) {
            case ChestRewardEvent r -> c.playerState().applyChestReward(r.itemType());
            case KeyRewardEvent r -> c.playerState().applyChestReward(r.itemType());
            case SlimeKeyRewardEvent r -> c.playerState().setGoldenLeavesCount(r.goldenLeavesCount());
            case HeartContainerRewardEvent r -> {
                c.playerState().applyHeartContainerReward();
                c.musicDirector().playDirectMusic(0x18);
            }
            case MadamMeowMeowFullHealRequest r -> c.playerState().requestFullHeal();
            case RichardProgressEvent r -> {
                if (r.kind() == RichardProgressEvent.Kind.RETURN_ALL_LEAVES) {
                    c.playerState().setGoldenLeavesCount(r.goldenLeavesCount());
                    c.gpu().replaceSlimeKeyTilesByGoldenLeaf(c.romData());
                }
            }
            case SwordPickupRewardEvent r -> c.playerState().playerRewards().applyBeachSwordReward();
            case ToadstoolRewardEvent r -> c.playerState().applyToadstoolReward();
            case WitchExchangeEvent r -> {
                c.playerState().beginWitchToadstoolExchange(r.inventorySlot());
                if (r.inventorySlot() == 0) c.link().clearDialogGotItem();
            }
            case WitchRewardEvent r -> c.playerState().applyWitchMagicPowderReward();
            default -> throw new IllegalStateException("Unexpected room reward: " + request);
        }
    }
}
