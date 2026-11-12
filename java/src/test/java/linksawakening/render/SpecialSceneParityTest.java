package linksawakening.render;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import linksawakening.cutscene.IntroSprite;
import linksawakening.ui.FileMenuFrameSnapshot;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Pixel and scene-lifetime parity for all previously owned-only presentation paths. */
final class SpecialSceneParityTest {
    @ParameterizedTest
    @ValueSource(
            strings = {
                "INTRO",
                "MENU",
                "INVENTORY",
                "PASSOUT",
                "MARIN",
                "TARIN",
                "GOT",
                "WARP",
                "FULLSCREEN",
                "HEART",
                "SCROLL"
            })
    void reusableScenesMatchOwnedPresentation(String kind) {
        var fixture = new SceneConstructionFixture();
        var state = new ReusableGameFrameState().copyFrom(fixture.state);
        switch (kind) {
            case "INTRO" -> state.copyFrom(SpecialSceneAllocationTest.intro());
            case "MENU" -> {
                var menu = new FileMenuFrameSnapshot.Builder()
                        .sceneId("synthetic")
                        .selectedSlot(0)
                        .nameBytes(new int[5])
                        .tilemap(new int[1024])
                        .attrmap(new int[1024])
                        .bgPalettes(state.roomInputs.palettes)
                        .objectPalettes(state.roomInputs.objectPalettes)
                        .sprites(List.of(new IntroSprite(4, 30, 30, 0, true, false, 16)))
                        .build();
                state.reset()
                        .withScreen(RenderScreen.FILE_MENU)
                        .withFileMenuFrame(menu)
                        .withIntroFrameSnapshot(
                                SpecialSceneAllocationTest.intro().introFrameSnapshot());
            }
            case "INVENTORY" -> state.inventoryController().transition().open(false);
            case "PASSOUT" -> state.link().status().beginPassOut();
            case "MARIN" -> state.link().animation().showMarinWakeUpBed(3);
            case "TARIN" -> state.link().animation().showTarinShieldPresentation(state.roomInputs.objectPalettes[7]);
            case "GOT" -> state.link().animation().presentToadstool();
            case "WARP", "FULLSCREEN" -> {
                int[] commands = new int[256];
                for (int i = 0; i < commands.length; i += 4) {
                    commands[i] = i / 4;
                    commands[i + 1] = i / 8;
                    commands[i + 2] = 0x40 + (i & 0x3e);
                    commands[i + 3] = (i & 3) | 0x20;
                }
                var visual = new InstrumentWarpVisual(
                        3, 5, kind.equals("WARP") ? 0 : 4, new int[360], new int[360], 70, 50, commands);
                var room = state.roomInputs;
                state.withRoomData(
                        room.tiles, room.attributes, room.palettes, room.entities, visual, null, room.grassPalette);
            }
            case "HEART" -> {
                int[] pairs = new int[20];
                for (int i = 0; i < pairs.length; i += 2) {
                    pairs[i] = 0x40 + i;
                    pairs[i + 1] = i & 7;
                }
                var room = state.roomInputs;
                state.withRoomData(
                        room.tiles,
                        room.attributes,
                        room.palettes,
                        room.entities,
                        null,
                        new HeartPieceVisual(3, pairs),
                        room.grassPalette);
            }
            case "SCROLL" -> state.scrollController().start(ScrollController.RIGHT, 40, 40, fixture.state.room(), 160);
            default -> throw new AssertionError(kind);
        }
        var live = new GameFrameSceneBuilder();
        var owned = new GameFrameSceneBuilder();
        fixture.configure(live);
        fixture.configure(owned);
        byte[] expected = new byte[fixture.output.length];
        FrameScene scene = null;
        for (int frame = 0; frame < 80; frame++) {
            state.withFrameCounter(frame);
            if (kind.equals("INVENTORY")) {
                if (frame == 48) state.inventoryController().transition().close();
                state.inventoryController().tick();
            }
            if (kind.equals("PASSOUT")) state.link().status().presentPassOut(192 - frame, 0, 0);
            if (kind.equals("MARIN")) state.link().animation().showMarinWakeUpBed(frame % 5);
            if (kind.equals("SCROLL")) state.scrollController().tick(2);
            owned.build(state.snapshot()).drawTo(expected, fixture.gpu);
            FrameScene next = live.build(state);
            if (scene != null) assertSame(scene, next);
            scene = next;
            scene.drawTo(fixture.output, fixture.gpu);
            assertArrayEquals(expected, fixture.output, kind + " frame " + frame);
        }
    }
}
