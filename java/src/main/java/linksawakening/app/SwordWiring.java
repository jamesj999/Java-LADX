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
                                int romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                if (c.playerState().equipmentLevels().swordLevel() != 2
                                        || !c.playerState().vitals().fullHearts()
                                        || c.roomSession().playerItems().activeProjectileCount() != 0) {
                                    return false;
                                }
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireSwordBeam(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                romDirection);
                                if (spawned) {
                                    c.link().equipment().startRomItemAttackStep();
                                }
                                return spawned;
                            }
                        }));
    }
}
