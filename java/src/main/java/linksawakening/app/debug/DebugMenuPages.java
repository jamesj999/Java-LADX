package linksawakening.app.debug;

import static org.lwjgl.glfw.GLFW.*;

import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomSession;

/** Entry definitions for player-editing pages, separate from keyboard navigation. */
final class DebugMenuPages {
    private DebugMenuPages() {}

    private static final String[] ITEMS = {
        "Empty",
        "Sword",
        "Bombs",
        "Bracelet",
        "Shield",
        "Bow",
        "Hookshot",
        "Magic rod",
        "Boots",
        "Ocarina",
        "Feather",
        "Shovel",
        "Powder",
        "Boomerang"
    };
    // TRADING_ITEM_* in LADX-Disassembly/src/constants/gameplay.asm.
    private static final String[] TRADES = {
        "None",
        "Yoshi doll",
        "Ribbon",
        "Dog food",
        "Bananas",
        "Stick",
        "Honeycomb",
        "Pineapple",
        "Hibiscus",
        "Letter",
        "Broom",
        "Fishing hook",
        "Necklace",
        "Scale",
        "Magnifying lens"
    };

    static void inventory(DebugMenu menu, PlayerState p) {
        menu.add(1, "A", p::itemA, p::setItemA, 0, 13, v -> ITEMS[v]);
        menu.add(1, "B", p::itemB, p::setItemB, 0, 13, v -> ITEMS[v]);
        for (int i = 0; i < PlayerState.SUBSCREEN_SLOT_COUNT; i++) {
            final int slot = i;
            menu.add(
                    1,
                    "Slot " + (i + 1),
                    () -> p.subscreenItem(slot),
                    v -> {
                        int[] items = new int[PlayerState.SUBSCREEN_SLOT_COUNT];
                        for (int j = 0; j < items.length; j++) items[j] = p.subscreenItem(j);
                        items[slot] = v;
                        p.setSubscreenItems(items);
                    },
                    0,
                    13,
                    v -> ITEMS[v]);
        }
        menu.add(1, "Sword level", p::swordLevel, p::setSwordLevel, 0, 2);
        menu.add(1, "Shield level", p::shieldLevel, p::setShieldLevel, 0, 2);
        menu.add(1, "Bracelet level", p::powerBraceletLevel, p::setPowerBraceletLevel, 0, 2);
    }

    static void resources(DebugMenu menu, PlayerState p) {
        menu.add(
                2,
                "Rupees",
                p::rupees,
                v -> {
                    p.clearResourceBuffersForDebug();
                    p.setRupees(v);
                },
                0,
                999);
        menu.add(
                2,
                "Hearts",
                p::maxHearts,
                v -> {
                    p.clearResourceBuffersForDebug();
                    p.setMaxHearts(v);
                },
                1,
                14);
        menu.add(
                2,
                "HP (1/8 heart)",
                p::health,
                v -> {
                    p.clearResourceBuffersForDebug();
                    p.setHealth(v);
                },
                0,
                112);
        menu.add(2, "Heart pieces", p::heartPieces, p::setHeartPieces, 0, 3);
        menu.add(2, "Max arrows", p::maxArrows, p::setMaxArrows, 0, 99);
        menu.add(2, "Arrows", p::arrowCount, p::setArrowCount, 0, 99);
        menu.add(2, "Max bombs", p::maxBombs, p::setMaxBombs, 0, 99);
        menu.add(2, "Bombs", p::bombCount, p::setBombCount, 0, 99);
        menu.add(2, "Max powder", p::maxMagicPowder, p::setMaxMagicPowder, 0, 99);
        menu.add(2, "Powder", p::magicPowderCount, p::setMagicPowderCount, 0, 99);
        menu.add(2, "Seashells", p::seashells, p::setSeashells, 0, 99);
    }

    static void questItems(DebugMenu menu, PlayerState p, RoomSession world) {
        menu.add(3, "Flippers", () -> p.hasFlippers() ? 1 : 0, v -> p.setHasFlippers(v != 0), 0, 1);
        menu.add(3, "Toadstool", () -> p.hasToadstool() ? 1 : 0, v -> p.setHasToadstool(v != 0), 0, 1);
        menu.add(
                3,
                "Medicine",
                p::medicineCount,
                v -> {
                    p.setMedicineCount(v);
                    p.setHasMedicine(v != 0);
                },
                0,
                255);
        menu.add(
                3,
                "Tail key",
                p::tailKeyCount,
                v -> {
                    p.setTailKeyCount(v);
                    world.story().setTailKeyOwned(v != 0);
                },
                0,
                1);
        menu.add(3, "Angler key", p::anglerKeyCount, p::setAnglerKeyCount, 0, 1);
        menu.add(3, "Face key", p::faceKeyCount, p::setFaceKeyCount, 0, 1);
        menu.add(
                3,
                "Bird key",
                p::birdKeyCount,
                v -> {
                    p.setBirdKeyCount(v);
                    world.story().setBirdKeyOwned(v != 0);
                },
                0,
                1);
    }

    static void questProgress(DebugMenu menu, PlayerState p, RoomSession world) {
        menu.add(3, "Leaves/key", p::goldenLeavesCount, p::setGoldenLeavesCount, 0, 6);
        menu.add(
                3,
                "Trade",
                p::tradeSequenceItem,
                p::setTradeSequenceItem,
                0,
                14,
                v -> v < TRADES.length ? TRADES[v] : Integer.toString(v));
        menu.add(3, "Song bits", p::ocarinaSongFlags, p::setOcarinaSongFlags, 0, 7);
        menu.add(3, "Tunic", p::tunicType, p::setTunicType, 0, 2);
        menu.add(3, "BowWow state", world.story()::bowWowState, world.story()::setBowWowState, 0, 255);
        menu.add(3, "Richard flag", world.story()::richardSpokenFlag, world.story()::setRichardSpokenFlag, 0, 255);
        menu.add(3, "Tarin flag", world.story()::tarinFlag, world.story()::setTarinFlag, 0, 255);
    }
}
