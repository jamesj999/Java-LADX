package linksawakening.world;

/** Installs family callbacks at phase 4 dispatch sites. */
final class EntityPhase4Handlers {
  static void install(EntityHandlerRegistry registry) {
    GenieHandler.installPhase4(registry);
    ArmosKnightHandler.installPhase4(registry);
    HinoxHandler.installPhase4(registry);
    RollingBonesHandler.installPhase4(registry);
    RollingBonesBarHandler.installPhase4(registry);
    MoblinKingHandler.installPhase4(registry);
    GopongaFlowerHandler.installPhase4(registry);
    GiantGopongaFlowerHandler.installPhase4(registry);
    PokeyHandler.installPhase4(registry);
    PiranhaPlantHandler.installPhase4(registry);
    ZoraHandler.installPhase4(registry);
    ZombieHandler.installPhase4(registry);
    BuzzBlobHandler.installPhase4(registry);
    SandCrabHandler.installPhase4(registry);
    DogHandler.installPhase4(registry);
    WitchHandler.installPhase4(registry);
    UrchinHandler.installPhase4(registry);
    WitchRatHandler.installPhase4(registry);
    HardhatBeetleHandler.installPhase4(registry);
    PolsVoiceHandler.installPhase4(registry);
    SpikedBeetleHandler.installPhase4(registry);
    TarinHandler.installPhase4(registry);
    GhiniTypeHandler.installPhase4(registry);
  }
}
