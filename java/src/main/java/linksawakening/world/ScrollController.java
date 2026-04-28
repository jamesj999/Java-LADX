package linksawakening.world;

public final class ScrollController {
    public static final int NONE = 0;
    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;

    private int direction = NONE;
    private int offset;
    private int target;
    private int linkScreenX;
    private int linkScreenY;
    private RoomRenderSnapshot previousRoom;

    public void start(int direction, int linkScreenX, int linkScreenY,
                      RoomRenderSnapshot previousRoom, int target) {
        this.direction = direction;
        this.linkScreenX = linkScreenX;
        this.linkScreenY = linkScreenY;
        this.previousRoom = previousRoom;
        this.target = target;
        this.offset = 0;
    }

    public boolean tick(int speed) {
        if (!isActive()) {
            return false;
        }
        offset += speed;
        if (offset < target) {
            return false;
        }
        offset = 0;
        direction = NONE;
        target = 0;
        previousRoom = null;
        return true;
    }

    public boolean isActive() {
        return direction != NONE;
    }

    public int direction() {
        return direction;
    }

    public int offset() {
        return offset;
    }

    public int target() {
        return target;
    }

    public int linkScreenX() {
        return linkScreenX;
    }

    public int linkScreenY() {
        return linkScreenY;
    }

    public RoomRenderSnapshot previousRoom() {
        return previousRoom;
    }
}
