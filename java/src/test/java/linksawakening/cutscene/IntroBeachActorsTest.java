package linksawakening.cutscene;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.testing.TestRoms;
import org.junit.jupiter.api.Test;

class IntroBeachActorsTest {
    @Test
    void walkCadenceAndLinkLaunchResetAreIndependentOfSequenceOrchestration() {
        var rom = new IntroRomData(TestRoms.partial());
        var video = new IntroVideoMemory();
        var clock = new IntroClock();
        var water = new IntroWater(rom, video, clock, new int[8]);
        var actors = new IntroBeachActors(rom, video, water, clock);
        actors.tickMarin();
        assertEquals(0xAF, actors.marinX());
        for (int i = 0; i < 3; i++) actors.tickMarin();
        assertEquals(0xAF, actors.marinX());
        actors.tickMarin();
        assertEquals(0xAE, actors.marinX());
        actors.marinState(1);
        clock.clock(1);
        actors.tickMarin();
        assertTrue(actors.linkVisible());
        assertEquals(0, clock.clock());
        assertEquals(0xFE, actors.linkX());
        assertFalse(actors.tickInertLink());
        assertEquals(255, actors.linkTimer());
    }

    @Test
    void finalLinkCountdownRequestsTitleOnlyOnTheSourceClockGate() {
        var rom = new IntroRomData(TestRoms.partial());
        var video = new IntroVideoMemory();
        var clock = new IntroClock();
        var actors = new IntroBeachActors(rom, video, new IntroWater(rom, video, clock, new int[8]), clock);
        actors.linkState(3);
        actors.linkTimer(1);
        clock.clock(1);
        assertFalse(actors.tickInertLink());
        assertEquals(1, actors.linkTimer());
        clock.clock(4);
        assertTrue(actors.tickInertLink());
        assertEquals(0, actors.linkTimer());
    }
}
