package linksawakening.world.room;

public record EntityTickInputs(
        int frameCounter, LinkPosition position, LinkMotion motion, int shieldLevel, SwordBox sword) {}
