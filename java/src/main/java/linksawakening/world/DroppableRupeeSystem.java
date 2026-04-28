package linksawakening.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;
import linksawakening.state.PlayerState;

/**
 * Minimal ROM-backed runtime for bush-spawned droppable rupees.
 *
 * <p>This follows the bush-cut path in bank0.asm: a 1/8 drop gate, a second
 * 50/50 rupee-vs-heart roll, spawn timers/countdowns matching the entity
 * setup, and the same OBJ tile/palette used by the original droppable rupee
 * entity handler.
 */
public final class DroppableRupeeSystem {

    private static final int OBJECT_BUSH = 0x5C;
    private static final int OBJECT_BUSH_GROUND_STAIRS = 0xD3;

    private static final int RUPEE_TILE_INDEX = 0xA6;
    private static final int DROP_GATE_MASK = 0x07;
    private static final int SLOW_COUNTDOWN_START = 0x80;
    private static final int PICKUP_DELAY_START = 0x18;
    private static final int INITIAL_SPEED_Z = 0x10;
    private static final int GRAVITY_PER_TICK = 0x02;

    // Hitbox derived from HitboxPositions[HITFLAGS_COLLISION_BOX_SMALL|PICKUP].
    private static final int PICKUP_HITBOX_X = 0x08;
    private static final int PICKUP_HITBOX_Y = 0x07;
    private static final int PICKUP_HITBOX_W = 0x06;
    private static final int PICKUP_HITBOX_H = 0x0A;

    // OAM_GBC_PAL_5 is the sixth always-loaded gameplay OBJ palette in
    // ObjectPalettes: blue/cyan objects such as blue rupees and bombs.
    private static final int[] BLUE_OBJECTS_SPRITE_PALETTE = {
        0x00000000,
        0x0070A8F8,
        0x000000F8,
        0x00000000,
    };

    public record Slot(int slotIndex, int worldX, int worldY, int posZSubpixels,
                       int slowCountdown, int pickupDelay, boolean visible) {}

    private static final class MutableSlot {
        private final int slotIndex;
        private int worldX;
        private int worldY;
        private int posZSubpixels;
        private int speedZSubpixels;
        private int slowCountdown;
        private int pickupDelay;

        private MutableSlot(int slotIndex, int worldX, int worldY) {
            this.slotIndex = slotIndex;
            this.worldX = worldX;
            this.worldY = worldY;
            this.slowCountdown = SLOW_COUNTDOWN_START;
            this.pickupDelay = PICKUP_DELAY_START;
            this.speedZSubpixels = INITIAL_SPEED_Z;
        }

        private Slot snapshot() {
            return new Slot(slotIndex, worldX, worldY, posZSubpixels, slowCountdown,
                pickupDelay, isVisible(slowCountdown));
        }
    }

    private final MutableSlot[] slots;
    private final IntSupplier randomByteSupplier;

    public DroppableRupeeSystem(int capacity, IntSupplier randomByteSupplier) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0");
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("randomByteSupplier must not be null");
        }
        this.slots = new MutableSlot[capacity];
        this.randomByteSupplier = randomByteSupplier;
    }

    public boolean maybeSpawnFromBush(int originalObjectId, int worldX, int worldY) {
        if (originalObjectId == OBJECT_BUSH_GROUND_STAIRS || originalObjectId != OBJECT_BUSH) {
            return false;
        }
        if ((nextRandomByte() & DROP_GATE_MASK) != 0) {
            return false;
        }
        if ((nextRandomByte() & 0x01) != 0) {
            return false;
        }

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                slots[i] = new MutableSlot(i, worldX, worldY);
                return true;
            }
        }
        return false;
    }

    public void tick(int linkPixelX, int linkPixelY, PlayerState playerState) {
        if (playerState == null) {
            return;
        }

        for (int i = 0; i < slots.length; i++) {
            MutableSlot slot = slots[i];
            if (slot == null) {
                continue;
            }

            if (slot.slowCountdown > 0) {
                slot.slowCountdown--;
            }
            if (slot.pickupDelay > 0) {
                slot.pickupDelay--;
            }
            if (slot.slowCountdown == 0) {
                slots[i] = null;
                continue;
            }

            if (slot.pickupDelay == 0 && intersectsLink(slot, linkPixelX, linkPixelY)) {
                playerState.setRupees(playerState.rupees() + 1);
                slots[i] = null;
                continue;
            }

            tickBouncePhysics(slot);
        }
    }

    public void render(byte[] displayBuffer, GPU gpu) {
        if (displayBuffer == null || gpu == null) {
            return;
        }

        Tile top = gpu.getTile(RUPEE_TILE_INDEX);
        Tile bottom = gpu.getTile(RUPEE_TILE_INDEX + 1);
        for (MutableSlot slot : slots) {
            if (slot == null || !isVisible(slot.slowCountdown)) {
                continue;
            }

            int screenX = slot.worldX - 4;
            int screenY = visualY(slot) - 16;
            drawSpriteTile(displayBuffer, top, screenX, screenY);
            drawSpriteTile(displayBuffer, bottom, screenX, screenY + 8);
        }
    }

    public void clear() {
        Arrays.fill(slots, null);
    }

    public int activeCount() {
        int count = 0;
        for (MutableSlot slot : slots) {
            if (slot != null) {
                count++;
            }
        }
        return count;
    }

    public List<Slot> activeSlots() {
        List<Slot> active = new ArrayList<>(slots.length);
        for (MutableSlot slot : slots) {
            if (slot != null) {
                active.add(slot.snapshot());
            }
        }
        return List.copyOf(active);
    }

    private int nextRandomByte() {
        return randomByteSupplier.getAsInt() & 0xFF;
    }

    private static void tickBouncePhysics(MutableSlot slot) {
        slot.posZSubpixels += slot.speedZSubpixels;
        slot.speedZSubpixels -= GRAVITY_PER_TICK;

        if (slot.posZSubpixels >= 0) {
            return;
        }

        slot.posZSubpixels = 0;
        if (slot.speedZSubpixels < 0) {
            slot.speedZSubpixels = Math.max(0, ((-slot.speedZSubpixels) - 1) / 2);
        }
    }

    private static boolean intersectsLink(MutableSlot slot, int linkPixelX, int linkPixelY) {
        int linkPosX = linkPixelX + 8;
        int linkPosY = linkPixelY + 16;

        int deltaX = Math.abs((slot.worldX + PICKUP_HITBOX_X) - linkPosX - 8);
        if (deltaX >= 4 + PICKUP_HITBOX_W) {
            return false;
        }

        int deltaY = Math.abs((visualY(slot) + PICKUP_HITBOX_Y) - linkPosY - 8);
        return deltaY < 4 + PICKUP_HITBOX_H;
    }

    private static int visualY(MutableSlot slot) {
        return slot.worldY - (slot.posZSubpixels >> 4);
    }

    private static boolean isVisible(int slowCountdown) {
        return slowCountdown >= 0x1C || (slowCountdown & 0x01) != 0;
    }

    private static void drawSpriteTile(byte[] buffer, Tile tile, int screenX, int screenY) {
        if (tile == null) {
            return;
        }
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = screenX + tx;
                int py = screenY + ty;
                if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                    continue;
                }
                int colorIndex = tile.getPixel(tx, ty);
                if (colorIndex == 0) {
                    continue;
                }
                int color = BLUE_OBJECTS_SPRITE_PALETTE[colorIndex];
                int offset = (py * Framebuffer.WIDTH + px) * 4;
                buffer[offset] = (byte) ((color >> 16) & 0xFF);
                buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
                buffer[offset + 2] = (byte) (color & 0xFF);
                buffer[offset + 3] = (byte) 0xFF;
            }
        }
    }
}
