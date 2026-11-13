package linksawakening.world.entity;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.EntitySpriteTileSnapshot;

/**
 * Read-only entity presentation. Public constructors capture an owned immutable snapshot;
 * EntityRoomInspection.view() instead returns a cached live view of ExecuteEntities output.
 * Live views are for immediate same-thread reads. Every withX method produces owned data.
 */
public final class RoomEntitySnapshot {

    private final Data data;

    /** Live read-only view; lists and metadata follow the owning room until it is discarded. */
    static RoomEntitySnapshot liveView(EntityRoomInspection liveSource) {
        return new RoomEntitySnapshot(liveSource);
    }

    private RoomEntitySnapshot(EntityRoomInspection liveSource) {
        java.util.Objects.requireNonNull(liveSource, "live source");
        data = new Live(
                liveSource, new EntityLiveSlotList(liveSource, false), new EntityLiveSlotList(liveSource, true));
    }

    /** Compares presentation without allocating iterator or array snapshots. */
    public boolean samePresentationAs(RoomEntitySnapshot other) {
        if (other == this) return true;
        if (other == null
                || spriteSelection() != other.spriteSelection()
                || spriteTiles() != other.spriteTiles()
                || sideScrolling() != other.sideScrolling()) return false;
        if (!sameValues(slots(), other.slots())
                || !sameValues(hookshotChainOam(), other.hookshotChainOam())
                || !sameValues(pincerBodyOam(), other.pincerBodyOam())
                || !sameValues(wingedOctorokOam(), other.wingedOctorokOam())
                || !sameValues(bowWowChainOam(), other.bowWowChainOam())) return false;
        for (int slot = 0; slot < RoomEntity.SLOT_COUNT; slot++)
            if (visualYOffset(slot) != other.visualYOffset(slot)) return false;
        return true;
    }

    private static boolean sameValues(List<?> first, List<?> second) {
        if (first == second) return true;
        if (first.size() != second.size()) return false;
        for (int i = 0; i < first.size(); i++) if (!java.util.Objects.equals(first.get(i), second.get(i))) return false;
        return true;
    }

    /** Returns an owned immutable value; already-owned snapshots retain their identity. */
    public RoomEntitySnapshot ownedCopy() {
        return data instanceof Live live ? EntityRoomInspection.entityRoomQueriesSnapshot(live.room()) : this;
    }

    public RoomEntitySnapshot(List<RoomEntity> slots) {
        this(slots, null, null);
    }

    public RoomEntitySnapshot(
            List<RoomEntity>
                    slots, /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */
            EntitySpriteSelection spriteSelection) {
        this(slots, spriteSelection, null);
    }

    public RoomEntitySnapshot(
            List<RoomEntity>
                    slots, /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */
            EntitySpriteSelection
                    spriteSelection, /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */
            EntitySpriteTileSnapshot spriteTiles) {
        this(slots, spriteSelection, spriteTiles, false);
    }

    public RoomEntitySnapshot(
            List<RoomEntity> slots,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    spriteSelection,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteTileSnapshot
                    spriteTiles,
            boolean sideScrolling) {
        this(slots, spriteSelection, spriteTiles, sideScrolling, List.of());
    }

    public RoomEntitySnapshot(
            List<RoomEntity> slots,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    spriteSelection,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteTileSnapshot
                    spriteTiles,
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
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    spriteSelection,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteTileSnapshot
                    spriteTiles,
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
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    spriteSelection,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteTileSnapshot
                    spriteTiles,
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
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    spriteSelection,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteTileSnapshot
                    spriteTiles,
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
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    spriteSelection,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteTileSnapshot
                    spriteTiles,
            boolean sideScrolling,
            List<HookshotChainOam.Entry> hookshotChainOam,
            List<PincerBodyOam.Entry> pincerBodyOam,
            List<WingedOctorokOam.Entry> wingedOctorokOam,
            int[] visualYOffsetBySlot,
            List<BowWowChain.Entry> bowWowChainOam) {
        List<BowWowChain.Entry> ownedBowWowChainOam = List.copyOf(bowWowChainOam);
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
        List<RoomEntity> ownedSlots = List.copyOf(slots);
        EntitySpriteSelection ownedSpriteSelection = spriteSelection;
        EntitySpriteTileSnapshot ownedSpriteTiles = spriteTiles;
        boolean ownedSideScrolling = sideScrolling;
        List<HookshotChainOam.Entry> ownedHookshotChainOam = List.copyOf(hookshotChainOam);
        List<PincerBodyOam.Entry> ownedPincerBodyOam = List.copyOf(pincerBodyOam);
        List<WingedOctorokOam.Entry> ownedWingedOctorokOam = List.copyOf(wingedOctorokOam);
        int[] ownedVisualYOffsetBySlot = visualYOffsetBySlot.clone();
        List<RoomEntity> loaded = new ArrayList<>();
        for (RoomEntity entity : ownedSlots) {
            if (entity.loaded()) {
                loaded.add(entity);
            }
        }
        List<RoomEntity> ownedLoadedEntities = List.copyOf(loaded);
        data = new Owned(
                ownedSlots,
                ownedLoadedEntities,
                ownedSpriteSelection,
                ownedSpriteTiles,
                ownedSideScrolling,
                ownedHookshotChainOam,
                ownedPincerBodyOam,
                ownedWingedOctorokOam,
                ownedVisualYOffsetBySlot,
                ownedBowWowChainOam);
    }

    public RoomEntitySnapshot withSpriteSelection(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteSelection
                    selection) {
        if (data instanceof Live) return ownedCopy().withSpriteSelection(selection);
        return new RoomEntitySnapshot(
                data.slots(),
                selection,
                data.spriteTiles(),
                data.sideScrolling(),
                data.hookshotChainOam(),
                data.pincerBodyOam(),
                data.wingedOctorokOam(),
                data.visualYOffsetBySlot(),
                data.bowWowChainOam());
    }

    public RoomEntitySnapshot withSpriteTiles(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteTileSnapshot
                    tiles) {
        if (data instanceof Live) return ownedCopy().withSpriteTiles(tiles);
        return new RoomEntitySnapshot(
                data.slots(),
                data.spriteSelection(),
                tiles,
                data.sideScrolling(),
                data.hookshotChainOam(),
                data.pincerBodyOam(),
                data.wingedOctorokOam(),
                data.visualYOffsetBySlot(),
                data.bowWowChainOam());
    }

    /** Returns whether the room uses the side-scroll OAM path. */
    public boolean sideScrolling() {
        return data.sideScrolling();
    }

    public RoomEntitySnapshot withSideScrolling(boolean sideScrolling) {
        if (data instanceof Live) return ownedCopy().withSideScrolling(sideScrolling);
        return new RoomEntitySnapshot(
                data.slots(),
                data.spriteSelection(),
                data.spriteTiles(),
                sideScrolling,
                data.hookshotChainOam(),
                data.pincerBodyOam(),
                data.wingedOctorokOam(),
                data.visualYOffsetBySlot(),
                data.bowWowChainOam());
    }

    public List<RoomEntity> slots() {
        return data.slots();
    }

    public List<RoomEntity> loadedEntities() {
        return data.loadedEntities();
    }

    public EntitySpriteSelection spriteSelection() {
        return data.spriteSelection();
    }

    public EntitySpriteTileSnapshot spriteTiles() {
        return data.spriteTiles();
    }

    public List<HookshotChainOam.Entry> hookshotChainOam() {
        return data.hookshotChainOam();
    }

    public List<PincerBodyOam.Entry> pincerBodyOam() {
        return data.pincerBodyOam();
    }

    public RoomEntitySnapshot withBowWowChainOam(List<BowWowChain.Entry> entries) {
        if (data instanceof Live) return ownedCopy().withBowWowChainOam(entries);
        return new RoomEntitySnapshot(
                data.slots(),
                data.spriteSelection(),
                data.spriteTiles(),
                data.sideScrolling(),
                data.hookshotChainOam(),
                data.pincerBodyOam(),
                data.wingedOctorokOam(),
                data.visualYOffsetBySlot(),
                entries);
    }

    public List<BowWowChain.Entry> bowWowChainOam() {
        return data.bowWowChainOam();
    }

    public List<WingedOctorokOam.Entry> wingedOctorokOam() {
        return data.wingedOctorokOam();
    }

    /** Returns the ROM display-only Y correction for an entity slot. */
    public int visualYOffset(int slot) {
        return data.visualYOffset(slot);
    }

    public RoomEntitySnapshot withVisualYOffset(int slot, int offset) {
        if (data instanceof Live) return ownedCopy().withVisualYOffset(slot, offset);
        if (slot < 0 || slot >= data.visualYOffsetBySlot().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (offset < -0x80 || offset > 0x7F) {
            throw new IllegalArgumentException("Entity visual Y offset must be a signed display offset");
        }
        int[] offsets = data.visualYOffsetBySlot().clone();
        offsets[slot] = offset;
        return new RoomEntitySnapshot(
                data.slots(),
                data.spriteSelection(),
                data.spriteTiles(),
                data.sideScrolling(),
                data.hookshotChainOam(),
                data.pincerBodyOam(),
                data.wingedOctorokOam(),
                offsets,
                data.bowWowChainOam());
    }

    public RoomEntitySnapshot withHookshotChainOam(List<HookshotChainOam.Entry> hookshotChainOam) {
        if (data instanceof Live) return ownedCopy().withHookshotChainOam(hookshotChainOam);
        return new RoomEntitySnapshot(
                data.slots(),
                data.spriteSelection(),
                data.spriteTiles(),
                data.sideScrolling(),
                hookshotChainOam,
                data.pincerBodyOam(),
                data.wingedOctorokOam(),
                data.visualYOffsetBySlot(),
                data.bowWowChainOam());
    }

    public RoomEntitySnapshot withPincerBodyOam(List<PincerBodyOam.Entry> pincerBodyOam) {
        if (data instanceof Live) return ownedCopy().withPincerBodyOam(pincerBodyOam);
        return new RoomEntitySnapshot(
                data.slots(),
                data.spriteSelection(),
                data.spriteTiles(),
                data.sideScrolling(),
                data.hookshotChainOam(),
                pincerBodyOam,
                data.wingedOctorokOam(),
                data.visualYOffsetBySlot(),
                data.bowWowChainOam());
    }

    public RoomEntitySnapshot withWingedOctorokOam(List<WingedOctorokOam.Entry> wingedOctorokOam) {
        if (data instanceof Live) return ownedCopy().withWingedOctorokOam(wingedOctorokOam);
        return new RoomEntitySnapshot(
                data.slots(),
                data.spriteSelection(),
                data.spriteTiles(),
                data.sideScrolling(),
                data.hookshotChainOam(),
                data.pincerBodyOam(),
                wingedOctorokOam,
                data.visualYOffsetBySlot(),
                data.bowWowChainOam());
    }

    /** Two actual representations: immutable captured values and a live room view. */
    private sealed interface Data permits Owned, Live {
        List<RoomEntity> slots();

        List<RoomEntity> loadedEntities();

        EntitySpriteSelection spriteSelection();

        EntitySpriteTileSnapshot spriteTiles();

        boolean sideScrolling();

        List<HookshotChainOam.Entry> hookshotChainOam();

        List<PincerBodyOam.Entry> pincerBodyOam();

        List<WingedOctorokOam.Entry> wingedOctorokOam();

        int[] visualYOffsetBySlot();

        List<BowWowChain.Entry> bowWowChainOam();

        int visualYOffset(int slot);
    }

    private record Owned(
            List<RoomEntity> slots,
            List<RoomEntity> loadedEntities,
            EntitySpriteSelection spriteSelection,
            EntitySpriteTileSnapshot spriteTiles,
            boolean sideScrolling,
            List<HookshotChainOam.Entry> hookshotChainOam,
            List<PincerBodyOam.Entry> pincerBodyOam,
            List<WingedOctorokOam.Entry> wingedOctorokOam,
            int[] visualYOffsetBySlot,
            List<BowWowChain.Entry> bowWowChainOam)
            implements Data {
        public int visualYOffset(int slot) {
            if (slot < 0 || slot >= visualYOffsetBySlot.length)
                throw new IllegalArgumentException("Entity slot out of range: " + slot);
            return visualYOffsetBySlot[slot];
        }
    }

    private record Live(EntityRoomInspection room, List<RoomEntity> slots, List<RoomEntity> loadedEntities)
            implements Data {
        public EntitySpriteSelection spriteSelection() {
            return room.presentationState.snapshotSelection();
        }

        public EntitySpriteTileSnapshot spriteTiles() {
            return room.spriteTiles;
        }

        public boolean sideScrolling() {
            return room.physicsTables.readGroundInteractionSideScrolling();
        }

        public List<HookshotChainOam.Entry> hookshotChainOam() {
            return room.slotProperties.readHookshotChainOam();
        }

        public List<PincerBodyOam.Entry> pincerBodyOam() {
            return room.slotProperties.readPincerBodyOam();
        }

        public List<WingedOctorokOam.Entry> wingedOctorokOam() {
            return room.slotProperties.readWingedOctorokOam();
        }

        public List<BowWowChain.Entry> bowWowChainOam() {
            return room.slotProperties.readBowWowChainOam();
        }

        public int visualYOffset(int slot) {
            java.util.Objects.checkIndex(slot, RoomEntity.SLOT_COUNT);
            return room.physicsTables.readFallingVisualYOffset(slot);
        }

        public int[] visualYOffsetBySlot() {
            int[] offsets = new int[RoomEntity.SLOT_COUNT];
            for (int slot = 0; slot < offsets.length; slot++) offsets[slot] = visualYOffset(slot);
            return offsets;
        }
    }
}
