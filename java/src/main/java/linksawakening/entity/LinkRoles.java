package linksawakening.entity;
/** Retained role composition for LinkMotionDefault and its presentation and interaction routines. */
final class LinkRoles {
    private final LinkSideViewRole sideView;

    private final LinkMotionRole motion;

    private final LinkStatusRole status;

    private final LinkEquipmentRole equipment;

    private final LinkAnimationRole animation;

    private final LinkActionsRole actions;

    private final LinkPositionRole position;

    private final LinkAirborneRole airborne;

    private final LinkControlsRole controls;

    LinkRoles(LinkAssembly assembly) {
        sideView = new DefaultLinkSideViewRole(assembly);
        motion = new DefaultLinkMotionRole(assembly);
        status = new DefaultLinkStatusRole(assembly, motion);
        equipment = new DefaultLinkEquipmentRole(assembly);
        animation = new DefaultLinkAnimationRole(assembly);
        actions = new DefaultLinkActionsRole(assembly);
        position = new DefaultLinkPositionRole(assembly);
        airborne = new DefaultLinkAirborneRole(assembly);
        controls = new DefaultLinkControlsRole(assembly);
    }

    LinkSideViewRole sideView() {
        return sideView;
    }

    LinkMotionRole motion() {
        return motion;
    }

    LinkStatusRole status() {
        return status;
    }

    LinkEquipmentRole equipment() {
        return equipment;
    }

    LinkAnimationRole animation() {
        return animation;
    }

    LinkActionsRole actions() {
        return actions;
    }

    LinkPositionRole position() {
        return position;
    }

    LinkAirborneRole airborne() {
        return airborne;
    }

    LinkControlsRole controls() {
        return controls;
    }
}
