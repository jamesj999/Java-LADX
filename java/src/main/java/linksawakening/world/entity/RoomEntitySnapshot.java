package linksawakening.world.entity;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.EntitySpriteTileSnapshot;

/** Immutable room entity slots captured at room-load time. */
public final class RoomEntitySnapshot {

    private final EntityRoomInspectionServices live;
    private final List<RoomEntity> slots;
    private final List<RoomEntity> loadedEntities;
    private final EntitySpriteSelection spriteSelection;
    private final EntitySpriteTileSnapshot spriteTiles;
    private final boolean sideScrolling;
    private final List<HookshotChainOam.Entry> hookshotChainOam;
    private final List<PincerBodyOam.Entry> pincerBodyOam;
    private final List<WingedOctorokOam.Entry> wingedOctorokOam;
    private final int[] visualYOffsetBySlot;
    private final List<BowWowChain.Entry> bowWowChainOam;

    /** Live read-only view; lists and metadata follow the owning room until it is discarded. */
    static RoomEntitySnapshot liveView(EntityRoomInspectionServices live) {
        return new RoomEntitySnapshot(live);
    }

    private RoomEntitySnapshot(EntityRoomInspectionServices live) {
        this.live = live;
        this.slots = new EntityLiveSlotList(live, false);
        this.loadedEntities = new EntityLiveSlotList(live, true);
        this.spriteSelection = null;
        this.spriteTiles = null;
        this.sideScrolling = false;
        this.hookshotChainOam = List.of();
        this.pincerBodyOam = List.of();
        this.wingedOctorokOam = List.of();
        this.bowWowChainOam = List.of();
        this.visualYOffsetBySlot = null;
    }

    public RoomEntitySnapshot(List<RoomEntity> slots) {
        this(slots, null, null);
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection) {
        this(slots, spriteSelection, null);
    }

    public RoomEntitySnapshot(
            List<RoomEntity> slots, EntitySpriteSelection spriteSelection, EntitySpriteTileSnapshot spriteTiles) {
        this(slots, spriteSelection, spriteTiles, false);
    }

    public RoomEntitySnapshot(
            List<RoomEntity> slots,
            EntitySpriteSelection spriteSelection,
            EntitySpriteTileSnapshot spriteTiles,
            boolean sideScrolling) {
        this(slots, spriteSelection, spriteTiles, sideScrolling, List.of());
    }

    public RoomEntitySnapshot(
            List<RoomEntity> slots,
            EntitySpriteSelection spriteSelection,
            EntitySpriteTileSnapshot spriteTiles,
            boolean sideScrolling,
            List<HookshotChainOam.Entry> hookshotChainOam) {
        this(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                List.of(),
                List.of(),
                new int[EntityRoomLoader.MAX_ENTITIES]);
    }

    public RoomEntitySnapshot(
            List<RoomEntity> slots,
            EntitySpriteSelection spriteSelection,
            EntitySpriteTileSnapshot spriteTiles,
            boolean sideScrolling,
            List<HookshotChainOam.Entry> hookshotChainOam,
            int[] visualYOffsetBySlot) {
        this(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                List.of(),
                List.of(),
                visualYOffsetBySlot);
    }

    public RoomEntitySnapshot(
            List<RoomEntity> slots,
            EntitySpriteSelection spriteSelection,
            EntitySpriteTileSnapshot spriteTiles,
            boolean sideScrolling,
            List<HookshotChainOam.Entry> hookshotChainOam,
            List<PincerBodyOam.Entry> pincerBodyOam,
            int[] visualYOffsetBySlot) {
        this(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                List.of(),
                visualYOffsetBySlot);
    }

    public RoomEntitySnapshot(
            List<RoomEntity> slots,
            EntitySpriteSelection spriteSelection,
            EntitySpriteTileSnapshot spriteTiles,
            boolean sideScrolling,
            List<HookshotChainOam.Entry> hookshotChainOam,
            List<PincerBodyOam.Entry> pincerBodyOam,
            List<WingedOctorokOam.Entry> wingedOctorokOam,
            int[] visualYOffsetBySlot) {
        this(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                wingedOctorokOam,
                visualYOffsetBySlot,
                List.of());
    }

    public RoomEntitySnapshot(
            List<RoomEntity> slots,
            EntitySpriteSelection spriteSelection,
            EntitySpriteTileSnapshot spriteTiles,
            boolean sideScrolling,
            List<HookshotChainOam.Entry> hookshotChainOam,
            List<PincerBodyOam.Entry> pincerBodyOam,
            List<WingedOctorokOam.Entry> wingedOctorokOam,
            int[] visualYOffsetBySlot,
            List<BowWowChain.Entry> bowWowChainOam) {
        this.live = null;
        this.bowWowChainOam = List.copyOf(bowWowChainOam);
        if (slots == null || slots.size() != EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException(
                    "A room must expose exactly " + EntityRoomLoader.MAX_ENTITIES + " entity slots");
        }
        if (hookshotChainOam == null) {
            throw new IllegalArgumentException("Hookshot chain OAM cannot be null");
        }
        if (pincerBodyOam == null) {
            throw new IllegalArgumentException("Pincer body OAM cannot be null");
        }
        if (wingedOctorokOam == null) {
            throw new IllegalArgumentException("Winged Octorok OAM cannot be null");
        }
        if (visualYOffsetBySlot == null || visualYOffsetBySlot.length != EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Exactly sixteen entity visual Y offsets are required");
        }
        for (int offset : visualYOffsetBySlot) {
            if (offset < -0x80 || offset > 0x7F) {
                throw new IllegalArgumentException("Entity visual Y offsets must be signed display offsets");
            }
        }
        this.slots = List.copyOf(slots);
        this.spriteSelection = spriteSelection;
        this.spriteTiles = spriteTiles;
        this.sideScrolling = sideScrolling;
        this.hookshotChainOam = List.copyOf(hookshotChainOam);
        this.pincerBodyOam = List.copyOf(pincerBodyOam);
        this.wingedOctorokOam = List.copyOf(wingedOctorokOam);
        this.visualYOffsetBySlot = visualYOffsetBySlot.clone();
        List<RoomEntity> loaded = new ArrayList<>();
        for (RoomEntity entity : this.slots) {
            if (entity.loaded()) {
                loaded.add(entity);
            }
        }
        this.loadedEntities = List.copyOf(loaded);
    }

    public RoomEntitySnapshot withSpriteSelection(EntitySpriteSelection selection) {
        if (live != null) return EntityRoomQueries.snapshot(live).withSpriteSelection(selection);
        return new RoomEntitySnapshot(
                slots,
                selection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                wingedOctorokOam,
                visualYOffsetBySlot,
                bowWowChainOam);
    }

    public RoomEntitySnapshot withSpriteTiles(EntitySpriteTileSnapshot tiles) {
        if (live != null) return EntityRoomQueries.snapshot(live).withSpriteTiles(tiles);
        return new RoomEntitySnapshot(
                slots,
                spriteSelection,
                tiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                wingedOctorokOam,
                visualYOffsetBySlot,
                bowWowChainOam);
    }

    /** Returns whether the room uses the side-scroll OAM path. */
    public boolean sideScrolling() {
        return live == null ? sideScrolling : live.physicsTables().readGroundInteractionSideScrolling();
    }

    public RoomEntitySnapshot withSideScrolling(boolean sideScrolling) {
        if (live != null) return EntityRoomQueries.snapshot(live).withSideScrolling(sideScrolling);
        return new RoomEntitySnapshot(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                wingedOctorokOam,
                visualYOffsetBySlot,
                bowWowChainOam);
    }

    public List<RoomEntity> slots() {
        return slots;
    }

    public List<RoomEntity> loadedEntities() {
        return loadedEntities;
    }

    public EntitySpriteSelection spriteSelection() {
        return live == null ? spriteSelection : live.presentationState().readSpriteSelection();
    }

    public EntitySpriteTileSnapshot spriteTiles() {
        return live == null ? spriteTiles : live.spriteTiles();
    }

    public List<HookshotChainOam.Entry> hookshotChainOam() {
        return live == null ? hookshotChainOam : live.slotProperties().readHookshotChainOam();
    }

    public List<PincerBodyOam.Entry> pincerBodyOam() {
        return live == null ? pincerBodyOam : live.slotProperties().readPincerBodyOam();
    }

    public RoomEntitySnapshot withBowWowChainOam(List<BowWowChain.Entry> entries) {
        if (live != null) return EntityRoomQueries.snapshot(live).withBowWowChainOam(entries);
        return new RoomEntitySnapshot(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                wingedOctorokOam,
                visualYOffsetBySlot,
                entries);
    }

    public List<BowWowChain.Entry> bowWowChainOam() {
        return live == null ? bowWowChainOam : live.slotProperties().readBowWowChainOam();
    }

    public List<WingedOctorokOam.Entry> wingedOctorokOam() {
        return live == null ? wingedOctorokOam : live.slotProperties().readWingedOctorokOam();
    }

    /** Returns the ROM display-only Y correction for an entity slot. */
    public int visualYOffset(int slot) {
        if (live != null) {
            java.util.Objects.checkIndex(slot, RoomEntity.SLOT_COUNT);
            return live.physicsTables().readFallingVisualYOffset(slot);
        }
        if (slot < 0 || slot >= visualYOffsetBySlot.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return visualYOffsetBySlot[slot];
    }

    public RoomEntitySnapshot withVisualYOffset(int slot, int offset) {
        if (live != null) return EntityRoomQueries.snapshot(live).withVisualYOffset(slot, offset);
        if (slot < 0 || slot >= visualYOffsetBySlot.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (offset < -0x80 || offset > 0x7F) {
            throw new IllegalArgumentException("Entity visual Y offset must be a signed display offset");
        }
        int[] offsets = visualYOffsetBySlot.clone();
        offsets[slot] = offset;
        return new RoomEntitySnapshot(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                wingedOctorokOam,
                offsets,
                bowWowChainOam);
    }

    public RoomEntitySnapshot withHookshotChainOam(List<HookshotChainOam.Entry> hookshotChainOam) {
        if (live != null) return EntityRoomQueries.snapshot(live).withHookshotChainOam(hookshotChainOam);
        return new RoomEntitySnapshot(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                wingedOctorokOam,
                visualYOffsetBySlot,
                bowWowChainOam);
    }

    public RoomEntitySnapshot withPincerBodyOam(List<PincerBodyOam.Entry> pincerBodyOam) {
        if (live != null) return EntityRoomQueries.snapshot(live).withPincerBodyOam(pincerBodyOam);
        return new RoomEntitySnapshot(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                wingedOctorokOam,
                visualYOffsetBySlot,
                bowWowChainOam);
    }

    public RoomEntitySnapshot withWingedOctorokOam(List<WingedOctorokOam.Entry> wingedOctorokOam) {
        if (live != null) return EntityRoomQueries.snapshot(live).withWingedOctorokOam(wingedOctorokOam);
        return new RoomEntitySnapshot(
                slots,
                spriteSelection,
                spriteTiles,
                sideScrolling,
                hookshotChainOam,
                pincerBodyOam,
                wingedOctorokOam,
                visualYOffsetBySlot,
                bowWowChainOam);
    }
}
