package linksawakening.app.headless;

import java.util.Objects;
import linksawakening.world.entity.EntityStatus;

/**
 * Immutable replay checkpoint values; the driver exposes the compatibility State name. Created on demand for
 * one diagnostic capture.
 */
// Facing is retained as an integer checkpoint value to preserve raw snapshot/equality representation.
public final class ReplayCheckpoint implements HeadlessState {
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

    public ReplayCheckpoint(SessionDiagnostics snapshot) {

        var tarin = snapshot.roomSession().rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
        this.urchins = (int) snapshot.roomSession().rooms().activeRoomOrNull().entities().loadedEntities().stream()
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

    @Override
    public int frame() {
        return frame;
    }

    @Override
    public int map() {
        return map;
    }

    @Override
    public int room() {
        return room;
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }

    @Override
    public boolean dialog() {
        return dialog;
    }

    @Override
    public boolean pageWait() {
        return pageWait;
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public int shield() {
        return shield;
    }

    @Override
    public int health() {
        return health;
    }

    @Override
    public int sword() {
        return sword;
    }

    @Override
    public int urchins() {
        return urchins;
    }

    @Override
    public boolean toadstool() {
        return toadstool;
    }

    @Override
    public int powder() {
        return powder;
    }

    @Override
    public int inventoryTick() {
        return inventoryTick;
    }

    @Override
    public int tarinX() {
        return tarinX;
    }

    @Override
    public int tarinY() {
        return tarinY;
    }

    @Override
    public boolean forestCleared() {
        return forestCleared;
    }

    @Override
    public int tailKey() {
        return tailKey;
    }

    @Override
    public int bgTick() {
        return bgTick;
    }

    @Override
    public int smallKeys() {
        return smallKeys;
    }

    @Override
    public int compass() {
        return compass;
    }

    @Override
    public int dungeonMap() {
        return dungeonMap;
    }

    @Override
    public int nightmareKey() {
        return nightmareKey;
    }

    @Override
    public int animation() {
        return animation;
    }

    @Override
    public int direction() {
        return direction;
    }

    @Override
    public int swordAnimation() {
        return swordAnimation;
    }

    @Override
    public int z() {
        return z;
    }

    @Override
    public int velocityZ() {
        return velocityZ;
    }

    @Override
    public int speedX() {
        return speedX;
    }

    @Override
    public int speedY() {
        return speedY;
    }

    @Override
    public boolean loading() {
        return loading;
    }

    @Override
    public int maxHearts() {
        return maxHearts;
    }

    @Override
    public int instrument1Flags() {
        return instrument1Flags;
    }

    @Override
    public boolean equals(Object value) {
        return value instanceof ReplayCheckpoint other
                && frame == other.frame()
                && map == other.map()
                && room == other.room()
                && x == other.x()
                && y == other.y()
                && dialog == other.dialog()
                && pageWait == other.pageWait()
                && Objects.equals(text, other.text())
                && shield == other.shield()
                && health == other.health()
                && sword == other.sword()
                && urchins == other.urchins()
                && toadstool == other.toadstool()
                && powder == other.powder()
                && inventoryTick == other.inventoryTick()
                && tarinX == other.tarinX()
                && tarinY == other.tarinY()
                && forestCleared == other.forestCleared()
                && tailKey == other.tailKey()
                && bgTick == other.bgTick()
                && smallKeys == other.smallKeys()
                && compass == other.compass()
                && dungeonMap == other.dungeonMap()
                && nightmareKey == other.nightmareKey()
                && animation == other.animation()
                && direction == other.direction()
                && swordAnimation == other.swordAnimation()
                && z == other.z()
                && velocityZ == other.velocityZ()
                && speedX == other.speedX()
                && speedY == other.speedY()
                && loading == other.loading()
                && maxHearts == other.maxHearts()
                && instrument1Flags == other.instrument1Flags();
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + Integer.hashCode(frame);
        hash = 31 * hash + Integer.hashCode(map);
        hash = 31 * hash + Integer.hashCode(room);
        hash = 31 * hash + Integer.hashCode(x);
        hash = 31 * hash + Integer.hashCode(y);
        hash = 31 * hash + Boolean.hashCode(dialog);
        hash = 31 * hash + Boolean.hashCode(pageWait);
        hash = 31 * hash + Objects.hashCode(text);
        hash = 31 * hash + Integer.hashCode(shield);
        hash = 31 * hash + Integer.hashCode(health);
        hash = 31 * hash + Integer.hashCode(sword);
        hash = 31 * hash + Integer.hashCode(urchins);
        hash = 31 * hash + Boolean.hashCode(toadstool);
        hash = 31 * hash + Integer.hashCode(powder);
        hash = 31 * hash + Integer.hashCode(inventoryTick);
        hash = 31 * hash + Integer.hashCode(tarinX);
        hash = 31 * hash + Integer.hashCode(tarinY);
        hash = 31 * hash + Boolean.hashCode(forestCleared);
        hash = 31 * hash + Integer.hashCode(tailKey);
        hash = 31 * hash + Integer.hashCode(bgTick);
        hash = 31 * hash + Integer.hashCode(smallKeys);
        hash = 31 * hash + Integer.hashCode(compass);
        hash = 31 * hash + Integer.hashCode(dungeonMap);
        hash = 31 * hash + Integer.hashCode(nightmareKey);
        hash = 31 * hash + Integer.hashCode(animation);
        hash = 31 * hash + Integer.hashCode(direction);
        hash = 31 * hash + Integer.hashCode(swordAnimation);
        hash = 31 * hash + Integer.hashCode(z);
        hash = 31 * hash + Integer.hashCode(velocityZ);
        hash = 31 * hash + Integer.hashCode(speedX);
        hash = 31 * hash + Integer.hashCode(speedY);
        hash = 31 * hash + Boolean.hashCode(loading);
        hash = 31 * hash + Integer.hashCode(maxHearts);
        hash = 31 * hash + Integer.hashCode(instrument1Flags);
        return hash;
    }

    @Override
    public String toString() {
        return "State[" + "frame=" + frame + ", map=" + map + ", room=" + room + ", x="
                + x + ", y=" + y
                + ", dialog=" + dialog + ", pageWait=" + pageWait + ", text=" + text
                + ", shield=" + shield
                + ", health=" + health + ", sword=" + sword + ", urchins=" + urchins
                + ", toadstool=" + toadstool
                + ", powder=" + powder + ", inventoryTick=" + inventoryTick + ", tarinX="
                + tarinX + ", tarinY="
                + tarinY + ", forestCleared=" + forestCleared + ", tailKey=" + tailKey
                + ", bgTick=" + bgTick
                + ", smallKeys=" + smallKeys + ", compass=" + compass + ", dungeonMap="
                + dungeonMap
                + ", nightmareKey=" + nightmareKey + ", animation=" + animation + ", direction="
                + direction
                + ", swordAnimation=" + swordAnimation + ", z=" + z + ", velocityZ=" + velocityZ
                + ", speedX="
                + speedX + ", speedY=" + speedY + ", loading=" + loading + ", maxHearts="
                + maxHearts
                + ", instrument1Flags=" + instrument1Flags + "]";
    }
}
