package linksawakening.app;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.state.InventoryItem;

/** Registers the sword and its room-facing adapter. */
final class SwordWiring {
    private SwordWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.SWORD.romValue(),
                new Sword(
                        c.romTables(),
                        c.graphics().swordSpriteSheet(),
                        c.gameplaySoundSink(),
                        c.random(),
                        c.graphics().swordPalette(),
                        new Sword.BeamTarget() {
                            @Override
                            public boolean fireSwordBeam() {
                                int romDirection = c.link().applyRomItemDirectionFromInput();
                                if (c.playerState().swordLevel() != 2
                                        || !c.playerState().fullHearts()
                                        || c.roomSession().playerItems().activeProjectileCount() != 0) {
                                    return false;
                                }
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireSwordBeam(
                                                c.link().romEntityX(),
                                                c.link().romEntityY(),
                                                c.link().romEntityZ(),
                                                romDirection);
                                if (spawned) {
                                    c.link().startRomItemAttackStep();
                                }
                                return spawned;
                            }
                        }));
    }
}
