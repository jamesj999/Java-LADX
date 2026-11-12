package linksawakening.world.entity.motion;

import linksawakening.world.entity.*;

/** Reused, reentrancy-safe scratch for WitchMotion.advance; values are assigned in source order. */
final class WitchStep {
    private WitchMotion owner;
    private linksawakening.world.entity.RoomEntity source;
    private int transitionCountdown;
    private linksawakening.world.entity.motion.WitchMotion.Input input;
    private int slot;
    private int displayVariant;
    private int nextInertia;
    private linksawakening.world.entity.RoomEntity entity;
    private int nextCountdown;
    private boolean blockLink;
    private int motionBlockValue;
    private int dialog;
    private int music;
    private int jingle;
    private linksawakening.world.entity.motion.WitchMotion.InventorySlot clearedSlot;
    private boolean exchangeStarted;
    private boolean grantMagicPowder;

    WitchStep() {}

    linksawakening.world.entity.motion.WitchMotion.Update run() {
        if (source.type() != WitchMotion.ENTITY_TYPE) {
            throw new IllegalArgumentException("Entity is not the witch: " + source.type());
        }
        slot = source.slot();
        displayVariant = (owner.inertia[slot] >>> 4) & 0x03;
        nextInertia = (owner.inertia[slot] + 1) & 0xFF;
        owner.inertia[slot] = nextInertia;
        entity = WitchMotion.withPresentation(source, 0x40, displayVariant);
        nextCountdown = transitionCountdown & 0xFF;
        blockLink = false;
        motionBlockValue = 0;
        dialog = WitchMotion.NO_DIALOG;
        music = WitchMotion.NO_SOUND;
        jingle = WitchMotion.NO_SOUND;
        clearedSlot = WitchMotion.InventorySlot.NONE;
        exchangeStarted = false;
        grantMagicPowder = false;

        switch (owner.state[slot]) {
            case 0 -> {
                if (!advanceState0()) return completedResult;
            }
            case 1 -> {
                if (!advanceState1()) return completedResult;
            }
            case 2 -> {
                if (!advanceState2()) return completedResult;
            }
            case 3 -> {
                if (!advanceState3()) return completedResult;
            }
            case 4 -> {
                if (!advanceState4()) return completedResult;
            }
            case 5 -> {
                if (!advanceState5()) return completedResult;
            }
            case 6 -> {
                if (!advanceState6()) return completedResult;
            }
            case 7 -> {
                if (!advanceState7()) return completedResult;
            }
            default -> throw new IllegalStateException("Invalid witch state: " + owner.state[slot]);
        }

        return new WitchMotion.Update(
                entity,
                nextInertia,
                nextCountdown,
                blockLink,
                dialog,
                music,
                jingle,
                clearedSlot,
                exchangeStarted,
                grantMagicPowder,
                motionBlockValue);
    }

    private linksawakening.world.entity.motion.WitchMotion.Update completedResult;

    private boolean advanceState0() {
        if (input.dialogActive()) {
            return true;
        }
        boolean nearbyAndFacing = !input.linkAirborne() && WitchMotion.nearbyAndFacing(entity, input);
        if (input.hasToadstool() && nearbyAndFacing) {
            if (input.itemB() == WitchMotion.INVENTORY_MAGIC_POWDER) {
                if (!input.actionButtonBHeld()) {
                    return true;
                }
                clearedSlot = WitchMotion.InventorySlot.B;
            } else if (input.itemA() == WitchMotion.INVENTORY_MAGIC_POWDER) {
                if (!input.actionButtonAHeld()) {
                    return true;
                }
                clearedSlot = WitchMotion.InventorySlot.A;
            }
            if (clearedSlot != WitchMotion.InventorySlot.NONE) {
                exchangeStarted = true;
                nextCountdown = 0x08;
                owner.state[slot] = 1;
                return true;
            }
        }
        if (nearbyAndFacing && input.actionButtonAHeld()) {
            dialog = 0x00C;
        }

        return true;
    }

    private boolean advanceState1() {
        if (input.transitionSequenceCounter() == 0x04) {
            blockLink = true;
            motionBlockValue = 2;
            if (nextCountdown == 0) {
                owner.state[slot] = 2;
            }
        }

        return true;
    }

    private boolean advanceState2() {
        if (input.transitionSequenceCounter() == 0x04) {
            dialog = 0x009;
            nextCountdown = 0xC0;
            owner.state[slot] = 3;
        }

        return true;
    }

    private boolean advanceState3() {
        if (input.dialogActive()) {
            return true;
        }
        if (!owner.musicTrackTiming[slot]) {
            music = input.defaultMusicTrack() & 0xFF;
            owner.musicTrackTiming[slot] = true;
        }
        blockLink = true;
        motionBlockValue = 1;
        nextInertia = (nextInertia + 4) & 0xFF;
        owner.inertia[slot] = nextInertia;
        if (nextCountdown == 0) {
            owner.musicTrackTiming[slot] = false;
            music = input.defaultMusicTrack() & 0xFF;
            dialog = 0x0FE;
            owner.state[slot] = 4;
        }

        return true;
    }

    private boolean advanceState4() {
        if (!input.dialogActive()) {
            grantMagicPowder = true;
            jingle = 0x01;
            owner.state[slot] = 5;
        }

        return true;
    }

    private boolean advanceState5() {
        if (input.roomTriggerCount() != 0 && input.bgPaletteEffectAddress() == 0 && input.paletteDataFlags() == 0) {
            owner.state[slot] = 6;
        }

        return true;
    }

    private boolean advanceState6() {
        dialog = 0x17E;
        owner.state[slot] = 7;

        return true;
    }

    private boolean advanceState7() {
        // Terminal source state.

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.WitchMotion.Update run(
            WitchMotion owner,
            linksawakening.world.entity.RoomEntity source,
            int transitionCountdown,
            linksawakening.world.entity.motion.WitchMotion.Input input) {
        if (inUse) {
            return new WitchStep().run(owner, source, transitionCountdown, input);
        }
        inUse = true;
        slot = 0;
        displayVariant = 0;
        nextInertia = 0;
        entity = null;
        nextCountdown = 0;
        blockLink = false;
        motionBlockValue = 0;
        dialog = 0;
        music = 0;
        jingle = 0;
        clearedSlot = null;
        exchangeStarted = false;
        grantMagicPowder = false;
        completedResult = null;
        this.owner = owner;
        this.source = source;
        this.transitionCountdown = transitionCountdown;
        this.input = input;
        try {
            return run();
        } finally {
            inUse = false;
            this.owner = null;
            this.source = null;
            this.input = null;
            this.entity = null;
            this.clearedSlot = null;
            this.completedResult = null;
        }
    }
}
