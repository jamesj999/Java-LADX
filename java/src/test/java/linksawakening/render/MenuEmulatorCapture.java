package linksawakening.render;

import linksawakening.gpu.*;
import linksawakening.input.*;
import linksawakening.scene.*;
import linksawakening.ui.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Offline screenshots through the production menu controller and framebuffer. */
final class MenuEmulatorCapture {
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        byte[] rom;
        try (var stream = MenuEmulatorCapture.class.getResourceAsStream("/rom/azle.gbc")) {
            rom = stream.readAllBytes();
        }
        var config = new InputConfig(1,2,3,4,5,6,7);
        for (String line : Files.readAllLines(out.resolve("scenarios.tsv"))) {
            String[] fields = line.split("\t", -1);
            int mask = Integer.parseInt(fields[1]);
            boolean saved = mask != 0;
            GPU gpu = new GPU(); gpu.loadMenuTiles(rom);
            var loader = new BackgroundSceneLoader(rom);
            var menu = new FileMenuController(new FileMenuRomData(rom),
                id -> loader.load(BackgroundSceneCatalog.forFileMenuScene(id)),
                mask, saved ? new int[][]{{66,67,68,69,70},
                    mask == 7 ? new int[]{77,74,79,76,0} : new int[5],
                    mask == 7 ? new int[]{85,70,84,85,0} : new int[5]} : null,
                new int[]{10,14,3}, new int[]{0x123,0x999,0x007});
            var input = new InputState();
            for (String button : fields[2].split(",")) {
                if (button.isEmpty()) continue;
                int key = switch(button) {
                    case "start" -> 1; case "up" -> 2; case "down" -> 3;
                    case "left" -> 4; case "right" -> 5; case "a" -> 6; case "b" -> 7;
                    default -> throw new IllegalArgumentException(button);
                };
                input.onKeyEvent(key,1); menu.tick(input,config); input.tickEdges();
                input.onKeyEvent(key,0); menu.tick(input,config); input.tickEdges();
            }
            Path folder = out.resolve("ours").resolve(fields[0]); Files.createDirectories(folder);
            for (int frame=0;frame<32;frame++) {
                byte[] pixels = new byte[160*144*4];
                new GameFrameSceneBuilder().build(GameFrameState.empty().withScreen(RenderScreen.FILE_MENU)
                    .withFileMenuFrame(menu.snapshot())).drawTo(pixels,gpu);
                BufferedImage image = new BufferedImage(160,144,BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<144;y++) for(int x=0;x<160;x++) {
                    int i=(y*160+x)*4;
                    image.setRGB(x,y,0xff000000|((pixels[i]&255)<<16)|((pixels[i+1]&255)<<8)|(pixels[i+2]&255));
                }
                ImageIO.write(image,"png",folder.resolve(String.format("%02d.png",frame)).toFile());
                menu.tick(input,config);input.tickEdges();
            }
        }
    }
}
