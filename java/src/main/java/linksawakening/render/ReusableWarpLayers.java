package linksawakening.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.event.InstrumentWarpVisual;

/** Owns instrument-warp snapshot extraction, sprite fade scope, swirl and fullscreen uploads. */
final class ReusableWarpLayers {
    final List<RenderLayer> sprites = new ArrayList<>(10);
    private InstrumentWarpVisual visual;
    private int[][] sourceBg, sourceObj;
    int[][] backgroundPalettes, objectPalettes;
    private int[] tiles, attributes, commands;
    private ReusableGameFrameState state;
    private final Node fade = new Node(0), swirl = new Node(1), fullscreen = new Node(2);
    private HardwareFrame frame;
    private GPU gpu;
    private HardwareRenderLayer child;
    private final IntUnaryOperator transform = this::fadeColor;
    private final Runnable draw = this::drawSprites, drawChild = this::drawChild;

    void configure(ReusableGameFrameState state) {
        this.state = state;
        var room = state.roomInputs;
        if (visual != room.warp || sourceBg != room.palettes || sourceObj != room.objectPalettes) {
            visual = room.warp;
            sourceBg = room.palettes;
            sourceObj = room.objectPalettes;
            backgroundPalettes = visual.backgroundPalettes(sourceBg);
            objectPalettes = sourceObj == null ? null : visual.objectPalettes(sourceObj);
            tiles = visual.tiles();
            attributes = visual.attributes();
            commands = visual.swirl();
        }
        sprites.clear();
    }

    void addSprites(List<RenderLayer> layers) {
        layers.add(fade);
    }

    void addFinalPlanes(List<RenderLayer> layers) {
        if (state.roomInputs.entities != null && state.roomInputs.entities.spriteSelection() != null) layers.add(swirl);
        if (visual.commandBatches() > 0) layers.add(fullscreen);
    }

    private void drawSprites() {
        for (int i = 0; i < sprites.size(); i++) {
            child = (HardwareRenderLayer) sprites.get(i);
            frame.withLayerSource(child.getClass().getSimpleName(), drawChild);
        }
    }

    private void drawChild() {
        child.submit(frame, gpu);
    }

    private int fadeColor(int rgb) {
        int amount = visual.objectFade();
        if (amount == 0) return rgb;
        int result = 0;
        for (int shift = 16; shift >= 0; shift -= 8)
            result |= (Math.min(31, (((rgb >> shift) & 255) >> 3) + amount) << 3) << shift;
        return result;
    }

    private final class Node implements HardwareRenderLayer {
        private final int kind;

        Node(int kind) {
            this.kind = kind;
        }

        @Override
        public void submit(HardwareFrame model, GPU device) {
            if (kind == 0) {
                frame = model;
                gpu = device;
                model.withPaletteTransform(transform, draw);
            } else if (kind == 1) {
                if (commands.length != 256 || state.roomInputs.entities.spriteTiles() == null) return;
                var context = RenderContext.indexed(device, model);
                int start = ((state.frameCounter() >> 3) & 3) * 64;
                for (int i = 15; i >= 0; i--) {
                    int at = start + i * 4, tile = commands[at + 2], attr = commands[at + 3];
                    if (tile == 255) continue;
                    OamDrawing.draw8x16(
                            context,
                            state.roomInputs.entities.spriteTiles(),
                            tile,
                            visual.centerX() + (byte) commands[at + 1] - 8,
                            visual.centerY() + (byte) commands[at] - 16,
                            (attr & 32) != 0,
                            (attr & 64) != 0,
                            objectPalettes[Math.min(attr & 7, objectPalettes.length - 1)]);
                }
            } else {
                HardwareFrame scratch = model.backgroundScratch();
                HardwareBackgroundRenderer.renderBackground(
                        scratch, device, tiles, attributes, backgroundPalettes, 20, 18);
                model.copyBackgroundRows(scratch, Math.min(Framebuffer.HEIGHT, visual.commandBatches() * 16));
            }
        }
    }
}
