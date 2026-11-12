package linksawakening.app.headless;

import linksawakening.world.entity.EntityStatus;

/**
 * Immutable replay checkpoint values; the driver exposes the compatibility State name. Created on demand for
 * one diagnostic capture.
 */
public abstract class HeadlessState {
    private final int frame;
    private final int map;
    private final int room;
    private final int x;
    private final int y;
    private final boolean dialog;
    private final boolean pageWait;
    private final String text;
    private final int shield;
    private final int health;
    private final int sword;
    private final int urchins;
    private final boolean toadstool;
    private final int powder;
    private final int inventoryTick;
    private final int tarinX;
    private final int tarinY;
    private final boolean forestCleared;
    private final int tailKey;
    private final int bgTick;
    private final int smallKeys;
    private final int compass;
    private final int dungeonMap;
    private final int nightmareKey;
    private final int animation;
    private final int direction;
    private final int swordAnimation;
    private final int z;
    private final int velocityZ;
    private final int speedX;
    private final int speedY;
    private final boolean loading;
    private final int maxHearts;
    private final int instrument1Flags;

    protected HeadlessState(SessionDiagnostics snapshot) {

        var tarin = snapshot.roomSession().rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x3F)
                .findFirst()
                .orElse(null);
        this.frame = snapshot.frameCounter();
        this.map = snapshot.roomSession().rooms().mapId();
        this.room = snapshot.roomSession().rooms().currentRoomId();
        this.x = snapshot.link().position().romEntityX();
        this.y = snapshot.link().position().romEntityY();
        this.dialog = snapshot.dialogController().isActive();
        this.pageWait = snapshot.dialogController().isWaitingForPageAdvance();
        this.text = snapshot.dialogController().visibleText();
        this.shield = snapshot.playerState().equipmentLevels().shieldLevel();
        this.health = snapshot.playerState().vitals().health();
        this.sword = snapshot.playerState().equipmentLevels().swordLevel();
        this.urchins = (int) snapshot.roomSession().rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xC5 && entity.status() == EntityStatus.ACTIVE)
                .count();
        this.toadstool = snapshot.playerState().ammunition().hasToadstool();
        this.powder = snapshot.playerState().ammunition().magicPowderCount();
        this.inventoryTick = snapshot.inventoryMenu().instrumentPaletteTick();
        this.tarinX = tarin == null ? -1 : tarin.x();
        this.tarinY = tarin == null ? -1 : tarin.y();
        this.forestCleared = (snapshot.roomSession().dungeon().overworldRoomStatusSnapshot()[0x51] & 0x10) != 0;
        this.tailKey = snapshot.playerState().progression().tailKeyCount();
        this.bgTick = snapshot.gpu().animatedTilesFrameCount();
        this.smallKeys = snapshot.roomSession().dungeon().currentDungeonItemFlagsSnapshot()[4] & 0xFF;
        this.compass = snapshot.roomSession().dungeon().currentDungeonItemFlagsSnapshot()[1] & 0xFF;
        this.dungeonMap = snapshot.roomSession().dungeon().currentDungeonItemFlagsSnapshot()[0] & 0xFF;
        this.nightmareKey = snapshot.roomSession().dungeon().currentDungeonItemFlagsSnapshot()[3] & 0xFF;
        this.animation = snapshot.link().animation().romAnimationState();
        this.direction = 3 - snapshot.link().animation().direction();
        this.swordAnimation = snapshot.equipmentController().activeSword() == null
                ? 0
                : snapshot.equipmentController().activeSword().state();
        this.z = snapshot.link().animation().romEntityZ();
        this.velocityZ = snapshot.link().airborne().zVelocity();
        this.speedX = snapshot.link().position().romSpeedX();
        this.speedY = snapshot.link().position().romSpeedY();
        this.loading = snapshot.scrollController().isActive()
                || snapshot.transitionController().isInputBlocked();
        this.maxHearts = snapshot.playerState().vitals().maxHearts();
        this.instrument1Flags = snapshot.roomSession().dungeon().dungeonProgressFlagsSnapshot()[0] & 0xFF;
    }

    public int frame() {
        return frame;
    }

    public int map() {
        return map;
    }

    public int room() {
        return room;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public boolean dialog() {
        return dialog;
    }

    public boolean pageWait() {
        return pageWait;
    }

    public String text() {
        return text;
    }

    public int shield() {
        return shield;
    }

    public int health() {
        return health;
    }

    public int sword() {
        return sword;
    }

    public int urchins() {
        return urchins;
    }

    public boolean toadstool() {
        return toadstool;
    }

    public int powder() {
        return powder;
    }

    public int inventoryTick() {
        return inventoryTick;
    }

    public int tarinX() {
        return tarinX;
    }

    public int tarinY() {
        return tarinY;
    }

    public boolean forestCleared() {
        return forestCleared;
    }

    public int tailKey() {
        return tailKey;
    }

    public int bgTick() {
        return bgTick;
    }

    public int smallKeys() {
        return smallKeys;
    }

    public int compass() {
        return compass;
    }

    public int dungeonMap() {
        return dungeonMap;
    }

    public int nightmareKey() {
        return nightmareKey;
    }

    public int animation() {
        return animation;
    }

    public int direction() {
        return direction;
    }

    public int swordAnimation() {
        return swordAnimation;
    }

    public int z() {
        return z;
    }

    public int velocityZ() {
        return velocityZ;
    }

    public int speedX() {
        return speedX;
    }

    public int speedY() {
        return speedY;
    }

    public boolean loading() {
        return loading;
    }

    public int maxHearts() {
        return maxHearts;
    }

    public int instrument1Flags() {
        return instrument1Flags;
    }

    @Override
    public boolean equals(Object value) {
        return HeadlessStateValues.equal(this, value);
    }

    @Override
    public int hashCode() {
        return HeadlessStateValues.hash(this);
    }

    @Override
    public String toString() {
        return HeadlessStateValues.text(this);
    }
}
