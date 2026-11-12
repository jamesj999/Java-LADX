package linksawakening.rom;
/** Compatibility aggregate of immutable, themed ROM tables. */
public final class RomTables {
    public static final int PHYSICS_TABLE_OVERWORLD = 0;
    public static final int PHYSICS_TABLE_INDOORS1 = 1;
    public static final int PHYSICS_TABLE_INDOORS2 = 2;
    private final PhysicsTables physicsTables;
    private final EntityPropertyTables entityPropertyTables;
    private final CollisionTables collisionTables;
    private final LinkMotionTables linkMotionTables;
    private final SwordTables swordTables;
    private final EquipmentTables equipmentTables;

    private RomTables(Rom rom) {
        physicsTables = PhysicsTables.load(rom);
        entityPropertyTables = EntityPropertyTables.load(rom);
        collisionTables = CollisionTables.load(rom);
        linkMotionTables = LinkMotionTables.load(rom);
        swordTables = SwordTables.load(rom);
        equipmentTables = EquipmentTables.load(rom);
    }

    public static RomTables load(Rom rom) {
        return new RomTables(rom);
    }
    /** @deprecated Use {@link #load(Rom)}. */
    @Deprecated(forRemoval = true)
    public static RomTables loadFromRom(byte[] romData) {
        return load(Rom.unchecked(romData));
    }

    public PhysicsTables physicsTables() {
        return physicsTables;
    }
    /** @deprecated Use {@link #physicsTables()}. */
    @Deprecated(forRemoval = true)
    public int objectPhysicsFlag(int tableIndex, int objectId) {
        return physicsTables.objectPhysicsFlag(tableIndex, objectId);
    }

    public EntityPropertyTables entityPropertyTables() {
        return entityPropertyTables;
    }
    /** @deprecated Use {@link #entityPropertyTables()}. */
    @Deprecated(forRemoval = true)
    public int entityOptions1(int entityType) {
        return entityPropertyTables.entityOptions1(entityType);
    }

    public CollisionTables collisionTables() {
        return collisionTables;
    }

    /** @deprecated Use {@link #collisionTables()}. */
    @Deprecated(forRemoval = true)
    public int linkFineCollisionShape(int physicsFlag, int quadrant) {
        return collisionTables.linkFineCollisionShape(physicsFlag, quadrant);
    }

    public LinkMotionTables linkMotionTables() {
        return linkMotionTables;
    }

    /** @deprecated Use {@link #linkMotionTables()}. */
    @Deprecated(forRemoval = true)
    public int linkSpeedX(int joypadMask) {
        return linkMotionTables.linkSpeedX(joypadMask);
    }
    /** @deprecated Use {@link #linkMotionTables()}. */
    @Deprecated(forRemoval = true)
    public int airSpeedX(int axisMask) {
        return linkMotionTables.airSpeedX(axisMask);
    }
    /** @deprecated Use {@link #linkMotionTables()}. */
    @Deprecated(forRemoval = true)
    public int airSpeedY(int axisMask) {
        return linkMotionTables.airSpeedY(axisMask);
    }
    /** @deprecated Use {@link #linkMotionTables()}. */
    @Deprecated(forRemoval = true)
    public int linkSpeedY(int joypadMask) {
        return linkMotionTables.linkSpeedY(joypadMask);
    }

    public SwordTables swordTables() {
        return swordTables;
    }
    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordDirectionAnimState(int romDirection, int swordState) {
        return swordTables.swordDirectionAnimState(romDirection, swordState);
    }
    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordDirectionFor(int romDirection, int swordState) {
        return swordTables.swordDirectionFor(romDirection, swordState);
    }
    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordSpinAnimationState(int baseRomDirection, int sector) {
        return swordTables.swordSpinAnimationState(baseRomDirection, sector);
    }
    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordSpinAbsoluteDirection(int baseRomDirection, int sector) {
        return swordTables.swordSpinAbsoluteDirection(baseRomDirection, sector);
    }
    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordBladeXOffset(int romDirection, int swordState) {
        return swordTables.swordBladeXOffset(romDirection, swordState);
    }
    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordBladeYOffset(int romDirection, int swordState) {
        return swordTables.swordBladeYOffset(romDirection, swordState);
    }

    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordCollisionXOriginOffset(int romDirection, int swordState) {
        return swordTables.swordCollisionXOriginOffset(romDirection, swordState);
    }

    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordCollisionYOriginOffset(int romDirection, int swordState) {
        return swordTables.swordCollisionYOriginOffset(romDirection, swordState);
    }

    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordSpriteTile(int swordDirection, int spriteSlot) {
        return swordTables.swordSpriteTile(swordDirection, spriteSlot);
    }
    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int swordSpriteAttr(int swordDirection, int spriteSlot) {
        return swordTables.swordSpriteAttr(swordDirection, spriteSlot);
    }
    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int staticSwordCollisionOffsetX(int collisionIndex) {
        return swordTables.staticSwordCollisionOffsetX(collisionIndex);
    }
    /** @deprecated Use {@link #swordTables()}. */
    @Deprecated(forRemoval = true)
    public int staticSwordCollisionOffsetY(int collisionIndex) {
        return swordTables.staticSwordCollisionOffsetY(collisionIndex);
    }

    public EquipmentTables equipmentTables() {
        return equipmentTables;
    }

    /** @deprecated Use {@link #equipmentTables()}. */
    @Deprecated(forRemoval = true)
    public int shovelAnimationState(int timer, int romDirection) {
        return equipmentTables.shovelAnimationState(timer, romDirection);
    }
}
