package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.state.PlayerState;

/** Registers the sword and its room-facing adapter. */
final class SwordWiring {
    private SwordWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                PlayerState.INVENTORY_SWORD,
                new Sword(
                        c.romTables(),
                        c.graphics().swordSpriteSheet(),
                        c.gameplaySoundSink(),
                        c.random(),
                        c.graphics().swordPalette(),
                        new Sword.BeamTarget() {
                            @Override
                            public boolean fireSwordBeam() {
                                if (c.link() == null || c.roomSession() == null || c.playerState() == null) {
                                    return false;
                                }
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
