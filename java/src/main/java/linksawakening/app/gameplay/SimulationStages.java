package linksawakening.app.gameplay;

import linksawakening.equipment.Sword;

/** Ordered simulation with early exits belonging to the source frame lifecycle. */
interface SimulationStages {
    boolean dialogStory();

    boolean dialogBlocks();

    void linkEquipment(boolean dialogBlocks, boolean inventoryBlocked);

    boolean passingOut();

    Sword.CollisionBox combat(boolean dialogBlocks);

    void entities(Sword.CollisionBox sword);

    void completeInput();

    void animatedTiles(boolean dialogBlocks);

    default void run(boolean inventoryBlocked) {
        if (!dialogStory()) return;
        boolean blocked = dialogBlocks();
        linkEquipment(blocked, inventoryBlocked);
        if (passingOut()) return;
        var sword = combat(blocked);
        entities(sword);
        completeInput();
        animatedTiles(blocked);
    }
}
