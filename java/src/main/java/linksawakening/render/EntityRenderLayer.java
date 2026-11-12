package linksawakening.render;

import linksawakening.dialog.DialogController;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.render.EntityRenderInputs.Offset;
import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.ScrollController;

/** Renders the ROM-backed OAM display lists for the room's loaded entities. */
final class EntityRenderLayer implements HardwareRenderLayer {
    private static final int ENTITY_BOMB = 0x02;
    private static final int ENTITY_WINGED_OCTOROK = 0xAE;
    private static final int ENTITY_PINCER = 0xB0;
    private static final int BOMB_NORMAL_DEFINITION_BANK = RomSymbols.BombSprite.bank();
    private static final int BOMB_NORMAL_DEFINITION_ADDRESS = RomSymbols.BombSprite.address();
    // EntityRoomLoader preserves hActiveEntityPosX/Y, which are the values
    // written to hardware OAM. The framebuffer stores sprite top-left pixels.
    private static final int OAM_X_SCREEN_ORIGIN = 0x08;
    private static final int OAM_Y_SCREEN_ORIGIN = 0x10;

    private RoomEntitySnapshot snapshot;
    private int[][] objectPalettes;
    private ScrollController scrollController;
    private int frameCounter;
    private final EntitySpritePainter painter;
    private EntitySelectionSprites currentSprites, previousSprites;
    private EntitySelectionSprites activeSprites;
    private final Offset currentOffset = new Offset(0, 0), previousOffset = new Offset(0, 0);

    public EntityRenderLayer(RoomEntitySnapshot snapshot, int[][] objectPalettes, ScrollController scrollController) {
        this(snapshot, objectPalettes, scrollController, 0);
    }

    public EntityRenderLayer(
            RoomEntitySnapshot snapshot, int[][] objectPalettes, ScrollController scrollController, int frameCounter) {
        this(snapshot, objectPalettes, scrollController, frameCounter, null);
    }

    public EntityRenderLayer(
            RoomEntitySnapshot snapshot,
            int[][] objectPalettes,
            ScrollController scrollController,
            int frameCounter,
            DialogController.BoxPosition dialogPosition) {
        if (snapshot == null || objectPalettes == null || scrollController == null) {
            throw new IllegalArgumentException("Entity render inputs cannot be null");
        }
        if (objectPalettes.length == 0) {
            throw new IllegalArgumentException("At least one object palette is required");
        }
        for (int[] palette : objectPalettes) {
            if (palette == null || palette.length < 4) {
                throw new IllegalArgumentException("Entity palettes must contain four colors");
            }
        }
        this.snapshot = snapshot;
        this.objectPalettes = EntityRenderInputs.clonePalettes(objectPalettes);
        this.scrollController = scrollController;
        this.frameCounter = frameCounter & 0xFF;
        painter = new EntitySpritePainter(dialogPosition);
    }

    void configure(
            RoomEntitySnapshot snapshot,
            int[][] palettes,
            ScrollController scroll,
            int counter,
            DialogController.BoxPosition dialog) {
        this.snapshot = snapshot;
        this.objectPalettes = palettes;
        this.scrollController = scroll;
        frameCounter = counter & 255;
        painter.dialogPosition = dialog;
    }

    private void submitEntities(RenderContext context) {
        if (context == null || context.buffer() == null || context.gpu() == null) {
            throw new IllegalArgumentException("Entity render context cannot be null");
        }
        if (currentSprites == null) currentSprites = new EntitySelectionSprites();
        if (scrollController.isActive()) {
            var previousRoom = scrollController.previousRoom();
            if (previousRoom != null
                    && previousRoom.entities() != null
                    && previousRoom.entities().spriteSelection() != null) {
                if (previousSprites == null) previousSprites = new EntitySelectionSprites();
                renderSnapshot(
                        context,
                        previousRoom.entities(),
                        previousRoom.entities().spriteSelection().objectPalettes(),
                        EntityRenderInputs.previousRoomOffset(scrollController, previousOffset),
                        previousSprites);
            }
        }
        renderSnapshot(
                context,
                snapshot,
                objectPalettes,
                EntityRenderInputs.currentRoomOffset(scrollController, currentOffset),
                currentSprites);
    }

    @Override
    public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        submitEntities(RenderContext.indexed(gpu, frame));
    }

    private void renderSnapshot(
            RenderContext context,
            RoomEntitySnapshot entities,
            int[][] palettes,
            Offset offset,
            EntitySelectionSprites styles) {
        styles.bind(entities.spriteSelection());
        activeSprites = styles;
        var loaded = entities.loadedEntities();
        for (int i = 0; i < loaded.size(); i++)
            renderSnapshotEntity(context, entities, palettes, offset, loaded.get(i));
        painter.renderHookshotChainOam(context, entities, palettes, offset.x(), offset.y());
    }

    private void renderSnapshotEntity(
            RenderContext context, RoomEntitySnapshot entities, int[][] palettes, Offset offset, RoomEntity entity) {
        EntitySpriteTileSnapshot tiles = entities.spriteTiles();
        EntitySpriteDefinition death = deathDefinition(entities, entity);
        boolean renderingDeath = death != null;
        EntitySpriteDefinition definition = renderingDeath ? death : liveDefinition(entities, entity);
        int transitionVisualYOffset = entities.visualYOffset(entity.slot());
        if (entity.type() == ENTITY_BOMB
                && definition.bank() == BOMB_NORMAL_DEFINITION_BANK
                && definition.address() == BOMB_NORMAL_DEFINITION_ADDRESS) {
            // RenderBomb adds two before entering the single-sprite helper.
            transitionVisualYOffset += 2;
        }
        if (skipDuringRoomTransition(entity, offset, transitionVisualYOffset)) {
            return;
        }
        int variant = renderingDeath ? entity.deathSpriteVariant() : entity.spriteVariant();
        if (!renderingDeath && entity.type() == ENTITY_WINGED_OCTOROK) {
            // The pair is allocated before func_007_5805's rectangle in
            // OAM. Draw the later rectangle first so the earlier pair
            // keeps Game Boy OAM priority when the two overlap.
            painter.renderWingedOctorokOam(
                    context, entities.wingedOctorokOam(), entity.slot(), palettes, offset.x(), offset.y(), entity);
        }
        if (!renderingDeath && entity.type() == 0x6D) {
            // Chain OAM is allocated after the head; draw it first so
            // the earlier head pair wins overlapping pixels.
            var chain = entities.bowWowChainOam();
            for (int i = chain.size() - 1; i >= 0; i--) {
                var entry = chain.get(i);
                if (entry.sourceSlot() != entity.slot()) continue;
                painter.renderPackedOamSprite(
                        context,
                        palettes,
                        tiles,
                        entry.tileIndex(),
                        0,
                        entry.rawX() + offset.x() - OAM_X_SCREEN_ORIGIN,
                        entry.rawY() + offset.y() - OAM_Y_SCREEN_ORIGIN,
                        false);
            }
        }
        painter.renderingDeath = renderingDeath;
        try {
            painter.renderEntity(
                    context,
                    entity,
                    definition,
                    variant,
                    palettes,
                    tiles,
                    offset.x(),
                    offset.y(),
                    entities.sideScrolling(),
                    entities.visualYOffset(entity.slot()));
        } finally {
            painter.renderingDeath = false;
        }
        if (entity.type() == ENTITY_PINCER) {
            painter.renderPincerBodyOam(
                    context, entities.pincerBodyOam(), entity.slot(), palettes, tiles, offset.x(), offset.y());
        }
        renderEffects(context, entities, palettes, offset, entity, definition, renderingDeath);
    }

    private void renderEffects(
            RenderContext context,
            RoomEntitySnapshot entities,
            int[][] palettes,
            Offset offset,
            RoomEntity entity,
            EntitySpriteDefinition definition,
            boolean renderingDeath) {
        EntitySpriteTileSnapshot tiles = entities.spriteTiles();
        if (!renderingDeath && definition.supported() && entities.spriteSelection() != null) {
            EntitySpriteDefinition overlay = activeSprites.overlay(entity.type());
            if (overlay != null) {
                // FloatingItemEntityHandler renders the two-entry
                // rectangle after its main display list. Its tile and
                // attribute bytes remain in the ROM-backed definition.
                painter.renderEntity(
                        context,
                        entity,
                        overlay,
                        (frameCounter & 0x08) != 0 ? 1 : 0,
                        palettes,
                        tiles,
                        offset.x(),
                        offset.y(),
                        entities.sideScrolling(),
                        entities.visualYOffset(entity.slot()));
            }
        }
        if (entity.status() == EntityStatus.BURNING && entities.spriteSelection() != null) {
            EntitySpriteDefinition burning = entities.spriteSelection().burningSpriteDefinition();
            if (burning != null) {
                painter.renderEntity(
                        context,
                        entity,
                        burning,
                        (frameCounter >>> 3) & 0x01,
                        palettes,
                        tiles,
                        offset.x(),
                        offset.y(),
                        entities.sideScrolling(),
                        entities.visualYOffset(entity.slot()));
            }
        }
    }

    private static EntitySpriteDefinition deathDefinition(RoomEntitySnapshot entities, RoomEntity entity) {
        if (entity.status() != EntityStatus.DYING || entities.spriteSelection() == null) return null;
        EntitySpriteDefinition death = entity.powerRecoilDeath()
                ? entities.spriteSelection().powerRecoilDeathSpriteDefinition()
                : entities.spriteSelection().deathSpriteDefinition();
        return death != null && entity.deathSpriteVariant() >= 0 && entity.deathSpriteVariant() < death.variantCount()
                ? death
                : null;
    }

    private EntitySpriteDefinition liveDefinition(RoomEntitySnapshot entities, RoomEntity entity) {
        EntitySpriteDefinition definition = entity.spriteDefinition();
        if (entities.spriteSelection() != null) {
            EntitySpriteDefinition override = activeSprites.override(entity.type());
            if (override != null) definition = override;
        }
        return definition;
    }

    private boolean skipDuringRoomTransition(RoomEntity entity, Offset offset, int visualYOffset) {
        if (!scrollController.isActive()) return false;
        // SkipDisabledEntityDuringRoomTransition ($00:3D57-$3D7E), called
        // by the pair, single and rectangle helpers before generating OAM.
        // Unbounded Java positions retain the ROM's high/sign bytes updated
        // by UpdateEntityPositionForRoomTransition ($19:798E-$79B5).
        // The guard runs before the OAM helper subtracts horizontal shake.
        int rawX = entity.x() + offset.x() + scrollController.screenShakeHorizontal();
        int rawY = entity.y() + offset.y();
        int visualY = rawY - entity.z() + visualYOffset;
        return ((rawX - 1) & 0xFF) >= 0xC0
                || ((visualY - 1) & 0xFF) >= 0x88
                || (rawX & ~0xFF) != 0
                || (rawY & ~0xFF) != 0;
    }
}
