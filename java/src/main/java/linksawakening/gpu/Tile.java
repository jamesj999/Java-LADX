package linksawakening.gpu;

public class Tile {
    private final int[] colorIndex;
    
    public Tile() {
        colorIndex = new int[64];
    }
    
    public Tile(int[] pixels) {
        this.colorIndex = pixels;
    }
    
    public int getPixel(int x, int y) {
        return colorIndex[y * 8 + x];
    }
    
    public void setPixel(int x, int y, int color) {
        colorIndex[y * 8 + x] = color;
    }
}