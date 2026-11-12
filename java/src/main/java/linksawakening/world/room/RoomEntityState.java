package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;

public interface RoomEntityState {
    void resetGuardianAcornCounterOnHurt();

    void setEntityActionButtonsHeld(boolean actionButtonsHeld);

    boolean gelClingingToLink();

    void setEntityDialogActive(boolean active);

    void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY);

    void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld);

    void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount);

    void setEntityScreenShakeCountdown(int countdown);

    void setSecretSeashellPegasusCollisionState(
            boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY);

    void setEntityPowerBraceletButtonHeld(boolean buttonHeld);

    void setEntityBombButtonHeld(boolean buttonHeld);

    void setEntityAttackStepAnimationCountdown(int countdown);

    void advanceBoomerangSoundClock(boolean worldGameplay);

    boolean deferContactRecoil(int slot, int linkX, int linkY);

    List<LinkAttackClearRequest> consumeLinkAttackClearRequests();

    boolean gotItemPresentationActive();

    boolean powerUpPickupSequenceActive();

    boolean swordPickupSequenceActive();

    List<SwordPickupRewardEvent> consumeSwordPickupRewards();

    List<DialogRequest> consumeEntityDialogRequests();

    void tickGameplayVBlank();

    int clearEntity(int slot);

    int entityDefaultMusicTrack();

    int entityHealth(int slot);

    void setColorShellSoundSink(GameplaySoundSink soundSink);
}
