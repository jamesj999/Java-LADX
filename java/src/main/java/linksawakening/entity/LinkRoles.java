package linksawakening.entity;
/** Retained role composition for LinkMotionDefault and its presentation and interaction routines. */
final class LinkRoles {
    private final LinkSideViewRole sideView;

    LinkSideViewRole sideView() {
        return sideView;
    }

    private final LinkMotionRole motion;

    LinkMotionRole motion() {
        return motion;
    }

    private final LinkStatusRole status;

    LinkStatusRole status() {
        return status;
    }

    private final LinkEquipmentRole equipment;

    LinkEquipmentRole equipment() {
        return equipment;
    }

    private final LinkAnimationRole animation;

    LinkAnimationRole animation() {
        return animation;
    }

    private final LinkActionsRole actions;

    LinkActionsRole actions() {
        return actions;
    }

    private final LinkPositionRole position;

    LinkPositionRole position() {
        return position;
    }

    private final LinkAirborneRole airborne;

    LinkAirborneRole airborne() {
        return airborne;
    }

    private final LinkControlsRole controls;

    LinkControlsRole controls() {
        return controls;
    }

    LinkRoles(LinkAssembly assembly) {
        sideView = new DefaultLinkSideViewRole(assembly);
        motion = new DefaultLinkMotionRole(assembly);
        status = new DefaultLinkStatusRole(assembly);
        equipment = new DefaultLinkEquipmentRole(assembly);
        animation = new DefaultLinkAnimationRole(assembly);
        actions = new DefaultLinkActionsRole(assembly);
        position = new DefaultLinkPositionRole(assembly);
        airborne = new DefaultLinkAirborneRole(assembly);
        controls = new DefaultLinkControlsRole(assembly);
    }
}
