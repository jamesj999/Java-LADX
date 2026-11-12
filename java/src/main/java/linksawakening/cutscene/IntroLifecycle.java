package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntConsumer;
import linksawakening.scene.BackgroundScene;

/**
 * Domain-specific runtime for the shipped opening sequence.
 *
 * <p>This follows the intro handlers' state and timer boundaries without
 * pretending to execute the Game Boy CPU. ROM tables provide all entity
 * records and animation data; this class only models the state that affects
 * the frame renderer.</p>
 */

/** IntroLifecycle behavior on the shared IntroSequence state. */
abstract class IntroLifecycle extends IntroSequenceState {
    protected IntroLifecycle() {
        super();
    }

    protected IntroLifecycle(byte[] romData, Function<String, BackgroundScene> backgroundProvider) {
        super(romData, backgroundProvider);
    }

    protected IntroLifecycle(DefaultResources resources) {
        super(resources);
    }

    public void setNoiseSound(IntConsumer sink) {
        noiseSound = Objects.requireNonNull(sink);
    }

    protected void tickSeaWaveSound() {
        // intro.asm func_001_71C7: called by stage 6 and Marin, not the sea handler.
        if (linkState < 2 && ++seaWaveCounter == 0xA0) {
            noiseSound.accept(0x0F);
            seaWaveCounter = 0;
        }
    }

    public void tick() {
        if (stage == Stage.COMPLETE && skipped) {
            return;
        }

        frameCounter++;
        stageFrame++;
        if (loadDelay > 0) {
            loadDelay--;
            refreshSnapshot();
            return;
        }
        drawCommandQueued = false;
        beachScrollStoppedThisFrame = false;
        topLineScroll = (scrollX() + sectionScrollOffsets[0]) & 255;
        // Draw commands, palettes and tile uploads become visible at the next VBlank.
        // Object positions additionally precede the entity handler's movement update.
        visibleTilemap = tilemap.clone();
        visibleAttrmap = attrmap.clone();
        visibleScrollY = scrollY;
        if (stage == Stage.TITLE_REVEAL || stage == Stage.TITLE_WAIT) titleEffectFrame++;
        visibleBg = dynamicBgPalettes();
        visibleObj = dynamicObjPalettes();
        visibleWave = waveTiles.clone();
        sprites = pendingSprites == null || stage == Stage.TITLE_DX || stage == Stage.COMPLETE
                ? renderSprites()
                : pendingSprites;
        pendingSprites = renderSprites();
        clock++;
        random.beginFrame(clock & 0xFF, 0);
        switch (stage) {
            case SEA -> tickSea();
            case LINK_FACE -> tickLinkFace();
            case SEA_FADE -> tickSeaFade();
            case BEACH_LOAD -> {
                if (stageFrame == 4) {
                    transitionTo(Stage.BEACH_FADE);
                    loadDelay = 2;
                }
            }
            case BEACH_FADE -> tickBeachFade();
            case BEACH -> tickBeach();
            case TITLE_REVEAL -> tickTitleReveal();
            case TITLE_WAIT -> {
                if (stageFrame >= 26) {
                    transitionTo(Stage.TITLE_DX);
                    clock = 1;
                }
            }
            case TITLE_DX -> tickTitleDx();
            case COMPLETE -> tickTitleDx();
        }
        refreshSnapshot();
    }

    public boolean isActive() {
        return stage != Stage.COMPLETE;
    }

    public void skipToTitle() {
        stage = Stage.COMPLETE;
        skipped = true;
        visibleScrollY = 0;
        visibleBg = visibleObj = null;
        verticalWaveOffset = 0;
        stageFrame = 0;
        scrollX = 0;
        scrollY = 0;
        beachScrollX = 0;
        lightning.clear();
        linkVisible = false;
        titleRowIndex = titleRows.size();
        titleRevealRows = titleRows.size();
        setScene(IntroCutsceneScript.SCENE_TITLE);
        // The direct title map already contains the logo; post-beach rows belong to the scrolled map.
        sprites = List.of();
        refreshSnapshot();
    }

    public String sceneId() {
        return switch (stage) {
            case SEA, SEA_FADE -> IntroCutsceneScript.SCENE_SEA;
            case LINK_FACE -> IntroCutsceneScript.SCENE_LINK_FACE;
            case BEACH_LOAD -> stageFrame == 0 ? IntroCutsceneScript.SCENE_SEA : IntroCutsceneScript.SCENE_BEACH;
            case BEACH_FADE, BEACH -> IntroCutsceneScript.SCENE_BEACH;
            case TITLE_REVEAL, TITLE_WAIT, TITLE_DX, COMPLETE -> IntroCutsceneScript.SCENE_TITLE;
        };
    }

    public int scrollX() {
        return sceneId().equals(IntroCutsceneScript.SCENE_BEACH) ? beachScrollX : scrollX;
    }

    public int scrollY() {
        return scrollY;
    }

    public int scrollXForLine(int screenY) {
        int[] lineScroll = snapshot == null ? null : snapshot.lineScrollX();
        if (lineScroll == null || lineScroll.length == 0) {
            return scrollX();
        }
        return lineScroll[Math.max(0, Math.min(screenY, lineScroll.length - 1))];
    }

    public int[] lineScrollX(int height) {
        int[] lineScroll = snapshot == null ? null : snapshot.lineScrollX();
        if (lineScroll == null) {
            return null;
        }
        int[] resized = new int[height];
        for (int index = 0; index < height; index++) {
            resized[index] = lineScroll[Math.min(index, lineScroll.length - 1)];
        }
        return resized;
    }

    public int titleRevealRows() {
        return titleRevealRows;
    }

    public List<IntroSprite> sprites() {
        return snapshot.sprites();
    }

    public IntroFrameSnapshot snapshot() {
        return snapshot;
    }

    protected int shipX() {
        return shipX;
    }

    protected void tickSea() {
        decrementLightning();
        if (shipX == 0x50) {
            lightning.clear();
            transitionTo(Stage.LINK_FACE);
            loadDelay = 3;
            return;
        }
        if (frameCounter <= 2) return; // initial map transfer, before the first sea handler
        updateWaveOffsets();
        animateWaves(false);
        if ((clock & 7) == 0) {
            scrollX = (scrollX + 1) & 255;
            shipX--;
            for (int i = 0; i < lightning.size(); i++) {
                Lightning l = lightning.get(i);
                lightning.set(i, new Lightning(l.variant(), l.x() - 1, l.y(), l.remaining()));
            }
            triggerSeaLightningIfNeeded();
        }
    }

    protected void tickLinkFace() {
        decrementLightning();
        linkFaceTimer++;
        if (linkFaceTimer == LINK_FACE_SCREAM_FRAME) rom.applyDrawCommands(tilemap, 0x74B8);
        if (linkFaceTimer == LINK_FACE_LIGHTNING_FRAME || (clock & 0x7f) == 0)
            playThunder(); // sound/palette flash only, no bolt entity
        if (linkFaceTimer >= LINK_FACE_FRAMES) {
            lightning.clear();
            seaFadeTimer = 1;
            transitionTo(Stage.SEA_FADE);
            loadDelay = 1;
        }
    }
}
