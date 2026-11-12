package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;

/** Ordered writes at one of the original Link consumption boundaries. */
public sealed interface LinkWrite {
    record PlatformWrite(SideViewPlatformLinkRequest request) implements LinkWrite {}

    record FinalPositionWrite(LinkFinalPositionRequest request) implements LinkWrite {}

    record PotWrite(SideViewPotLinkRequest request) implements LinkWrite {}

    record AttackClearWrite(LinkAttackClearRequest request) implements LinkWrite {}

    record AttackStepWrite(LinkAttackStepRequest request) implements LinkWrite {}
}
