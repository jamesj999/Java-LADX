package linksawakening.gpu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TilemapLoader {
    
    public static int[] loadTilemap(String resourcePath) {
        try {
            InputStream is = TilemapLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                System.err.println("Tilemap not found: " + resourcePath);
                return null;
            }
            
            byte[] data = is.readAllBytes();
            
            int[] tilemap;
            
            // Regular 1-byte tile indices
            tilemap = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                tilemap[i] = Byte.toUnsignedInt(data[i]);
            }
            System.out.println("Tilemap: 1-byte entries, " + tilemap.length + " tiles");
            
            System.out.print("First 10 tile IDs: ");
            for (int i = 0; i < Math.min(10, tilemap.length); i++) {
                System.out.print(String.format("0x%02X ", tilemap[i]));
            }
            System.out.println();
            
            return tilemap;
            
        } catch (IOException e) {
            System.err.println("Failed to load tilemap: " + e.getMessage());
            return null;
        }
    }
}