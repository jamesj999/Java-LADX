package linksawakening.world.entity.event;

public record HeartPiecePickupUpdate(
        int dialogId,
        int tilesCommand,
        boolean incrementPiece,
        boolean newContainer,
        boolean interactionLocked,
        boolean complete)
        implements linksawakening.world.entity.event.EntityRequestBridge {}
