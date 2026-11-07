package linksawakening.world;

/** Installs family callbacks at phase 3 dispatch sites. */
final class EntityPhase3Handlers {
  static void install(EntityHandlerRegistry registry) {
    IronMaskHandler.installPhase3(registry);
    MoblinSwordHandler.installPhase3(registry);
    HidingZolHandler.installPhase3(registry);
    SpikeTrapHandler.installPhase3(registry);
    PairoddHandler.installPhase3(registry);
    StarHandler.installPhase3(registry);
    BlooperHandler.installPhase3(registry);
    PincerHandler.installPhase3(registry);
    WingedOctorokHandler.installPhase3(registry);
    BushCrawlerHandler.installPhase3(registry);
    PairoddProjectileHandler.installPhase3(registry);
    WizrobeProjectileHandler.installPhase3(registry);
    GopongaFlowerProjectileHandler.installPhase3(registry);
    WaterTektiteHandler.installPhase3(registry);
    FishHandler.installPhase3(registry);
    CrowHandler.installPhase3(registry);
    CuccoHandler.installPhase3(registry);
    BooBuddyHandler.installPhase3(registry);
    DroppableFairyHandler.installPhase3(registry);
    StalfosAggressiveHandler.installPhase3(registry);
    StalfosEvasiveHandler.installPhase3(registry);
    ThreeOfAKindHandler.installPhase3(registry);
    LikeLikeHandler.installPhase3(registry);
    GoombaHandler.installPhase3(registry);
    SnakeHandler.installPhase3(registry);
    WizrobeHandler.installPhase3(registry);
    PeahatHandler.installPhase3(registry);
    ArmosStatueHandler.installPhase3(registry);
    BombiteTypeHandler.installPhase3(registry);
    RoamingEnemyTypeHandler.installPhase3(registry);
    SparkTypeHandler.installPhase3(registry);
    ZolGelTypeHandler.installPhase3(registry);
    EnemyProjectileTypeHandler.installPhase3(registry);
  }
}
