package linksawakening.world;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.gpu.EntitySpriteTileSnapshot;

import java.util.ArrayList;
import java.util.List;

/** Immutable room entity slots captured at room-load time. */
public final class RoomEntitySnapshot {

    private final List<RoomEntity> slots;
    private final List<RoomEntity> loadedEntities;
    private final EntitySpriteSelection spriteSelection;
    private final EntitySpriteTileSnapshot spriteTiles;
    private final boolean sideScrolling;
    private final List<HookshotChainOam.Entry> hookshotChainOam;
    private final List<PincerBodyOam.Entry> pincerBodyOam;
    private final List<WingedOctorokOam.Entry> wingedOctorokOam;
    private final int[] visualYOffsetBySlot;

    public RoomEntitySnapshot(List<RoomEntity> slots) {
        this(slots, null, null);
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection) {
        this(slots, spriteSelection, null);
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection,
                              EntitySpriteTileSnapshot spriteTiles) {
        this(slots, spriteSelection, spriteTiles, false);
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection,
                              EntitySpriteTileSnapshot spriteTiles, boolean sideScrolling) {
        this(slots, spriteSelection, spriteTiles, sideScrolling, List.of());
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection,
                              EntitySpriteTileSnapshot spriteTiles, boolean sideScrolling,
                              List<HookshotChainOam.Entry> hookshotChainOam) {
        this(slots, spriteSelection, spriteTiles, sideScrolling, hookshotChainOam,
            List.of(), List.of(), new int[EntityRoomLoader.MAX_ENTITIES]);
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection,
                              EntitySpriteTileSnapshot spriteTiles, boolean sideScrolling,
                              List<HookshotChainOam.Entry> hookshotChainOam,
                              int[] visualYOffsetBySlot) {
        this(slots, spriteSelection, spriteTiles, sideScrolling, hookshotChainOam,
            List.of(), List.of(), visualYOffsetBySlot);
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection,
                              EntitySpriteTileSnapshot spriteTiles, boolean sideScrolling,
                              List<HookshotChainOam.Entry> hookshotChainOam,
                              List<PincerBodyOam.Entry> pincerBodyOam,
                              int[] visualYOffsetBySlot) {
        this(slots, spriteSelection, spriteTiles, sideScrolling, hookshotChainOam,
            pincerBodyOam, List.of(), visualYOffsetBySlot);
    }

    public RoomEntitySnapshot(List<RoomEntity> slots, EntitySpriteSelection spriteSelection,
                              EntitySpriteTileSnapshot spriteTiles, boolean sideScrolling,
                              List<HookshotChainOam.Entry> hookshotChainOam,
                              List<PincerBodyOam.Entry> pincerBodyOam,
                              List<WingedOctorokOam.Entry> wingedOctorokOam,
                              int[] visualYOffsetBySlot) {
        if (slots == null || slots.size() != EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("A room must expose exactly "
                + EntityRoomLoader.MAX_ENTITIES + " entity slots");
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
        if (visualYOffsetBySlot == null
            || visualYOffsetBySlot.length != EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Exactly sixteen entity visual Y offsets are required");
        }
        for (int offset : visualYOffsetBySlot) {
            if (offset < -0x80 || offset > 0x7F) {
                throw new IllegalArgumentException(
                    "Entity visual Y offsets must be signed display offsets");
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
        return new RoomEntitySnapshot(slots, selection, spriteTiles, sideScrolling,
            hookshotChainOam, pincerBodyOam, wingedOctorokOam, visualYOffsetBySlot);
    }

    public RoomEntitySnapshot withSpriteTiles(EntitySpriteTileSnapshot tiles) {
        return new RoomEntitySnapshot(slots, spriteSelection, tiles, sideScrolling,
            hookshotChainOam, pincerBodyOam, wingedOctorokOam, visualYOffsetBySlot);
    }

    /** Returns whether the room uses the side-scroll OAM path. */
    public boolean sideScrolling() {
        return sideScrolling;
    }

    public RoomEntitySnapshot withSideScrolling(boolean sideScrolling) {
        return new RoomEntitySnapshot(slots, spriteSelection, spriteTiles, sideScrolling,
            hookshotChainOam, pincerBodyOam, wingedOctorokOam, visualYOffsetBySlot);
    }

    public List<RoomEntity> slots() {
        return slots;
    }

    public List<RoomEntity> loadedEntities() {
        return loadedEntities;
    }

    public EntitySpriteSelection spriteSelection() {
        return spriteSelection;
    }

    public EntitySpriteTileSnapshot spriteTiles() {
        return spriteTiles;
    }

    public List<HookshotChainOam.Entry> hookshotChainOam() {
        return hookshotChainOam;
    }

    public List<PincerBodyOam.Entry> pincerBodyOam() {
        return pincerBodyOam;
    }

    public List<WingedOctorokOam.Entry> wingedOctorokOam() {
        return wingedOctorokOam;
    }

    /** Returns the ROM display-only Y correction for an entity slot. */
    public int visualYOffset(int slot) {
        if (slot < 0 || slot >= visualYOffsetBySlot.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return visualYOffsetBySlot[slot];
    }

    public RoomEntitySnapshot withVisualYOffset(int slot, int offset) {
        if (slot < 0 || slot >= visualYOffsetBySlot.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (offset < -0x80 || offset > 0x7F) {
            throw new IllegalArgumentException(
                "Entity visual Y offset must be a signed display offset");
        }
        int[] offsets = visualYOffsetBySlot.clone();
        offsets[slot] = offset;
        return new RoomEntitySnapshot(slots, spriteSelection, spriteTiles, sideScrolling,
            hookshotChainOam, pincerBodyOam, wingedOctorokOam, offsets);
    }

    public RoomEntitySnapshot withHookshotChainOam(
        List<HookshotChainOam.Entry> hookshotChainOam) {
        return new RoomEntitySnapshot(slots, spriteSelection, spriteTiles, sideScrolling,
            hookshotChainOam, pincerBodyOam, wingedOctorokOam, visualYOffsetBySlot);
    }

    public RoomEntitySnapshot withPincerBodyOam(List<PincerBodyOam.Entry> pincerBodyOam) {
        return new RoomEntitySnapshot(slots, spriteSelection, spriteTiles, sideScrolling,
            hookshotChainOam, pincerBodyOam, wingedOctorokOam, visualYOffsetBySlot);
    }

    public RoomEntitySnapshot withWingedOctorokOam(
        List<WingedOctorokOam.Entry> wingedOctorokOam) {
        return new RoomEntitySnapshot(slots, spriteSelection, spriteTiles, sideScrolling,
            hookshotChainOam, pincerBodyOam, wingedOctorokOam, visualYOffsetBySlot);
    }
}
