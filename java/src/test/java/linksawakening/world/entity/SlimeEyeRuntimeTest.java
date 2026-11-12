package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.RomTables;
import linksawakening.testing.TestRom;
import linksawakening.world.room.ChestContentsTable;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class SlimeEyeRuntimeTest {
    private int frame;

    private byte[] rom() throws Exception {
        try (var input = TestRom.open()) {
            return input.readAllBytes();
        }
    }

    private RoomEntityRuntime runtime() throws Exception {
        byte[] rom = rom();
        var sprites = new EntitySpriteHandlerCatalog(rom);
        var list = new ArrayList<RoomEntity>();
        for (int s = 0; s < 16; s++) list.add(RoomEntity.disabled(s));
        list.set(
                0,
                new RoomEntity(
                        0,
                        0,
                        0x5B,
                        0x50,
                        0x48,
                        EntityStatus.INIT,
                        sprites.forEntityType(0x5B, EntityRoomLoader.RoomTable.INDOORS_A),
                        0,
                        0,
                        0,
                        0,
                        -1,
                        false));
        var runtime = RoomEntityRuntime.from(
                new RoomEntitySnapshot(list),
                true,
                () -> 0,
                sprites,
                new RomEnemyCombatTables(rom),
                new ChestContentsTable(rom),
                RomTables.loadFromRom(rom),
                rom);
        runtime.configuration().setEntityMapId(2);
        return runtime;
    }

    private void tick(RoomEntityRuntime r) {
        r.tick(EntityTestFrames.frame(r, frame++ & 255, 0x50, 0x60, () -> 0));
    }

    private List<RoomEntity> eyes(RoomEntityRuntime r) {
        return r.inspection().snapshot().slots().stream()
                .filter(e -> e.loaded() && e.type() == 0x5B)
                .toList();
    }

    private void sword(RoomEntityRuntime r, RoomEntity e, boolean dash) {
        r.resolveCombat(
                frame & 255,
                0x50,
                0x60,
                false,
                true,
                true,
                e.x() + 8,
                4,
                e.y() - e.z() + 8,
                4,
                new EnemyAttackContext(1, false, false, false, dash));
    }

    private void drop(RoomEntityRuntime r) {
        tick(r);
        assertEquals(0x7E, eyes(r).getFirst().z());
        r.configuration().setScreenShakeCountdown(4);
        tick(r);
        assertEquals(0, r.slimeEye.state(0));
        r.configuration().setScreenShakeCountdown(5);
        tick(r);
        assertEquals(1, r.slimeEye.state(0));
        r.configuration().setScreenShakeCountdown(0);
        for (int i = 0; i < 200 && r.slimeEye.state(0) != 3; i++) tick(r);
        assertEquals(3, r.slimeEye.state(0));
        assertEquals(0, eyes(r).getFirst().z());
    }

    @Test
    void connectedEncounterSpawnsZolsAndDropsOnlyOnFive() throws Exception {
        var r = runtime();
        tick(r);
        for (int i = 0; i < 160; i++) tick(r);
        assertTrue(r.inspection().snapshot().slots().stream().anyMatch(e -> e.loaded() && e.type() == 0x1B));
        assertEquals(0, r.slimeEye.state(0));
        r.configuration().setScreenShakeCountdown(5);
        tick(r);
        assertEquals(1, r.slimeEye.state(0));
        assertTrue(r.consumePendingEntityEvents().stream().anyMatch(e -> e.soundId() == 8));
    }

    @Test
    void productionRegistryRunsBodyStretchSplitBothHalvesAndSingleReward() throws Exception {
        var r = runtime();
        drop(r);
        // Keep sword collision active as the ROM player can; collision timers govern accepted hits.
        for (int i = 0; i < 500 && r.slimeEye.stretch(0) < 4; i++) {
            sword(r, eyes(r).getFirst(), false);
            tick(r);
        }
        assertEquals(4, r.slimeEye.stretch(0));
        r.consumePendingLinkMotionBlockRequests();
        var splitBlockValues = new ArrayList<Integer>();
        for (int i = 0; i < 30 && eyes(r).size() == 1; i++) {
            sword(r, eyes(r).getFirst(), true);
            tick(r);
            r.consumePendingLinkMotionBlockRequests().forEach(request -> splitBlockValues.add(request.value()));
        }
        assertEquals(
                List.of(11, 10, 9, 8, 7, 6, 5, 4, 3, 2),
                splitBlockValues,
                "04:4A94 writes the live split countdown, not a Boolean 2");
        assertEquals(2, eyes(r).size());
        assertTrue(
                eyes(r).stream().allMatch(e -> r.slimeEye.half(e.slot()) && r.entityState.enemyHealth[e.slot()] == 5));
        assertFalse(r.inspection().snapshot().slots().getFirst().loaded());
        int hits = 0;
        for (int i = 0; i < 2400 && !eyes(r).isEmpty(); i++) {
            for (RoomEntity e : eyes(r)) {
                if (e.status() == EntityStatus.ACTIVE) {
                    int before = r.entityState.enemyHealth[e.slot()];
                    sword(r, e, false);
                    if (r.entityState.enemyHealth[e.slot()] < before) hits++;
                }
            }
            tick(r);
        }
        assertTrue(hits >= 2);
        assertTrue(eyes(r).isEmpty());
        assertEquals(
                1,
                r.inspection().snapshot().slots().stream()
                        .filter(e -> e.loaded() && e.type() == 0x36)
                        .count());
    }

    @Test
    void writesIsolatedProductionHandlerTracesForEmulatorComparison() throws Exception {
        var directory = java.nio.file.Path.of("build/slime-eye");
        java.nio.file.Files.createDirectories(directory);
        String header = "frame\tstatus\tstate\tx\ty\tz\tspeedZ\tcountdown\tvariant\tprivate4\tslow\thealth\n";
        for (String name : List.of("body-drop", "half-rise-drop", "half-death")) {
            var r = runtime();
            tick(r);
            boolean half = !name.equals("body-drop");
            boolean dead = name.equals("half-death");
            // Disclosed isolated comparison setup: preset handler state, not a D3 route.
            set(r, "state", dead ? 0 : half ? 2 : 1);
            set(r, "half", half ? 1 : 0);
            r.entityState.enemyTransitionCountdown[0] = 0;
            r.entityState.enemyHealth[0] = dead ? 0 : half ? 5 : 0x50;
            r.slots[0] = EntitySnapshots.withZ(
                    EntitySnapshots.withStatus(r.slots[0], dead ? EntityStatus.DYING : EntityStatus.ACTIVE),
                    half ? 0 : 0x7E);
            StringBuilder trace = new StringBuilder(header);
            for (int i = 0; i < 33; i++) r.entityState.bossIntroMotion.advance(0x90, 0x5B, 2, 0);
            frame = 39;
            for (int i = 0; i < (dead ? 226 : 145); i++) {
                tick(r);
                var e = r.slots[0];
                // Cleared ROM slots retain position and state bytes; the engine disables its value.
                trace.append(frame & 255)
                        .append('\t')
                        .append(e.status().value())
                        .append('\t')
                        .append(r.slimeEye.state(0))
                        .append('\t')
                        .append(e.x())
                        .append('\t')
                        .append(e.y())
                        .append('\t')
                        .append(e.z())
                        .append('\t')
                        .append(r.slimeEye.speedZ(0))
                        .append('\t')
                        .append(r.entityState.enemyTransitionCountdown[0])
                        .append('\t')
                        .append(r.slimeEye.variant(0))
                        .append('\t')
                        .append(r.slimeEye.dropping(0))
                        .append('\t')
                        .append(r.entityState.slowTransitionCountdown[0])
                        .append('\t')
                        .append(r.entityState.enemyHealth[0])
                        .append('\n');
            }
            java.nio.file.Files.writeString(directory.resolve(name + "-engine.tsv"), trace);
        }
    }

    private void set(RoomEntityRuntime r, String field, int value) throws Exception {
        var reflected = SlimeEyeController.class.getDeclaredField(field);
        reflected.setAccessible(true);
        ((int[]) reflected.get(r.slimeEye))[0] = value;
    }

    @Test
    void roomFacadeLoadsActualD3BossRoomAndRendersPlayableSplit() throws Exception {
        byte[] bytes = rom();
        var gpu = new linksawakening.gpu.GPU();
        gpu.loadBaseTiles(bytes);
        var session = new linksawakening.world.room.RoomSession(
                bytes,
                gpu,
                new linksawakening.world.room.RoomLoader(bytes),
                new linksawakening.world.room.OverworldTilesetTable(bytes),
                new linksawakening.physics.OverworldCollision(RomTables.loadFromRom(bytes)),
                null,
                null);
        session.loadIndoor(2, 0x5A);
        for (int i = 0; i < 2; i++) session.tickEntitiesWithProjectileEvents(i, 80, 96, 0, 0, 0, false);
        session.setEntityScreenShakeCountdown(5);
        session.tickEntitiesWithProjectileEvents(2, 80, 96, 0, 0, 0, false);
        session.setEntityScreenShakeCountdown(0);
        for (int i = 3; i < 160; i++) session.tickEntitiesWithProjectileEvents(i & 255, 80, 96, 0, 0, 0, false);
        capture(session, gpu, "body.png");
        captureFixture(session, gpu, false);
        // Continuous normal blade collision, followed by a Pegasus strike, uses the live facade.
        for (int i = 160; i < 1100; i++) {
            var eyes = session.activeRoom().entities().loadedEntities().stream()
                    .filter(e -> e.type() == 0x5B)
                    .toList();
            if (eyes.size() == 2) break;
            var eye = eyes.getFirst();
            boolean dash = i % 200 >= 160;
            session.resolveEntityCombat(
                    i & 255,
                    80,
                    96,
                    false,
                    true,
                    true,
                    eye.x() + 8,
                    4,
                    eye.y() - eye.z() + 8,
                    4,
                    new EnemyAttackContext(1, false, false, false, dash));
            session.tickEntitiesWithProjectileEvents(i & 255, 80, 96, 0, 0, 0, false);
        }
        assertEquals(
                2,
                session.activeRoom().entities().loadedEntities().stream()
                        .filter(e -> e.type() == 0x5B)
                        .count());
        for (int i = 0; i < 20; i++) session.tickEntitiesWithProjectileEvents(i, 80, 96, 0, 0, 0, false);
        capture(session, gpu, "halves.png");
        captureFixture(session, gpu, true);
        for (int i = 0; i < 2400; i++) {
            var eyes = session.activeRoom().entities().loadedEntities().stream()
                    .filter(e -> e.type() == 0x5B)
                    .toList();
            if (eyes.isEmpty()) break;
            for (var eye : eyes)
                session.resolveEntityCombat(
                        i & 255,
                        80,
                        96,
                        false,
                        true,
                        true,
                        eye.x() + 8,
                        4,
                        eye.y() - eye.z() + 8,
                        4,
                        EnemyAttackContext.standard());
            session.tickEntitiesWithProjectileEvents(i & 255, 80, 96, 0, 0, 0, false);
        }
        assertEquals(
                0,
                session.activeRoom().entities().loadedEntities().stream()
                        .filter(e -> e.type() == 0x5B)
                        .count());
        assertEquals(
                1,
                session.activeRoom().entities().loadedEntities().stream()
                        .filter(e -> e.type() == 0x36)
                        .count());
        capture(session, gpu, "defeated.png");
    }

    private void capture(linksawakening.world.room.RoomSession session, linksawakening.gpu.GPU gpu, String name)
            throws Exception {
        captureSnapshot(session.renderSnapshot(), gpu, name);
    }

    private void captureFixture(linksawakening.world.room.RoomSession session, linksawakening.gpu.GPU gpu, boolean half)
            throws Exception {
        var room = session.renderSnapshot();
        var catalog = new EntitySpriteHandlerCatalog(rom());
        var slots = new ArrayList<RoomEntity>();
        for (int i = 0; i < 16; i++) slots.add(RoomEntity.disabled(i));
        int[] xs = half ? new int[] {54, 90} : new int[] {72};
        for (int i = 0; i < xs.length; i++)
            slots.set(
                    i,
                    new RoomEntity(
                            i,
                            -1,
                            0x5B,
                            xs[i],
                            64,
                            EntityStatus.ACTIVE,
                            catalog.forSlimeEyeState(half, 0, 0),
                            0,
                            0,
                            0,
                            0,
                            -1,
                            false));
        var entities = new RoomEntitySnapshot(
                slots, room.entities().spriteSelection(), room.entities().spriteTiles());
        captureSnapshot(
                new linksawakening.world.room.RoomRenderSnapshot(
                        room.tileIds(), room.tileAttrs(), room.palettes(), entities),
                gpu,
                half ? "halves-fixture.png" : "body-fixture.png");
    }

    private void captureSnapshot(
            linksawakening.world.room.RoomRenderSnapshot snapshot, linksawakening.gpu.GPU gpu, String name)
            throws Exception {
        byte[] pixels = new byte[160 * 144 * 4];
        new linksawakening.render.GameFrameSceneBuilder()
                .build(linksawakening.render.GameFrameState.empty()
                        .withScreen(linksawakening.render.RenderScreen.OVERWORLD)
                        .withRoom(
                                snapshot,
                                new linksawakening.world.transition.ScrollController(),
                                new linksawakening.world.transition.TransitionController()))
                .drawTo(pixels, gpu);
        var image = new java.awt.image.BufferedImage(160, 144, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 144; y++)
            for (int x = 0; x < 160; x++) {
                int offset = (y * 160 + x) * 4;
                image.setRGB(
                        x,
                        y,
                        0xff000000
                                | ((pixels[offset] & 255) << 16)
                                | ((pixels[offset + 1] & 255) << 8)
                                | (pixels[offset + 2] & 255));
            }
        var directory = java.nio.file.Path.of("build/slime-eye");
        java.nio.file.Files.createDirectories(directory);
        javax.imageio.ImageIO.write(image, "png", directory.resolve(name).toFile());
    }

    @Test
    void romArrowImmunityAndLethalSwordReenterHalfDeathHandlerThenClearSlotState() throws Exception {
        var r = runtime();
        tick(r);
        set(r, "half", 1);
        set(r, "state", 1);
        r.slots[0] = EntitySnapshots.withZ(r.slots[0], 0);
        r.entityState.enemyHealth[0] = 1;
        assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0x5B));
        r.projectiles.applyPlayerProjectileDamage(r.slots[0], EntityRomConstants.DAMAGE_TYPE_ARROW, 0x20, 0);
        assertEquals(EntityStatus.ACTIVE, r.slots[0].status());
        assertEquals(1, r.entityState.enemyHealth[0]);
        r.entityState.enemyTransitionCountdown[0] = 0;
        sword(r, r.slots[0], false);
        assertEquals(EntityStatus.DYING, r.slots[0].status());
        tick(r); // active handler's lethal collision frame
        tick(r); // first destruction handler visit
        assertEquals(1, r.slimeEye.state(0));
        assertEquals(0x40, r.entityState.enemyTransitionCountdown[0]);
        r.lifecycle.disableEntityWithoutPersistence(0);
        assertFalse(r.slimeEye.half(0));
        assertEquals(0, r.slimeEye.state(0));
    }

    @Test
    void halfPegasusHitClearsBootsBeforeDamageAndRecoilSkipsHopIntegration() throws Exception {
        var r = runtime();
        tick(r);
        set(r, "half", 1);
        set(r, "state", 1);
        r.slots[0] = EntitySnapshots.withZ(r.slots[0], 1);
        r.entityState.enemyTransitionCountdown[0] = 0;
        r.entityState.enemyHealth[0] = 5;
        var e = r.slots[0];
        var events = r.resolveCombat(
                frame,
                80,
                88,
                false,
                true,
                true,
                e.x() + 8,
                4,
                e.y() - e.z() + 8,
                4,
                new EnemyAttackContext(1, false, false, false, true));
        assertEquals(4, r.entityState.enemyHealth[0], "6DDF clears boots before damage type is selected");
        assertEquals(0x10, events.getFirst().linkCollisionResponse().ignoreCollisionCountdown());
        assertTrue(events.getFirst().linkCollisionResponse().active());
        tick(r);
        int z = r.slots[0].z();
        assertEquals(
                0x18, r.entityState.enemyFlashCountdown[0], "Fresh hit flash is not aged before its source call site");
        for (int i = 0; i < 10; i++) {
            tick(r);
            assertEquals(z, r.slots[0].z(), "04:6DC8 discards the half handler return during recoil");
        }
        assertEquals(0, r.entityState.enemyIgnoreHitsCountdown[0]);
    }

    @Test
    void ordinaryHalfSwordHitRequestsFourFrameLinkPush() throws Exception {
        var r = runtime();
        tick(r);
        set(r, "half", 1);
        set(r, "state", 0);
        r.slots[0] = EntitySnapshots.withZ(r.slots[0], 0);
        r.entityState.enemyHealth[0] = 5;
        var e = r.slots[0];
        var events = r.resolveCombat(
                frame, 80, 88, false, true, true, e.x() + 8, 4, e.y() + 8, 4, EnemyAttackContext.standard());
        assertEquals(4, events.getFirst().linkCollisionResponse().ignoreCollisionCountdown());
        assertEquals(1, events.getFirst().enemyDamage());
    }

    @Test
    void romDisplayListsPreserveBodyStretchHalfAndShadowOffsets() throws Exception {
        var catalog = new EntitySpriteHandlerCatalog(rom());
        var full = catalog.forSlimeEyeState(false, 0, 0).dynamicVariant(0);
        assertEquals(12, full.size());
        assertEquals(-16, full.getFirst().xOffset());
        assertEquals(0x60, full.getFirst().oam().tile());
        assertEquals(3, full.getFirst().oam().attributes());
        assertEquals(16, catalog.forSlimeEyeState(false, 6, 0).dynamicVariant(0).size());
        var half = catalog.forSlimeEyeState(true, 1, 0x20).dynamicVariant(0);
        assertEquals(12, half.size());
        assertEquals(0x68, half.getFirst().oam().tile());
        assertEquals(0x2A, half.get(8).yOffset());
    }
}
