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
        menu.add(1, "A", p.inventory()::itemA, p.inventory()::setItemA, 0, 13, v -> ITEMS[v]);
        menu.add(1, "B", p.inventory()::itemB, p.inventory()::setItemB, 0, 13, v -> ITEMS[v]);
        for (int i = 0; i < PlayerState.SUBSCREEN_SLOT_COUNT; i++) {
            final int slot = i;
            menu.add(
                    1,
                    "Slot " + (i + 1),
                    () -> p.inventory().subscreenItem(slot),
                    v -> {
                        int[] items = new int[PlayerState.SUBSCREEN_SLOT_COUNT];
                        for (int j = 0; j < items.length; j++)
                            items[j] = p.inventory().subscreenItem(j);
                        items[slot] = v;
                        p.inventory().setSubscreenItems(items);
                    },
                    0,
                    13,
                    v -> ITEMS[v]);
        }
        menu.add(1, "Sword level", p.equipmentLevels()::swordLevel, p.equipmentLevels()::setSwordLevel, 0, 2);
        menu.add(1, "Shield level", p.equipmentLevels()::shieldLevel, p.equipmentLevels()::setShieldLevel, 0, 2);
        menu.add(
                1,
                "Bracelet level",
                p.equipmentLevels()::powerBraceletLevel,
                p.equipmentLevels()::setPowerBraceletLevel,
                0,
                2);
    }

    static void resources(DebugMenu menu, PlayerState p) {
        menu.add(
                2,
                "Rupees",
                p.wallet()::rupees,
                v -> {
                    p.playerResources().clearResourceBuffersForDebug();
                    p.wallet().setRupees(v);
                },
                0,
                999);
        menu.add(
                2,
                "Hearts",
                p.vitals()::maxHearts,
                v -> {
                    p.playerResources().clearResourceBuffersForDebug();
                    p.vitals().setMaxHearts(v);
                },
                1,
                14);
        menu.add(
                2,
                "HP (1/8 heart)",
                p.vitals()::health,
                v -> {
                    p.playerResources().clearResourceBuffersForDebug();
                    p.vitals().setHealth(v);
                },
                0,
                112);
        menu.add(2, "Heart pieces", p.progression()::heartPieces, p.progression()::setHeartPieces, 0, 3);
        menu.add(2, "Max arrows", p.ammunition()::maxArrows, p.ammunition()::setMaxArrows, 0, 99);
        menu.add(2, "Arrows", p.ammunition()::arrowCount, p.ammunition()::setArrowCount, 0, 99);
        menu.add(2, "Max bombs", p.ammunition()::maxBombs, p.ammunition()::setMaxBombs, 0, 99);
        menu.add(2, "Bombs", p.ammunition()::bombCount, p.ammunition()::setBombCount, 0, 99);
        menu.add(2, "Max powder", p.ammunition()::maxMagicPowder, p.ammunition()::setMaxMagicPowder, 0, 99);
        menu.add(2, "Powder", p.ammunition()::magicPowderCount, p.ammunition()::setMagicPowderCount, 0, 99);
        menu.add(2, "Seashells", p.progression()::seashells, p.progression()::setSeashells, 0, 99);
    }

    static void questItems(DebugMenu menu, PlayerState p, RoomSession world) {
        menu.add(
                3,
                "Flippers",
                () -> p.progression().hasFlippers() ? 1 : 0,
                v -> p.progression().setHasFlippers(v != 0),
                0,
                1);
        menu.add(
                3,
                "Toadstool",
                () -> p.ammunition().hasToadstool() ? 1 : 0,
                v -> p.ammunition().setHasToadstool(v != 0),
                0,
                1);
        menu.add(
                3,
                "Medicine",
                p.progression()::medicineCount,
                v -> {
                    p.progression().setMedicineCount(v);
                    p.progression().setHasMedicine(v != 0);
                },
                0,
                255);
        menu.add(
                3,
                "Tail key",
                p.progression()::tailKeyCount,
                v -> {
                    p.progression().setTailKeyCount(v);
                    world.story().setTailKeyOwned(v != 0);
                },
                0,
                1);
        menu.add(3, "Angler key", p.progression()::anglerKeyCount, p.progression()::setAnglerKeyCount, 0, 1);
        menu.add(3, "Face key", p.progression()::faceKeyCount, p.progression()::setFaceKeyCount, 0, 1);
        menu.add(
                3,
                "Bird key",
                p.progression()::birdKeyCount,
                v -> {
                    p.progression().setBirdKeyCount(v);
                    world.story().setBirdKeyOwned(v != 0);
                },
                0,
                1);
    }

    static void questProgress(DebugMenu menu, PlayerState p, RoomSession world) {
        menu.add(3, "Leaves/key", p.progression()::goldenLeavesCount, p.progression()::setGoldenLeavesCount, 0, 6);
        menu.add(
                3,
                "Trade",
                p.progression()::tradeSequenceItem,
                p.progression()::setTradeSequenceItem,
                0,
                14,
                v -> v < TRADES.length ? TRADES[v] : Integer.toString(v));
        menu.add(3, "Song bits", p.progression()::ocarinaSongFlags, p.progression()::setOcarinaSongFlags, 0, 7);
        menu.add(3, "Tunic", p.equipmentLevels()::tunicType, p.equipmentLevels()::setTunicType, 0, 2);
        menu.add(3, "BowWow state", world.story()::bowWowState, world.story()::setBowWowState, 0, 255);
        menu.add(3, "Richard flag", world.story()::richardSpokenFlag, world.story()::setRichardSpokenFlag, 0, 255);
        menu.add(3, "Tarin flag", world.story()::tarinFlag, world.story()::setTarinFlag, 0, 255);
    }
}
