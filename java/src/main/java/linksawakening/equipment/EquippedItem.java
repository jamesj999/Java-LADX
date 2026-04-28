package linksawakening.equipment;

/**
 * Something Link can carry in the A or B button slot and activate by pressing
 * that button. Each inventory id (see {@link linksawakening.state.PlayerState})
 * maps to at most one {@code EquippedItem} via {@link ItemRegistry}.
 *
 * <p>Mirrors the disassembly's item-use path (see {@code CheckItemsToUse} in
 * {@code home/check_items_to_use.asm}): per-frame the game edge-detects A/B
 * against the slot's inventory id, then calls item-specific code.
 *
 * <p>All methods have no-op defaults so implementations only override what
 * they need.
 */
public interface EquippedItem {

    /** Called once on the frame the bound button is pressed. */
    default void onPress() {}

    /** Called once on the frame the bound button is released. */
    default void onRelease() {}

    /**
     * Per-frame update; runs every frame regardless of button state so
     * internal state machines (like the sword swing) can finish after the
     * button is released. {@code buttonHeld} reflects the current frame.
     */
    default void tick(boolean buttonHeld) {}

    /** True while this item prevents Link from walking. */
    default boolean blocksMotion() {
        return false;
    }

    /**
     * Whether this item should keep Link's current facing direction while the
     * D-pad is held. Mirrors the ROM's wC16E gate around hLinkDirection writes.
     */
    default boolean locksFacing() {
        return false;
    }

    /**
     * Override Link's {@code hLinkAnimationState} this frame; return -1 for no
     * override. Called during rendering to let items drive Link's pose (e.g.
     * sword swing frames).
     */
    default int overrideAnimationState(int direction, int walkFrame) {
        return -1;
    }

    /**
     * Paint any auxiliary sprites for this item (e.g. the sword blade) into
     * the display buffer. Called after Link's body is drawn so the blade
     * overlays him correctly. {@code linkPixelX/Y} is Link's world-space
     * top-left; {@code offsetX/Y} applies the same camera/scroll shift used
     * for Link so the item tracks with him.
     */
    default void render(byte[] displayBuffer, int linkPixelX, int linkPixelY,
                        int direction, int offsetX, int offsetY) {
    }
}
