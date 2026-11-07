package linksawakening.world;

/** Installs family callbacks at phase 2 dispatch sites. */
final class EntityPhase2Handlers {
  static void install(EntityHandlerRegistry registry) {
    ChestWithItemHandler.installPhase2(registry);
    ZombieHandler.installPhase2(registry);
    ChestWithItemHandler.installPhase2Part2(registry);
    DroppableSecretSeashellHandler.installPhase2(registry);
    HidingSlimeKeyHandler.installPhase2(registry);
    HidingSlimeKeyHandler.installPhase2Part2(registry);
    KeyDropPointHandler.installPhase2(registry);
    WizrobeHandler.installPhase2(registry);
    PolsVoiceHandler.installPhase2(registry);
    MiniMoldormHandler.installPhase2(registry);
    MoldormHandler.installPhase2(registry);
    MimicHandler.installPhase2(registry);
    MaskedMimicGoriyaHandler.installPhase2(registry);
    CrystalSwitchHandler.installPhase2(registry);
    LeeverHandler.installPhase2(registry);
    BomberHandler.installPhase2(registry);
    MadBomberHandler.installPhase2(registry);
    SideViewPotHandler.installPhase2(registry);
    RoosterHandler.installPhase2(registry);
    BombHandler.installPhase2(registry);
    ColorShellTypeHandler.installPhase2(registry);
    MoblinKingHandler.installPhase2(registry);
    GenieHandler.installPhase2(registry);
    RollingBonesHandler.installPhase2(registry);
    MoldormHandler.installPhase2Part2(registry);
    BombHandler.installPhase2Part2(registry);
    HidingSlimeKeyHandler.installPhase2Part3(registry);
    MoblinHandler.installPhase2(registry);
    MoblinSwordHandler.installPhase2(registry);
    SparkTypeHandler.installPhase2(registry);
    ZolGelTypeHandler.installPhase2(registry);
    MiniMoldormHandler.installPhase2Part2(registry);
    MoldormHandler.installPhase2Part3(registry);
    WizrobeHandler.installPhase2Part2(registry);
    WizrobeProjectileHandler.installPhase2(registry);
    GhiniTypeHandler.installPhase2(registry);
    BombiteTypeHandler.installPhase2(registry);
    FollowingNpcTypeHandler.installPhase2(registry);
    ColorShellTypeHandler.installPhase2Part2(registry);
    SideViewPlatformHandler.installPhase2(registry);
    CommonDroppableTypeHandler.installPhase2(registry);
    CommonDroppableTypeHandler.installPhase2Part2(registry);
    RollingBonesBarHandler.installPhase2(registry);
    SparkTypeHandler.installPhase2Part2(registry);
  }
}
