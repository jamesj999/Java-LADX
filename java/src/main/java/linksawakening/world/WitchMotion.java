package linksawakening.world;

/** State and timing port of bank-$05 {@code WitchEntityHandler}. */
final class WitchMotion {
    static final int ENTITY_TYPE = 0x40;
    private static final int INVENTORY_MAGIC_POWDER = 0x0C;
    private static final int NO_DIALOG = -1;
    private static final int NO_SOUND = -1;

    enum InventorySlot {
        NONE,
        A,
        B
    }

    record Input(boolean hasToadstool, int itemB, int itemA,
                 boolean actionButtonAHeld, boolean actionButtonBHeld,
                 boolean dialogActive, boolean linkAirborne,
                 int linkX, int linkY, int linkDirection,
                 int transitionSequenceCounter, int roomTriggerCount,
                 int bgPaletteEffectAddress, int paletteDataFlags,
                 int defaultMusicTrack) {
    }

    record Update(RoomEntity entity, int inertia, int transitionCountdown,
                  boolean blockLink, int dialogGlobalId, int musicTrack,
                  int jingleId, InventorySlot clearedSlot,
                  boolean exchangeStarted, boolean grantMagicPowder) {
    }

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] musicTrackTiming = new boolean[EntityRoomLoader.MAX_ENTITIES];

    Update advance(RoomEntity source, int transitionCountdown, Input input) {
        if (source.type() != ENTITY_TYPE) {
            throw new IllegalArgumentException("Entity is not the witch: " + source.type());
        }
        int slot = source.slot();
        int displayVariant = (inertia[slot] >>> 4) & 0x03;
        int nextInertia = (inertia[slot] + 1) & 0xFF;
        inertia[slot] = nextInertia;
        RoomEntity entity = withPresentation(source, 0x40, displayVariant);
        int nextCountdown = transitionCountdown & 0xFF;
        boolean blockLink = false;
        int dialog = NO_DIALOG;
        int music = NO_SOUND;
        int jingle = NO_SOUND;
        InventorySlot clearedSlot = InventorySlot.NONE;
        boolean exchangeStarted = false;
        boolean grantMagicPowder = false;

        switch (state[slot]) {
            case 0 -> {
                if (input.dialogActive()) {
                    break;
                }
                boolean nearbyAndFacing = !input.linkAirborne()
                    && nearbyAndFacing(entity, input);
                if (input.hasToadstool() && nearbyAndFacing) {
                    if (input.itemB() == INVENTORY_MAGIC_POWDER) {
                        if (!input.actionButtonBHeld()) {
                            break;
                        }
                        clearedSlot = InventorySlot.B;
                    } else if (input.itemA() == INVENTORY_MAGIC_POWDER) {
                        if (!input.actionButtonAHeld()) {
                            break;
                        }
                        clearedSlot = InventorySlot.A;
                    }
                    if (clearedSlot != InventorySlot.NONE) {
                        exchangeStarted = true;
                        nextCountdown = 0x08;
                        state[slot] = 1;
                        break;
                    }
                }
                if (nearbyAndFacing && input.actionButtonAHeld()) {
                    dialog = 0x00C;
                }
            }
            case 1 -> {
                if (input.transitionSequenceCounter() == 0x04) {
                    blockLink = true;
                    if (nextCountdown == 0) {
                        state[slot] = 2;
                    }
                }
            }
            case 2 -> {
                if (input.transitionSequenceCounter() == 0x04) {
                    dialog = 0x009;
                    nextCountdown = 0xC0;
                    state[slot] = 3;
                }
            }
            case 3 -> {
                if (input.dialogActive()) {
                    break;
                }
                if (!musicTrackTiming[slot]) {
                    music = input.defaultMusicTrack() & 0xFF;
                    musicTrackTiming[slot] = true;
                }
                blockLink = true;
                nextInertia = (nextInertia + 4) & 0xFF;
                inertia[slot] = nextInertia;
                if (nextCountdown == 0) {
                    musicTrackTiming[slot] = false;
                    music = input.defaultMusicTrack() & 0xFF;
                    dialog = 0x0FE;
                    state[slot] = 4;
                }
            }
            case 4 -> {
                if (!input.dialogActive()) {
                    grantMagicPowder = true;
                    jingle = 0x01;
                    state[slot] = 5;
                }
            }
            case 5 -> {
                if (input.roomTriggerCount() != 0
                    && input.bgPaletteEffectAddress() == 0
                    && input.paletteDataFlags() == 0) {
                    state[slot] = 6;
                }
            }
            case 6 -> {
                dialog = 0x17E;
                state[slot] = 7;
            }
            case 7 -> {
                // Terminal source state.
            }
            default -> throw new IllegalStateException("Invalid witch state: " + state[slot]);
        }

        return new Update(entity, nextInertia, nextCountdown, blockLink, dialog,
            music, jingle, clearedSlot, exchangeStarted, grantMagicPowder);
    }

    private static boolean nearbyAndFacing(RoomEntity entity, Input input) {
        int yWindow = (input.linkY() - entity.y() + 0x14) & 0xFF;
        if (yWindow >= 0x2B) {
            return false;
        }
        int xWindow = (input.linkX() - entity.x() + 0x10) & 0xFF;
        if (xWindow >= 0x20) {
            return false;
        }
        return directionToLink(entity, input.linkX(), input.linkY())
            == ((input.linkDirection() & 0x03) ^ 0x01);
    }

    private static int directionToLink(RoomEntity entity, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entity.x());
        int distanceY = signedByte(linkY - entity.y());
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static int signedByte(int value) {
        return (byte) value;
    }

    private static RoomEntity withPresentation(RoomEntity source, int y, int variant) {
        return new RoomEntity(source.slot(), source.sourceLoadOrder(), source.type(),
            source.x(), y, source.status(), source.spriteDefinition(), variant,
            source.entityFlipAttribute(), source.spriteTileOffset(), source.z());
    }

    int stateForTest(int slot) {
        return state[slot];
    }

    void setStateForTest(int slot, int value) {
        state[slot] = value;
    }

    void setInertiaForTest(int slot, int value) {
        inertia[slot] = value & 0xFF;
    }
}
