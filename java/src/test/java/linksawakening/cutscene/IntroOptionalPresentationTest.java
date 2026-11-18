package linksawakening.cutscene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class IntroOptionalPresentationTest {
    @Test
    void uniformScrollAndMissingTileUploadRemainDistinctFromEmptyMaps() {
        var builder = new IntroFrameSnapshot.Builder()
                .sceneId("")
                .substate("")
                .tilemap(new int[0])
                .attrmap(new int[0])
                .bgPalettes(new int[0][])
                .objPalettes(new int[0][])
                .sprites(List.of());
        var absent = builder.build();
        assertFalse(absent.hasLineScrollX());
        assertFalse(absent.hasLineScrollY());
        assertFalse(absent.hasAnimatedTiles());
        assertNull(absent.lineScrollX());
        assertNull(absent.animatedTiles());
        assertEquals(19, absent.scrollXAt(50, 19));
        var empty = builder.lineScrollX(new int[0]).build();
        assertTrue(empty.hasLineScrollX());
        assertEquals(0, empty.lineScrollX().length);
        assertEquals(19, empty.scrollXAt(50, 19));
        var mapped = builder.lineScrollX(new int[] {3, 4}).build();
        assertEquals(3, mapped.scrollXAt(-1, 19));
        assertEquals(4, mapped.scrollXAt(50, 19));
    }
}
