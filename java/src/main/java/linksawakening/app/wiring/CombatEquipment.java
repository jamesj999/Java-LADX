package linksawakening.app.wiring;

import linksawakening.app.wiring.EquipmentWiring.EquipmentInputs;
import linksawakening.equipment.Arrow;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.MagicPowder;
import linksawakening.equipment.MagicRod;
import linksawakening.equipment.Sword;
import linksawakening.state.InventoryItem;
import linksawakening.state.PowerUp;
import linksawakening.world.room.ArrowShotResult;

/** Registers combat items and their room targets. Constructed only during session wiring. */
final class CombatEquipment {
    private CombatEquipment() {}

    static void sword(EquipmentInputs c, ItemRegistry registry) {
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

    static void magicRod(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.MAGIC_ROD.romValue(),
                new MagicRod(
                        c.romTables(),
                        c.graphics().linkSpriteSheet(),
                        c.graphics().linkTunicPalette(),
                        c.gameplaySoundSink(),
                        new MagicRod.LaunchTarget() {
                            @Override
                            public void startMagicRodAttackStep() {

                                c.link().equipment().startRomMagicRodAttackStep();
                            }

                            @Override
                            public boolean fireMagicRodFireball() {
                                int romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                boolean spawned = c.roomSession()
                                        .playerItems()
                                        .fireMagicRodFireball(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                romDirection,
                                                c.playerState()
                                                                .equipmentLevels()
                                                                .activePowerUp()
                                                        == PowerUp.PIECE_OF_POWER.romValue());
                                return spawned;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return c.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }

    static void magicPowder(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.MAGIC_POWDER.romValue(),
                new MagicPowder(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new MagicPowder.SprinkleTarget() {
                            @Override
                            public boolean sprinkleMagicPowder() {
                                int romDirection = c.link().equipment().applyRomItemDirectionFromInput();
                                return c.roomSession()
                                        .playerItems()
                                        .sprinkleMagicPowder(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                romDirection);
                            }

                            @Override
                            public void startMagicPowderAttackStep() {

                                c.link().equipment().startRomMagicPowderAttackStep();
                            }

                            @Override
                            public boolean magicPowderAttackStepActive() {
                                return c.link().equipment().romAttackStepAnimationCountdown() != 0;
                            }

                            @Override
                            public int linkPositionZ() {
                                return c.link().animation().romEntityZ();
                            }

                            @Override
                            public void presentToadstool() {
                                c.link().animation().presentToadstool();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }

    static void arrow(EquipmentInputs c, ItemRegistry registry) {
        registry.register(
                InventoryItem.BOW.romValue(),
                new Arrow(
                        c.playerState(),
                        c.gameplaySoundSink(),
                        new Arrow.ShootTarget() {
                            private boolean playWhooshForLastShot;

                            @Override
                            public boolean shootArrow() {
                                ArrowShotResult result = c.roomSession()
                                        .playerItems()
                                        .shootArrowResult(
                                                c.link().position().romEntityX(),
                                                c.link().position().romEntityY(),
                                                c.link().animation().romEntityZ(),
                                                c.link().equipment().applyRomItemDirectionFromInput(),
                                                c.playerState()
                                                                .equipmentLevels()
                                                                .activePowerUp()
                                                        == PowerUp.PIECE_OF_POWER.romValue());
                                playWhooshForLastShot = result.playWhoosh();
                                if (result.spawned()) {
                                    c.link().equipment().startRomItemAttackStep();
                                }
                                return result.spawned();
                            }

                            @Override
                            public boolean playWhooshForLastShot() {
                                return playWhooshForLastShot;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return c.roomSession().playerItems().activeProjectileCount();
                            }
                        },
                        c.link().equipment()::canUseItems));
    }
}
