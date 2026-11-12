package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.MagicRod;
import linksawakening.state.PlayerState;

/** Registers the magicrod and its room-facing adapter. */
final class MagicRodWiring {
    private MagicRodWiring() {}

    static void register(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                PlayerState.INVENTORY_MAGIC_ROD,
                new MagicRod(
                        c.romTables(),
                        c.graphics().linkSpriteSheet(),
                        c.graphics().linkTunicPalette(),
                        c.gameplaySoundSink(),
                        new MagicRod.LaunchTarget() {
                            @Override
                            public void startMagicRodAttackStep() {
                                if (c.link() != null) {
                                    c.link().startRomMagicRodAttackStep();
                                }
                            }

                            @Override
                            public boolean fireMagicRodFireball() {
                                if (c.link() == null || c.roomSession() == null || c.playerState() == null) {
                                    return false;
                                }
                                int romDirection = c.link().applyRomItemDirectionFromInput();
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireMagicRodFireball(
                                                c.link().romEntityX(),
                                                c.link().romEntityY(),
                                                c.link().romEntityZ(),
                                                romDirection,
                                                c.playerState().activePowerUp()
                                                        == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
                                return spawned;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return c.roomSession() == null
                                        ? 0
                                        : c.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        c.link()::canUseItems));
    }
}
