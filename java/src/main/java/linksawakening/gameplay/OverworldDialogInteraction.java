package linksawakening.gameplay;

import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.world.Warp;

import java.util.Objects;

import static linksawakening.world.RoomConstants.OBJECTS_PER_COLUMN;
import static linksawakening.world.RoomConstants.OBJECTS_PER_ROW;
import static linksawakening.world.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.RoomConstants.ROOM_OBJECT_ROW_STRIDE;

public final class OverworldDialogInteraction {

    private static final int OBJECT_OWL_STATUE = 0x6F;
    private static final int OBJECT_SIGNPOST = 0xD4;

    private final SignpostDialogTable signpostDialogTable;
    private final DialogTextLoader dialogTextLoader;

    public OverworldDialogInteraction(SignpostDialogTable signpostDialogTable, DialogTextLoader dialogTextLoader) {
        this.signpostDialogTable = Objects.requireNonNull(signpostDialogTable);
        this.dialogTextLoader = Objects.requireNonNull(dialogTextLoader);
    }

    public boolean tryOpenSignpostDialog(int roomId,
                                         int mapCategory,
                                         int[] roomObjectsArea,
                                         int linkPixelX,
                                         int linkPixelY,
                                         int linkDirection,
                                         boolean actionPressed,
                                         OverworldDialogBlockers blockers,
                                         DialogController dialogController) {
        if (!actionPressed
            || (blockers != null && blockers.blocksOpening())
            || mapCategory != Warp.CATEGORY_OVERWORLD
            || linkDirection != Link.DIRECTION_UP
            || roomObjectsArea == null
            || dialogController == null
            || dialogController.isActive()) {
            return false;
        }

        int areaIndex = objectAreaIndexInFrontOfLink(linkPixelX, linkPixelY);
        if (areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
            return false;
        }

        int objectId = roomObjectsArea[areaIndex] & 0xFF;
        if (objectId != OBJECT_SIGNPOST && objectId != OBJECT_OWL_STATUE) {
            return false;
        }

        SignpostDialogRef dialogRef = signpostDialogTable.resolve(roomId);
        dialogController.openPreformattedForLinkY(
            dialogTextLoader.load(dialogRef),
            LinkDialogPosition.dialogYFromTopLeft(linkPixelY));
        return true;
    }

    private static int objectAreaIndexInFrontOfLink(int linkPixelX, int linkPixelY) {
        int col = Math.floorDiv(linkPixelX + Link.SPRITE_SIZE / 2, Link.SPRITE_SIZE);
        int row = Math.floorDiv(linkPixelY - 1, Link.SPRITE_SIZE);
        if (col < 0 || col >= OBJECTS_PER_ROW || row < 0 || row >= OBJECTS_PER_COLUMN) {
            return -1;
        }
        return ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + col;
    }
}
