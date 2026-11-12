package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class EntitySpriteHandlerCatalogAdditionalTest extends EntitySpriteHandlerCatalogFixture {

    @Test
    void mapsTheGhiniFamilyToExactShippedRomDisplayLists() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        for (int entityType : new int[] {0x10, 0x12}) {
            EntitySpriteDefinition ghini = catalog.forEntityType(entityType, EntityRoomLoader.RoomTable.OVERWORLD);
            assertDefinition(ghini, 0x04, 0x5BFC, EntitySpriteDefinition.Shape.PAIR, 2, 0);
            assertPairBytes(ghini, new int[][] {
                {0x58, 0x02, 0x5A, 0x02},
                {0x5C, 0x02, 0x5E, 0x02}
            });
        }

        EntitySpriteDefinition giant = catalog.forEntityType(0x11, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(giant, 0x04, 0x5D26, EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        int[][][] expectedGiant = {
            {
                {-8, -8, 0x60, 0x02}, {-8, 0, 0x62, 0x02},
                {-8, 8, 0x62, 0x22}, {-8, 16, 0x60, 0x22},
                {8, -8, 0x64, 0x02}, {8, 0, 0x66, 0x02},
                {8, 8, 0x68, 0x02}, {8, 16, 0x6A, 0x02}
            },
            {
                {-8, -8, 0x60, 0x02}, {-8, 0, 0x62, 0x02},
                {-8, 8, 0x62, 0x22}, {-8, 16, 0x60, 0x22},
                {8, -8, 0x64, 0x02}, {8, 0, 0x6C, 0x02},
                {8, 8, 0x6E, 0x02}, {8, 16, 0x6A, 0x02}
            },
            {
                {-8, -8, 0x60, 0x02}, {-8, 0, 0x62, 0x02},
                {-8, 8, 0x62, 0x22}, {-8, 16, 0x60, 0x22},
                {8, -8, 0x6A, 0x22}, {8, 0, 0x68, 0x22},
                {8, 8, 0x66, 0x22}, {8, 16, 0x64, 0x22}
            },
            {
                {-8, -8, 0x60, 0x02}, {-8, 0, 0x62, 0x02},
                {-8, 8, 0x62, 0x22}, {-8, 16, 0x60, 0x22},
                {8, -8, 0x6A, 0x22}, {8, 0, 0x6E, 0x22},
                {8, 8, 0x6C, 0x22}, {8, 16, 0x64, 0x22}
            }
        };
        for (int variant = 0; variant < expectedGiant.length; variant++) {
            assertEquals(8, giant.rectangleVariant(variant).size());
            for (int spriteIndex = 0; spriteIndex < expectedGiant[variant].length; spriteIndex++) {
                int[] expected = expectedGiant[variant][spriteIndex];
                EntitySpriteDefinition.RectangleSprite actual =
                        giant.rectangleVariant(variant).get(spriteIndex);
                assertEquals(expected[0], actual.yOffset());
                assertEquals(expected[1], actual.xOffset());
                assertEquals(expected[2], actual.oam().tile());
                assertEquals(expected[3], actual.oam().attributes());
            }
        }
    }

    @Test
    void moblinSwordDynamicDefinitionMirrorsItsRomOamConstruction() throws Exception {
        EntitySpriteDefinition sword = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()))
                .forEntityType(0x14, EntityRoomLoader.RoomTable.OVERWORLD);

        assertDefinition(sword, 0x07, 0x7A95, EntitySpriteDefinition.Shape.DYNAMIC, 8, 0);
        assertEquals(4, sword.dynamicVariant(0).size());

        EntitySpriteDefinition.DynamicSprite warning = sword.dynamicVariant(0).get(0);
        assertEquals(5, warning.yOffset());
        assertEquals(-2, warning.xOffset());
        assertEquals(0x86, warning.oam().tile());
        assertEquals(0x16, warning.oam().attributes());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.GPU, warning.tileSource());
        assertFalse(warning.appliesEntityFlipAttribute());

        EntitySpriteDefinition.DynamicSprite swordTip = sword.dynamicVariant(0).get(1);
        assertEquals(8, swordTip.yOffset());
        assertEquals(8, swordTip.xOffset());
        assertEquals(0x04, swordTip.oam().tile());
        assertEquals(0x03, swordTip.oam().attributes());

        EntitySpriteDefinition.DynamicSprite body = sword.dynamicVariant(0).get(2);
        assertEquals(0, body.yOffset());
        assertEquals(0, body.xOffset());
        assertEquals(0x60, body.oam().tile());
        assertEquals(0x03, body.oam().attributes());
        assertEquals(EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, body.tileSource());
        assertTrue(body.appliesEntityFlipAttribute());

        assertEquals(3, sword.dynamicVariant(2).size());
        assertEquals(0x64, sword.dynamicVariant(2).get(0).oam().tile());
        assertEquals(-8, sword.dynamicVariant(2).get(2).yOffset());
        assertEquals(1, sword.dynamicVariant(2).get(2).xOffset());
        assertEquals(0x04, sword.dynamicVariant(2).get(2).oam().tile());
    }

    @Test
    void rejectsDisplayListsThatWouldReadOutsideTheSelectedRomBank() {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(syntheticRom()));

        assertThrows(IllegalArgumentException.class, () -> catalog.decodePair(0xD0, 0x06, 0x7FFF, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> catalog.decodeSingle(0xD1, 0x06, 0x4000, 0, 0));
    }

    @Test
    void shippedRomDisplayListsContainTheExpectedFirstVariants() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        EntitySpriteDefinition crow = catalog.forEntityType(0x7A, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x50, crow.variant(0).first().tile());
        assertEquals(0x03, crow.variant(0).first().attributes());
        assertEquals(0x52, crow.variant(2).first().tile());
        assertEquals(0x23, crow.variant(2).first().attributes());

        EntitySpriteDefinition butterfly = catalog.forEntityType(0x6E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x5E, butterfly.variant(0).first().tile());
        assertEquals(0x01, butterfly.variant(0).first().attributes());
        assertEquals(0x41, butterfly.variant(1).first().attributes());

        EntitySpriteDefinition pieceOfPower = catalog.forEntityType(0x33, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x14, pieceOfPower.variant(0).first().tile());
        assertEquals(0x02, pieceOfPower.variant(0).first().attributes());
        assertEquals(0x14, pieceOfPower.variant(1).first().tile());
        assertEquals(0x14, pieceOfPower.variant(1).first().attributes());

        EntitySpriteDefinition keese = catalog.forEntityType(0x19, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x42, keese.variant(0).first().tile());
        assertEquals(0x20, keese.variant(0).second().attributes());
        assertEquals(0x40, keese.variant(1).first().tile());

        EntitySpriteDefinition caveBKeese = catalog.forEntityType(0x19, EntityRoomLoader.RoomTable.INDOORS_A, 0x0A);
        assertEquals(0x62, caveBKeese.variant(0).first().tile());
        assertEquals(0x60, caveBKeese.variant(1).first().tile());

        EntitySpriteDefinition octorok = catalog.forEntityType(0x09, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x30, octorok.variant(0).first().tile());
        assertEquals(0x02, octorok.variant(0).first().attributes());
        assertEquals(0x32, octorok.variant(1).first().tile());
        assertEquals(0x34, octorok.variant(4).first().tile());

        EntitySpriteDefinition moblin = catalog.forEntityType(0x0B, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(8, moblin.variantCount());
        assertEquals(0x60, moblin.variant(0).first().tile());
        assertEquals(0x03, moblin.variant(0).first().attributes());
        assertEquals(0x6A, moblin.variant(6).first().tile());
        assertEquals(0x23, moblin.variant(6).first().attributes());

        EntitySpriteDefinition maskedIronMask = catalog.forIronMaskState(0);
        assertEquals(0x60, maskedIronMask.variant(0).first().tile());
        assertEquals(0x02, maskedIronMask.variant(0).first().attributes());
        assertEquals(0x6E, maskedIronMask.variant(7).first().tile());
        assertEquals(0x22, maskedIronMask.variant(7).first().attributes());

        EntitySpriteDefinition unmaskedIronMask = catalog.forIronMaskState(1);
        assertEquals(0x70, unmaskedIronMask.variant(0).first().tile());
        assertEquals(0x02, unmaskedIronMask.variant(0).first().attributes());
        assertEquals(0x70, unmaskedIronMask.variant(1).second().tile());
        assertEquals(0x22, unmaskedIronMask.variant(1).second().attributes());

        EntitySpriteDefinition armos = catalog.forEntityType(0x0F, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x60, armos.variant(0).first().tile());
        assertEquals(0x07, armos.variant(0).first().attributes());
        assertEquals(0x64, armos.variant(1).first().tile());

        EntitySpriteDefinition tektite = catalog.forEntityType(0x0D, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x58, tektite.variant(0).first().tile());
        assertEquals(0x02, tektite.variant(0).first().attributes());
        assertEquals(0x5A, tektite.variant(1).first().tile());
        assertEquals(0x22, tektite.variant(1).second().attributes());

        EntitySpriteDefinition waterTektite = catalog.forEntityType(0x99, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x70, waterTektite.variant(0).first().tile());
        assertEquals(0x00, waterTektite.variant(0).first().attributes());
        assertEquals(0x70, waterTektite.variant(0).second().tile());
        assertEquals(0x20, waterTektite.variant(0).second().attributes());
        assertEquals(0x72, waterTektite.variant(1).first().tile());
        assertEquals(0x00, waterTektite.variant(1).first().attributes());
        assertEquals(0x72, waterTektite.variant(1).second().tile());
        assertEquals(0x20, waterTektite.variant(1).second().attributes());

        EntitySpriteDefinition pairodd = catalog.forEntityType(0x57, EntityRoomLoader.RoomTable.INDOORS_A);
        int[] pairoddTiles = {
            0x70, 0x72, 0x72, 0x70, 0x74, 0x74, 0x00, 0x00, 0x7A, 0x7A, 0xFF, 0xFF, 0x76, 0x78, 0x78, 0x76
        };
        int[] pairoddAttributes = {
            0x01, 0x01, 0x21, 0x21, 0x01, 0x21, 0x00, 0x00, 0x01, 0x21, 0x00, 0x00, 0x01, 0x01, 0x21, 0x21
        };
        for (int variant = 0; variant < pairodd.variantCount(); variant++) {
            EntitySpriteDefinition.Variant pair = pairodd.variant(variant);
            assertEquals(pairoddTiles[variant * 2], pair.first().tile());
            assertEquals(pairoddAttributes[variant * 2], pair.first().attributes());
            assertEquals(pairoddTiles[variant * 2 + 1], pair.second().tile());
            assertEquals(pairoddAttributes[variant * 2 + 1], pair.second().attributes());
        }

        EntitySpriteDefinition pairoddProjectile = catalog.forEntityType(0x58, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x7C, pairoddProjectile.variant(0).first().tile());
        assertEquals(0x00, pairoddProjectile.variant(0).first().attributes());
        assertEquals(0x7C, pairoddProjectile.variant(0).second().tile());
        assertEquals(0x20, pairoddProjectile.variant(0).second().attributes());
        assertEquals(0x7E, pairoddProjectile.variant(1).first().tile());
        assertEquals(0x00, pairoddProjectile.variant(1).first().attributes());
        assertEquals(0x7E, pairoddProjectile.variant(1).second().tile());
        assertEquals(0x20, pairoddProjectile.variant(1).second().attributes());

        EntitySpriteDefinition star =
                catalog.forEntityType(EntitySpriteHandlerCatalog.ENTITY_STAR, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(star, 0x07, 0x7247, EntitySpriteDefinition.Shape.PAIR, 5, 0);
        assertPairBytes(star, new int[][] {
            {0x74, 0x01, 0x74, 0x21},
            {0x76, 0x01, 0x78, 0x01},
            {0x7A, 0x01, 0x7A, 0x21},
            {0x78, 0x21, 0x76, 0x21},
            {0x7C, 0x01, 0x7C, 0x01}
        });

        EntitySpriteDefinition leever = catalog.forEntityType(0x0E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x56, leever.variant(0).first().tile());
        assertEquals(0x02, leever.variant(0).first().attributes());
        assertEquals(0x54, leever.variant(1).first().tile());
        assertEquals(0x52, leever.variant(2).first().tile());
        assertEquals(0x50, leever.variant(3).first().tile());
        assertEquals(0x22, leever.variant(3).second().attributes());

        EntitySpriteDefinition stalfos = catalog.forEntityType(0x1A, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x4A, stalfos.variant(0).first().tile());
        assertEquals(0x00, stalfos.variant(0).first().attributes());
        assertEquals(0x4C, stalfos.variant(1).first().tile());
        assertEquals(0x20, stalfos.variant(1).first().attributes());
        assertEquals(0x4E, stalfos.variant(2).first().tile());
        assertEquals(0x20, stalfos.variant(2).second().attributes());

        EntitySpriteDefinition antiFairy = catalog.forEntityType(0x15, EntityRoomLoader.RoomTable.INDOORS_B);
        assertEquals(0x5A, antiFairy.variant(0).first().tile());
        assertEquals(0x00, antiFairy.variant(0).first().attributes());
        assertEquals(0x20, antiFairy.variant(0).second().attributes());
        assertEquals(0x14, antiFairy.variant(1).first().attributes());
        assertEquals(0x34, antiFairy.variant(1).second().attributes());

        EntitySpriteDefinition spark = catalog.forEntityType(0x16, EntityRoomLoader.RoomTable.INDOORS_B);
        assertEquals(0x5C, spark.variant(0).first().tile());
        assertEquals(0x00, spark.variant(0).first().attributes());
        assertEquals(0x20, spark.variant(0).second().attributes());
        assertEquals(0x14, spark.variant(1).first().attributes());
        assertEquals(0x34, spark.variant(1).second().attributes());

        EntitySpriteDefinition zol = catalog.forEntityType(0x1B, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x52, zol.variant(0).first().tile());
        assertEquals(0x02, zol.variant(0).first().attributes());
        assertEquals(0x54, zol.variant(1).first().tile());

        EntitySpriteDefinition slimeEyeZol = catalog.forZolSlimeEye();
        assertEquals(0x7C11, slimeEyeZol.address());
        assertEquals(0x00, slimeEyeZol.variant(0).first().attributes());
        assertEquals(0x20, slimeEyeZol.variant(0).second().attributes());

        EntitySpriteDefinition gel = catalog.forEntityType(0x1C, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x56, gel.variant(0).first().tile());
        assertEquals(0x02, gel.variant(0).first().attributes());
        assertEquals(0x22, gel.variant(1).first().attributes());

        EntitySpriteDefinition hidingZol = catalog.forEntityType(0x9B, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xFF, hidingZol.variant(0).first().tile());
        assertEquals(0x56, hidingZol.variant(1).first().tile());
        assertNull(hidingZol.variant(1).second());
        assertEquals(0x54, hidingZol.variant(2).first().tile());
        assertEquals(0x52, hidingZol.variant(3).first().tile());

        EntitySpriteDefinition gibdo = catalog.forEntityType(0x1F, EntityRoomLoader.RoomTable.INDOORS_B);
        assertEquals(0x74, gibdo.variant(0).first().tile());
        assertEquals(0x02, gibdo.variant(0).first().attributes());
        assertEquals(0x76, gibdo.variant(0).second().tile());
        assertEquals(0x22, gibdo.variant(1).second().attributes());

        EntitySpriteDefinition turtleRockGibdo =
                catalog.forEntityType(0x1F, EntityRoomLoader.RoomTable.INDOORS_B, 0x07);
        assertEquals(0x44, turtleRockGibdo.variant(0).first().tile());
        assertEquals(0x46, turtleRockGibdo.variant(0).second().tile());
        assertEquals(0x22, turtleRockGibdo.variant(1).first().attributes());

        EntitySpriteDefinition peaHat = catalog.forEntityType(0xA0, EntityRoomLoader.RoomTable.INDOORS_B);
        assertEquals(0x40, peaHat.variant(0).first().tile());
        assertEquals(0x02, peaHat.variant(0).first().attributes());
        assertEquals(0x42, peaHat.variant(1).first().tile());
        assertEquals(0x22, peaHat.variant(1).second().attributes());

        EntitySpriteDefinition ghini = catalog.forEntityType(0x12, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x58, ghini.variant(0).first().tile());
        assertEquals(0x02, ghini.variant(0).first().attributes());
        assertEquals(0x5C, ghini.variant(1).first().tile());

        EntitySpriteDefinition hardHat = catalog.forEntityType(0x20, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x44, hardHat.variant(0).first().tile());
        assertEquals(0x01, hardHat.variant(0).first().attributes());
        assertEquals(0x21, hardHat.variant(0).second().attributes());
        assertEquals(0x46, hardHat.variant(1).first().tile());

        EntitySpriteDefinition caveBHardHat = catalog.forEntityType(0x20, EntityRoomLoader.RoomTable.INDOORS_A, 0x0A);
        assertEquals(0x64, caveBHardHat.variant(0).first().tile());
        assertEquals(0x66, caveBHardHat.variant(1).first().tile());

        EntitySpriteDefinition spikeTrap = catalog.forEntityType(0x27, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(0x50, spikeTrap.variant(0).first().tile());
        assertEquals(0x02, spikeTrap.variant(0).first().attributes());
        assertEquals(0x50, spikeTrap.variant(0).second().tile());
        assertEquals(0x22, spikeTrap.variant(0).second().attributes());

        EntitySpriteDefinition grandpa = catalog.forEntityType(0x77, EntityRoomLoader.RoomTable.INDOORS_A);
        assertEquals(-9, grandpa.rectangleVariant(0).get(0).yOffset());
        assertEquals(0, grandpa.rectangleVariant(0).get(0).xOffset());
        assertEquals(0x70, grandpa.rectangleVariant(0).get(0).oam().tile());
        assertEquals(0x78, grandpa.rectangleVariant(1).get(0).oam().tile());

        EntitySpriteDefinition guardianAcorn = catalog.forEntityType(0x34, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xAE, guardianAcorn.variant(0).first().tile());
        assertEquals(0x14, guardianAcorn.variant(0).first().attributes());

        EntitySpriteDefinition heartPiece = catalog.forEntityType(0x35, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xAC, heartPiece.variant(0).first().tile());
        assertEquals(0x02, heartPiece.variant(0).first().attributes());
        assertEquals(0x22, heartPiece.variant(0).second().attributes());

        EntitySpriteDefinition heartContainer = catalog.forEntityType(0x36, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xAA, heartContainer.variant(0).first().tile());
        assertEquals(0x14, heartContainer.variant(0).first().attributes());
        assertEquals(0x34, heartContainer.variant(0).second().attributes());

        EntitySpriteDefinition droppableHeart = catalog.forEntityType(0x2D, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xA8, droppableHeart.variant(0).first().tile());
        assertEquals(0x14, droppableHeart.variant(0).first().attributes());

        EntitySpriteDefinition droppableRupee = catalog.forEntityType(0x2E, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xA6, droppableRupee.variant(0).first().tile());
        assertEquals(0x15, droppableRupee.variant(0).first().attributes());

        EntitySpriteDefinition ironMask = catalog.forEntityType(0x32, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x74, ironMask.variant(0).first().tile());
        assertEquals(0x00, ironMask.variant(0).first().attributes());
        assertEquals(0x76, ironMask.variant(0).second().tile());
        assertEquals(0x20, ironMask.variant(1).first().attributes());

        EntitySpriteDefinition arrows = catalog.forEntityType(0x37, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x2A, arrows.variant(0).first().tile());
        assertEquals(0x41, arrows.variant(0).first().attributes());
        assertEquals(0x61, arrows.variant(0).second().attributes());

        EntitySpriteDefinition bombs = catalog.forEntityType(0x38, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x80, bombs.variant(0).first().tile());
        assertEquals(0x15, bombs.variant(0).first().attributes());

        EntitySpriteDefinition toadstool = catalog.forEntityType(0x3A, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x5E, toadstool.variant(0).first().tile());
        assertEquals(0x02, toadstool.variant(0).first().attributes());
        assertEquals(0x22, toadstool.variant(0).second().attributes());

        EntitySpriteDefinition powder = catalog.forEntityType(0x3B, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x8E, powder.variant(0).first().tile());
        assertEquals(0x16, powder.variant(0).first().attributes());

        EntitySpriteDefinition slimeKey = catalog.forEntityType(0x3C, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xCA, slimeKey.variant(0).first().tile());
        assertEquals(0x14, slimeKey.variant(0).first().attributes());

        EntitySpriteDefinition seashell = catalog.forEntityType(0x3D, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0x9E, seashell.variant(0).first().tile());
        assertEquals(0x14, seashell.variant(0).first().attributes());

        EntitySpriteDefinition bowWowFollowing = catalog.forFollowerEntityType(0x6D);
        assertEquals(0x40, bowWowFollowing.variant(0).first().tile());
        assertEquals(0x40, bowWowFollowing.variant(0).second().tile());
        EntitySpriteDefinition marinFollowing = catalog.forFollowerEntityType(0xC1);
        assertEquals(0x42, marinFollowing.variant(0).first().tile());
        assertEquals(0x40, marinFollowing.variant(0).second().tile());
        EntitySpriteDefinition ghostFollowing = catalog.forFollowerEntityType(0xD4);
        assertEquals(0x42, ghostFollowing.variant(0).first().tile());
        EntitySpriteDefinition roosterFollowing = catalog.forFollowerEntityType(0xD5);
        assertEquals(0x42, roosterFollowing.variant(0).first().tile());
    }

    @Test
    void mapsColorShellStatesToTheSixBankTwentyRectangleFamilies() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));
        int[] types = {0xE9, 0xEA, 0xEB};
        int[] activeAddresses = {0x6688, 0x66E8, 0x6748};
        int[] inactiveAddresses = {0x67A8, 0x67D8, 0x6808};

        for (int index = 0; index < types.length; index++) {
            EntitySpriteDefinition active = catalog.forColorShellState(types[index], 0x00, EntityStatus.ACTIVE);
            assertDefinition(active, 0x20, activeAddresses[index], EntitySpriteDefinition.Shape.RECTANGLE, 8, 0);
            assertEquals(3, active.rectangleVariant(0).size());

            EntitySpriteDefinition inactive = catalog.forColorShellState(types[index], 0x06, EntityStatus.ACTIVE);
            assertDefinition(inactive, 0x20, inactiveAddresses[index], EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
            assertEquals(3, inactive.rectangleVariant(0).size());

            EntitySpriteDefinition stunned = catalog.forColorShellState(types[index], 0x00, EntityStatus.STUNNED);
            assertEquals(inactive.address(), stunned.address());
        }
    }

    @Test
    void colorShellRectangleBytesPreserveRomOffsetsAttributesAndHiddenOam() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        EntitySpriteDefinition active = catalog.forColorShellState(0xE9, 0x00, EntityStatus.ACTIVE);
        EntitySpriteDefinition.RectangleSprite first =
                active.rectangleVariant(0).get(0);
        assertEquals(0, first.yOffset());
        assertEquals(8, first.xOffset());
        assertEquals(0x48, first.oam().tile());
        assertEquals(0x02, first.oam().attributes());

        EntitySpriteDefinition.RectangleSprite hidden =
                active.rectangleVariant(1).get(2);
        assertEquals(0xFF, hidden.oam().tile());
        assertEquals(0xFF, hidden.oam().attributes());
    }

    @Test
    void decodesTheRomDeathRectangleFamiliesAndPowerFinalFrame() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        EntitySpriteDefinition death = catalog.forDeathEntity();
        assertDefinition(death, 0x03, 0x5488, EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        EntitySpriteDefinition.RectangleSprite signed =
                death.rectangleVariant(3).get(0);
        assertEquals(4, signed.yOffset());
        assertEquals(-4, signed.xOffset());
        assertEquals(0x30, signed.oam().tile());
        assertEquals(0x01, signed.oam().attributes());

        EntitySpriteDefinition.RectangleSprite hidden =
                death.rectangleVariant(0).get(2);
        assertEquals(0xFF, hidden.oam().tile());
        assertEquals(0xFF, hidden.oam().attributes());

        EntitySpriteDefinition power = catalog.forPowerRecoilDeathEntity();
        assertDefinition(power, 0x03, 0x54C8, EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
        assertEquals(8, power.rectangleVariant(3).size());
        EntitySpriteDefinition.RectangleSprite fifth = power.rectangleVariant(3).get(4);
        assertEquals(0x10, fifth.oam().tile());
        assertEquals(0x42, fifth.oam().attributes());
    }

    @Test
    void decodesFloatingItemsMixedMainListSourceQuirkAndRectangleOverlay() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        EntitySpriteDefinition floating = catalog.forEntityType(0x86, EntityRoomLoader.RoomTable.OVERWORLD);
        assertDefinition(floating, 0x06, 0x7ADD, EntitySpriteDefinition.Shape.PAIR, 7, 0);
        int[][] singles = {
            {0xA6, 0x15}, {0x8E, 0x16}, {0x80, 0x15}, {0xA6, 0x15}, {0xA9, 0x14}, {0x2A, 0x41}, {0x2A, 0x61}
        };
        for (int variant = 0; variant < singles.length; variant++) {
            assertEquals(singles[variant][0], floating.variant(variant).first().tile());
            assertEquals(singles[variant][1], floating.variant(variant).first().attributes());
            assertNull(floating.variant(variant).second());
        }

        EntitySpriteDefinition second = catalog.forEntityType(0xE5, EntityRoomLoader.RoomTable.OVERWORLD);
        assertEquals(0xA9, second.variant(4).first().tile());
        assertEquals(0x14, second.variant(4).first().attributes());
        assertEquals(0x0C, second.variant(5).first().tile());
        assertEquals(0xFE, second.variant(5).first().attributes());
        assertEquals(0x01, second.variant(5).second().tile());
        assertEquals(0xC0, second.variant(5).second().attributes());

        EntitySpriteDefinition overlay = catalog.forFloatingItemOverlay();
        assertDefinition(overlay, 0x06, 0x7AEB, EntitySpriteDefinition.Shape.RECTANGLE, 2, 0);
        assertEquals(0, overlay.rectangleVariant(0).get(0).yOffset());
        assertEquals(-4, overlay.rectangleVariant(0).get(0).xOffset());
        assertEquals(0x22, overlay.rectangleVariant(0).get(0).oam().tile());
        assertEquals(0x00, overlay.rectangleVariant(0).get(0).oam().attributes());
        assertEquals(12, overlay.rectangleVariant(1).get(1).xOffset());
        assertEquals(0x60, overlay.rectangleVariant(1).get(1).oam().attributes());
    }

    @Test
    void evasiveStalfosVariantsMatchBothRomDisplayListFamilies() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        assertPairBytes(catalog.forEntityType(0x1E, EntityRoomLoader.RoomTable.INDOORS_A), new int[][] {
            {0x4A, 0x01, 0x4C, 0x01},
            {0x4C, 0x21, 0x4A, 0x21},
            {0x4E, 0x01, 0x4E, 0x21}
        });
        assertPairBytes(catalog.forStalfosEvasiveState(1), new int[][] {
            {0x48, 0x01, 0x48, 0x61},
            {0x48, 0x41, 0x48, 0x21}
        });
    }

    @Test
    void mapsLaserParentToTheRomRotationDisplayListAndLeavesBeamInvisible() throws Exception {
        EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(new linksawakening.rom.Rom(loadRom()));

        EntitySpriteDefinition laser = catalog.forEntityType(0x2A, EntityRoomLoader.RoomTable.INDOORS_A);
        assertDefinition(laser, 0x04, 0x6C2D, EntitySpriteDefinition.Shape.PAIR, 8, 0);
        assertPairBytes(laser, new int[][] {
            {0x70, 0x03, 0x70, 0x23},
            {0x78, 0x03, 0x7A, 0x03},
            {0x74, 0x03, 0x76, 0x03},
            {0x7C, 0x03, 0x7E, 0x03},
            {0x72, 0x03, 0x72, 0x23},
            {0x7E, 0x23, 0x7C, 0x23},
            {0x76, 0x23, 0x74, 0x23},
            {0x7A, 0x23, 0x78, 0x23}
        });

        EntitySpriteDefinition beam = catalog.forEntityType(0x2B, EntityRoomLoader.RoomTable.INDOORS_A);
        assertFalse(beam.supported());
    }
}
