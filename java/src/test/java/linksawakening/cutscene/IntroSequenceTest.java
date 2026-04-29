package linksawakening.cutscene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntroSequenceTest {

    @Test
    void seaStageScrollsShipAndKeepsSpriteOverlaysActive() {
        IntroSequence intro = new IntroSequence();

        assertEquals(IntroCutsceneScript.SCENE_SEA, intro.sceneId());
        assertEquals(0, intro.scrollX());
        assertFalse(intro.sprites().isEmpty());

        tick(intro, 8);

        assertEquals(1, intro.scrollX());
        assertEquals(0xBF, intro.shipX());
        assertTrue(intro.sprites().stream().anyMatch(sprite -> sprite.tileIndex() == 0x1C));
    }

    @Test
    void introTransitionsThroughLinkFaceBeachAndTitle() {
        IntroSequence intro = new IntroSequence();

        tick(intro, IntroSequence.SEA_SCROLL_FRAMES);
        assertEquals(IntroCutsceneScript.SCENE_LINK_FACE, intro.sceneId());

        tick(intro, IntroSequence.LINK_FACE_FRAMES);
        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());

        tick(intro, IntroSequence.BEACH_FRAMES);
        assertEquals(IntroCutsceneScript.SCENE_TITLE, intro.sceneId());
        assertEquals(0, intro.titleRevealRows());

        tick(intro, 1);
        assertEquals(1, intro.titleRevealRows());

        tick(intro, IntroSequence.TITLE_HOLD_FRAMES);
        assertFalse(intro.isActive());
        assertEquals(TitleReveal.ROW_COUNT, intro.titleRevealRows());
    }

    @Test
    void beachStartsWithOnlyMarinMovingRightFromTheLeftAtTheOriginalCadence() {
        IntroSequence intro = new IntroSequence();

        tick(intro, IntroSequence.SEA_SCROLL_FRAMES + IntroSequence.LINK_FACE_FRAMES);
        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());

        ListAssert.assertSprite(intro.sprites(), 0x00, -0x18, 0x58, 3, 16);
        assertFalse(intro.sprites().stream().anyMatch(sprite -> sprite.tileIndex() == 0x50));

        tick(intro, 2);
        ListAssert.assertSprite(intro.sprites(), 0x00, -0x18, 0x58, 3, 16);

        tick(intro, 2);
        ListAssert.assertSprite(intro.sprites(), 0x00, -0x17, 0x58, 3, 16);
    }

    @Test
    void beachMarinUsesFullGbOamSpriteColumns() {
        IntroSequence intro = new IntroSequence();

        tick(intro, IntroSequence.SEA_SCROLL_FRAMES + IntroSequence.LINK_FACE_FRAMES);

        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());
        assertEquals(2, intro.sprites().stream().filter(sprite -> sprite.height() == 16).count());
        ListAssert.assertSprite(intro.sprites(), 0x00, -0x18, 0x58, 3, 16);
        ListAssert.assertSprite(intro.sprites(), 0x02, -0x10, 0x58, 3, 16);
    }

    @Test
    void beachLastsThroughMarinWalkAndLinkEntrance() {
        IntroSequence intro = new IntroSequence();

        tick(intro, IntroSequence.SEA_SCROLL_FRAMES + IntroSequence.LINK_FACE_FRAMES);
        tick(intro, 192);
        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());
        ListAssert.assertAnySpriteAtX(intro.sprites(), 0x18);

        tick(intro, 420 - 192);
        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());
        ListAssert.assertAnySpriteAtX(intro.sprites(), 0x51);

        tick(intro, 0x40 + 16);
        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());
        ListAssert.assertSprite(intro.sprites(), 0x10, 0xE6, 0x5E, 0, 16);
        assertTrue(intro.scrollX() > 0);
    }

    @Test
    void beachShoreBandUsesDisassemblySectionScrollForLinkApproach() {
        IntroSequence intro = new IntroSequence();

        tick(intro, IntroSequence.SEA_SCROLL_FRAMES + IntroSequence.LINK_FACE_FRAMES);
        tick(intro, 420 + 0x40 + 16);

        assertEquals(0x08, intro.scrollX());
        assertEquals(0x9D, intro.scrollXForLine(0x60));
    }

    @Test
    void beachContinuesAfterFirstLinkApproachBeforeFinalTitleReveal() {
        IntroSequence intro = new IntroSequence();

        tick(intro, IntroSequence.SEA_SCROLL_FRAMES + IntroSequence.LINK_FACE_FRAMES);
        tick(intro, 420 + 0x40 + 96);

        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());
        assertEquals(0x30, intro.scrollX());
        ListAssert.assertSprite(intro.sprites(), 0x10, 0x96, 0x5E, 0, 16);

        tick(intro, 32);

        assertEquals(IntroCutsceneScript.SCENE_BEACH, intro.sceneId());
        assertEquals(0x30, intro.scrollX());
        ListAssert.assertSprite(intro.sprites(), 0x10, 0x96, 0x5E, 0, 16);
    }

    @Test
    void titleSparkleUsesIntroSparkleSpriteTiles() {
        IntroSequence intro = new IntroSequence();

        tick(intro, IntroSequence.SEA_SCROLL_FRAMES
            + IntroSequence.LINK_FACE_FRAMES
            + IntroSequence.BEACH_FRAMES);

        assertEquals(IntroCutsceneScript.SCENE_TITLE, intro.sceneId());
        assertEquals(2, intro.sprites().size());
        assertTrue(intro.sprites().stream().allMatch(sprite ->
            sprite.tileIndex() == 0x38
                || sprite.tileIndex() == 0x3A
                || sprite.tileIndex() == 0x3C
                || sprite.tileIndex() == 0x3E
        ));
    }

    @Test
    void skipCompletesIntroOnFullyRevealedTitle() {
        IntroSequence intro = new IntroSequence();

        tick(intro, 32);
        intro.skipToTitle();

        assertFalse(intro.isActive());
        assertEquals(IntroCutsceneScript.SCENE_TITLE, intro.sceneId());
        assertEquals(TitleReveal.ROW_COUNT, intro.titleRevealRows());
        assertTrue(intro.sprites().isEmpty());
    }

    private static void tick(IntroSequence intro, int frames) {
        for (int i = 0; i < frames; i++) {
            intro.tick();
        }
    }

    private static final class ListAssert {
        private static void assertAnySpriteAtX(java.util.List<IntroSprite> sprites, int x) {
            assertTrue(sprites.stream().anyMatch(sprite -> sprite.x() == x));
        }

        private static void assertSprite(
            java.util.List<IntroSprite> sprites,
            int tileIndex,
            int x,
            int y,
            int paletteIndex,
            int height
        ) {
            assertTrue(sprites.stream().anyMatch(sprite ->
                sprite.tileIndex() == tileIndex
                    && sprite.x() == x
                    && sprite.y() == y
                    && sprite.paletteIndex() == paletteIndex
                    && sprite.height() == height
            ));
        }
    }
}
