package linksawakening.gpu;

import java.nio.ByteBuffer;

public class Framebuffer {
    
    public static final int WIDTH = 160;
    public static final int HEIGHT = 144;
    public static final int SCALE = 4;
    
    private ByteBuffer buffer;
    
    public Framebuffer() {
        buffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4).order(java.nio.ByteOrder.nativeOrder());
    }
    
    public void clear(int r, int g, int b) {
        for (int i = 0; i < WIDTH * HEIGHT; i++) {
            int index = i * 4;
            buffer.put(index + 0, (byte) r);
            buffer.put(index + 1, (byte) g);
            buffer.put(index + 2, (byte) b);
            buffer.put(index + 3, (byte) 255);
        }
    }
    
    public void clear() {
        for (int i = 0; i < WIDTH * HEIGHT * 4; i++) {
            buffer.put(i, (byte) 0);
        }
    }
    
    public void drawTile(Tile tile, int x, int y) {
        if (tile == null || x < -8 || x >= WIDTH || y < -8 || y >= HEIGHT) return;
        
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = x + tx;
                int py = y + ty;
                
                if (px >= 0 && px < WIDTH && py >= 0 && py < HEIGHT) {
                    int color = tile.getPixel(tx, ty);
                    int index = (py * WIDTH + px) * 4;
                    buffer.put(index + 0, (byte) ((color >> 16) & 0xFF));
                    buffer.put(index + 1, (byte) ((color >> 8) & 0xFF));
                    buffer.put(index + 2, (byte) (color & 0xFF));
                    buffer.put(index + 3, (byte) 255);
                }
            }
        }
    }
    
    public void drawTileFlipped(Tile tile, int x, int y, boolean flipX, boolean flipY) {
        if (tile == null || x < -8 || x >= WIDTH || y < -8 || y >= HEIGHT) return;
        
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = x + (flipX ? 7 - tx : tx);
                int py = y + (flipY ? 7 - ty : ty);
                
                if (px >= 0 && px < WIDTH && py >= 0 && py < HEIGHT) {
                    int color = tile.getPixel(tx, ty);
                    int index = (py * WIDTH + px) * 4;
                    buffer.put(index + 0, (byte) ((color >> 16) & 0xFF));
                    buffer.put(index + 1, (byte) ((color >> 8) & 0xFF));
                    buffer.put(index + 2, (byte) (color & 0xFF));
                    buffer.put(index + 3, (byte) 255);
                }
            }
        }
    }
    
    public void drawTilemap(GPU gpu, int[] tilemap, int startX, int startY, int width, int height) {
        if (tilemap == null || tilemap.length == 0) return;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int mapIndex = y * width + x;
                if (mapIndex >= tilemap.length) break;
                
                int tileId = tilemap[mapIndex];
                Tile tile = gpu.getTile(tileId & 0xFF);
                
                if (tile != null) {
                    drawTile(tile, startX + x * 8, startY + y * 8);
                }
            }
        }
    }
    
    public ByteBuffer getBuffer() {
        buffer.rewind();
        return buffer;
    }
    
    public void rewind() {
        buffer.rewind();
    }
}
