package linksawakening.cutscene;

import linksawakening.scene.BackgroundScene;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntroSequenceTest {

    @Test
    void initialSnapshotUsesRomShipDataAndIsImmutable() {
        IntroSequence intro = newIntro();

        IntroFrameSnapshot snapshot = intro.snapshot();

        assertEquals(IntroCutsceneScript.SCENE_SEA, snapshot.sceneId());
        assertEquals("SEA", snapshot.substate());
        assertEquals(0, snapshot.frameCounter());
        assertEquals(22, snapshot.sprites().size());
        assertTrue(snapshot.sprites().stream().anyMatch(sprite ->
            sprite.tileIndex() == 0x1C && sprite.x() == 0xB8 && sprite.y() == 0x3F));

        int[] tilemap = snapshot.tilemap();
        tilemap[0] = 0xFF;
        assertEquals(0x7E, intro.snapshot().tilemap()[0]);
        assertThrows(UnsupportedOperationException.class,
            () -> snapshot.sprites().add(snapshot.sprites().get(0)));
    }

    @Test
    void seaMovesShipAndScrollOnlyOnTheSourceEightFrameCadence() {
        IntroSequence intro = newIntro();

        tick(intro, 7);
        assertEquals(0, intro.snapshot().scrollX());
        assertEquals(0xB8, spriteX(intro.snapshot(), 0x1C));

        tick(intro, 1);
        assertEquals(1, intro.snapshot().scrollX());
        assertEquals(0xB7, spriteX(intro.snapshot(), 0x1C));
        assertEquals(22, intro.snapshot().sprites().size());
    }

    @Test
    void followsSeaLinkFaceAndSecondSeaFadeBoundaries() {
        IntroSequence intro = newIntro();

        tick(intro, IntroSequence.SEA_SCROLL_FRAMES);
        assertEquals(IntroCutsceneScript.SCENE_LINK_FACE, intro.sceneId());
        assertEquals("LINK_FACE", intro.snapshot().substate());

        tick(intro, 127);
        assertEquals("LINK_FACE", intro.snapshot().substate());
        tick(intro, 1);
        assertEquals("LINK_FACE_SCREAM", intro.snapshot().substate());
        tick(intro, 16);
        assertEquals("LINK_FACE_LIGHTNING", intro.snapshot().substate());
        assertTrue(intro.snapshot().sprites().stream().anyMatch(sprite -> sprite.tileIndex() == 0x34));

        tick(intro, 16);
        assertEquals(IntroCutsceneScript.SCENE_SEA, intro.sceneId());
        assertEquals("SEA_FADE", intro.snapshot().substate());

        tick(intro, IntroSequence.SEA_FADE_FRAMES);
        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());
        assertEquals("BEACH_FADE", intro.snapshot().substate());
    }

    @Test
    void beachPublishesLineScrollAndTitleRowsInRomPointerOrder() {
        IntroSequence intro = newIntro();
        tick(intro, IntroSequence.SEA_SCROLL_FRAMES
            + IntroSequence.LINK_FACE_FRAMES
            + IntroSequence.SEA_FADE_FRAMES);

        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());
        tick(intro, IntroSequence.BEACH_FADE_FRAMES);
        assertEquals("MARIN_STATE_0", intro.snapshot().substate());
        assertNotNull(intro.snapshot().lineScrollX());

        tickUntil(intro, IntroCutsceneScript.SCENE_TITLE, 3000);
        assertEquals("TITLE_REVEAL", intro.snapshot().substate());
        assertEquals(0, intro.titleRevealRows());

        tick(intro, 1);
        IntroRomData.TitleRow firstRow = new IntroRomData(loadRom()).titleRows().get(0);
        assertEquals(1, intro.titleRevealRows());
        assertArrayEquals(firstRow.tileBytes(), rowBytes(intro.snapshot().tilemap(),
            firstRow.tileTargetAddress()));
        assertArrayEquals(firstRow.attributeBytes(), rowBytes(intro.snapshot().attrmap(),
            firstRow.attributeTargetAddress()));
    }

    @Test
    void marinStateThreePreservesTheSourceShoreScrollPauses() {
        IntroSequence intro = newIntro();
        tick(intro, IntroSequence.SEA_SCROLL_FRAMES
            + IntroSequence.LINK_FACE_FRAMES
            + IntroSequence.SEA_FADE_FRAMES
            + IntroSequence.BEACH_FADE_FRAMES);
        tickUntilSubstate(intro, "MARIN_STATE_3", 3000);

        assertEquals(0x30, intro.snapshot().scrollX());
        tick(intro, 0x40);
        assertEquals(0x30, intro.snapshot().scrollX());

        tickUntilScroll(intro, 0x3A, 300);
        tick(intro, 0x30);
        assertEquals(0x3A, intro.snapshot().scrollX());

        tickUntilScroll(intro, 0x40, 300);
        tick(intro, 0x50);
        assertEquals(0x40, intro.snapshot().scrollX());
    }

    @Test
    void skipPublishesStableTitleWithoutBeachSprites() {
        IntroSequence intro = newIntro();

        tick(intro, 32);
        intro.skipToTitle();

        assertFalse(intro.isActive());
        assertEquals(IntroCutsceneScript.SCENE_TITLE, intro.sceneId());
        assertEquals(7, intro.titleRevealRows());
        assertTrue(intro.snapshot().sprites().isEmpty());
    }

    private static IntroSequence newIntro() {
        byte[] rom = loadRom();
        return new IntroSequence(rom, IntroSequenceTest::backgroundFor);
    }

    private static BackgroundScene backgroundFor(String sceneId) {
        int fill = IntroCutsceneScript.SCENE_TITLE.equals(sceneId) ? 0 : 0x7E;
        int[] tilemap = new int[32 * 32];
        int[] attrmap = new int[32 * 32];
        Arrays.fill(tilemap, fill);
        int[][] palettes = new int[8][4];
        int[][] objectPalettes = new int[8][4];
        return new BackgroundScene(tilemap, attrmap, palettes, objectPalettes);
    }

    private static int spriteX(IntroFrameSnapshot snapshot, int tileIndex) {
        return snapshot.sprites().stream()
            .filter(sprite -> sprite.tileIndex() == tileIndex)
            .findFirst()
            .orElseThrow()
            .x();
    }

    private static int[] rowBytes(int[] map, int targetAddress) {
        int index = targetAddress - 0x9800;
        return Arrays.copyOfRange(map, index, index + 16);
    }

    private static void tickUntil(IntroSequence intro, String sceneId, int maxFrames) {
        for (int frame = 0; frame < maxFrames && !sceneId.equals(intro.sceneId()); frame++) {
            intro.tick();
        }
        assertEquals(sceneId, intro.sceneId());
    }

    private static void tickUntilSubstate(IntroSequence intro, String substate, int maxFrames) {
        for (int frame = 0; frame < maxFrames && !substate.equals(intro.snapshot().substate()); frame++) {
            intro.tick();
        }
        assertEquals(substate, intro.snapshot().substate());
    }

    private static void tickUntilScroll(IntroSequence intro, int scroll, int maxFrames) {
        for (int frame = 0; frame < maxFrames && intro.snapshot().scrollX() != scroll; frame++) {
            intro.tick();
        }
        assertEquals(scroll, intro.snapshot().scrollX());
    }

    private static void tick(IntroSequence intro, int frames) {
        for (int frame = 0; frame < frames; frame++) {
            intro.tick();
        }
    }

    private static byte[] loadRom() {
        try (InputStream stream = IntroSequenceTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load test ROM", exception);
        }
    }
}
