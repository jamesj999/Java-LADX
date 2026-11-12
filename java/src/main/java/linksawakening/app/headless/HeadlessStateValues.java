package linksawakening.app.headless;

import java.util.Objects;
import linksawakening.app.HeadlessGameDriver;

/** Stable checkpoint equality, hashing and text formatting. */
final class HeadlessStateValues {
    private HeadlessStateValues() {}

    static boolean equal(HeadlessState state, Object value) {
        return value instanceof HeadlessGameDriver.State other
                && state.frame() == other.frame()
                && state.map() == other.map()
                && state.room() == other.room()
                && state.x() == other.x()
                && state.y() == other.y()
                && state.dialog() == other.dialog()
                && state.pageWait() == other.pageWait()
                && Objects.equals(state.text(), other.text())
                && state.shield() == other.shield()
                && state.health() == other.health()
                && state.sword() == other.sword()
                && state.urchins() == other.urchins()
                && state.toadstool() == other.toadstool()
                && state.powder() == other.powder()
                && state.inventoryTick() == other.inventoryTick()
                && state.tarinX() == other.tarinX()
                && state.tarinY() == other.tarinY()
                && state.forestCleared() == other.forestCleared()
                && state.tailKey() == other.tailKey()
                && state.bgTick() == other.bgTick()
                && state.smallKeys() == other.smallKeys()
                && state.compass() == other.compass()
                && state.dungeonMap() == other.dungeonMap()
                && state.nightmareKey() == other.nightmareKey()
                && state.animation() == other.animation()
                && state.direction() == other.direction()
                && state.swordAnimation() == other.swordAnimation()
                && state.z() == other.z()
                && state.velocityZ() == other.velocityZ()
                && state.speedX() == other.speedX()
                && state.speedY() == other.speedY()
                && state.loading() == other.loading()
                && state.maxHearts() == other.maxHearts()
                && state.instrument1Flags() == other.instrument1Flags();
    }

    static int hash(HeadlessState state) {
        int hash = 0;
        hash = 31 * hash + Integer.hashCode(state.frame());
        hash = 31 * hash + Integer.hashCode(state.map());
        hash = 31 * hash + Integer.hashCode(state.room());
        hash = 31 * hash + Integer.hashCode(state.x());
        hash = 31 * hash + Integer.hashCode(state.y());
        hash = 31 * hash + Boolean.hashCode(state.dialog());
        hash = 31 * hash + Boolean.hashCode(state.pageWait());
        hash = 31 * hash + Objects.hashCode(state.text());
        hash = 31 * hash + Integer.hashCode(state.shield());
        hash = 31 * hash + Integer.hashCode(state.health());
        hash = 31 * hash + Integer.hashCode(state.sword());
        hash = 31 * hash + Integer.hashCode(state.urchins());
        hash = 31 * hash + Boolean.hashCode(state.toadstool());
        hash = 31 * hash + Integer.hashCode(state.powder());
        hash = 31 * hash + Integer.hashCode(state.inventoryTick());
        hash = 31 * hash + Integer.hashCode(state.tarinX());
        hash = 31 * hash + Integer.hashCode(state.tarinY());
        hash = 31 * hash + Boolean.hashCode(state.forestCleared());
        hash = 31 * hash + Integer.hashCode(state.tailKey());
        hash = 31 * hash + Integer.hashCode(state.bgTick());
        hash = 31 * hash + Integer.hashCode(state.smallKeys());
        hash = 31 * hash + Integer.hashCode(state.compass());
        hash = 31 * hash + Integer.hashCode(state.dungeonMap());
        hash = 31 * hash + Integer.hashCode(state.nightmareKey());
        hash = 31 * hash + Integer.hashCode(state.animation());
        hash = 31 * hash + Integer.hashCode(state.direction());
        hash = 31 * hash + Integer.hashCode(state.swordAnimation());
        hash = 31 * hash + Integer.hashCode(state.z());
        hash = 31 * hash + Integer.hashCode(state.velocityZ());
        hash = 31 * hash + Integer.hashCode(state.speedX());
        hash = 31 * hash + Integer.hashCode(state.speedY());
        hash = 31 * hash + Boolean.hashCode(state.loading());
        hash = 31 * hash + Integer.hashCode(state.maxHearts());
        hash = 31 * hash + Integer.hashCode(state.instrument1Flags());
        return hash;
    }

    static String text(HeadlessState state) {
        return "State[" + "frame=" + state.frame() + ", map=" + state.map() + ", room=" + state.room() + ", x="
                + state.x() + ", y=" + state.y()
                + ", dialog=" + state.dialog() + ", pageWait=" + state.pageWait() + ", text=" + state.text()
                + ", shield=" + state.shield()
                + ", health=" + state.health() + ", sword=" + state.sword() + ", urchins=" + state.urchins()
                + ", toadstool=" + state.toadstool()
                + ", powder=" + state.powder() + ", inventoryTick=" + state.inventoryTick() + ", tarinX="
                + state.tarinX() + ", tarinY="
                + state.tarinY() + ", forestCleared=" + state.forestCleared() + ", tailKey=" + state.tailKey()
                + ", bgTick=" + state.bgTick()
                + ", smallKeys=" + state.smallKeys() + ", compass=" + state.compass() + ", dungeonMap="
                + state.dungeonMap()
                + ", nightmareKey=" + state.nightmareKey() + ", animation=" + state.animation() + ", direction="
                + state.direction()
                + ", swordAnimation=" + state.swordAnimation() + ", z=" + state.z() + ", velocityZ=" + state.velocityZ()
                + ", speedX="
                + state.speedX() + ", speedY=" + state.speedY() + ", loading=" + state.loading() + ", maxHearts="
                + state.maxHearts()
                + ", instrument1Flags=" + state.instrument1Flags() + "]";
    }
}
