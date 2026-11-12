package linksawakening.entity;

/** Constructs the Link component graph before publishing any behavior. */
final class LinkAssembly {
    final LinkComponentRef<LinkAnimator> linkAnimator = new LinkComponentRef<>();
    final LinkComponentRef<LinkSideView> linkSideView = new LinkComponentRef<>();
    final LinkComponentRef<LinkStateMachine> linkStateMachine = new LinkComponentRef<>();
    final LinkComponentRef<LinkEquipment> linkEquipment = new LinkComponentRef<>();
    final LinkComponentRef<LinkEntityEffects> linkEntityEffects = new LinkComponentRef<>();
    final LinkComponentRef<LinkMotion> linkMotion = new LinkComponentRef<>();
    final LinkComponentRef<LinkAirborne> linkAirborne = new LinkComponentRef<>();
    final LinkComponentRef<LinkSwimming> linkSwimming = new LinkComponentRef<>();
    final LinkComponentRef<LinkDamage> linkDamage = new LinkComponentRef<>();
    final LinkComponentRef<LinkAnimationState> linkAnimationState = new LinkComponentRef<>();
    final LinkComponentRef<LinkItemState> linkItemState = new LinkComponentRef<>();
    final LinkComponentRef<LinkGroundMotion> linkGroundMotion = new LinkComponentRef<>();
    final LinkComponentRef<LinkSignals> linkSignals = new LinkComponentRef<>();
    final LinkComponentRef<LinkRecovery> linkRecovery = new LinkComponentRef<>();
    final LinkComponentRef<LinkServices> linkServices = new LinkComponentRef<>();
    final LinkComponentRef<LinkControls> linkControls = new LinkComponentRef<>();
    final LinkComponentRef<DialogGotItemSequence> dialogGotItemSequence = new LinkComponentRef<>();

    LinkAssembly(LinkServices services) {
        bindGroup0(services);
        bindGroup1(services);
        bindGroup2(services);
        bindGroup3(services);
        bindGroup4(services);
    }

    private void bindGroup0(LinkServices services) {

        LinkAnimator.Dependencies linkAnimatorDependencies = new LinkAnimator.Dependencies();
        linkAnimatorDependencies.dialogGotItemSequence = dialogGotItemSequence;
        linkAnimatorDependencies.linkAirborne = linkAirborne;
        linkAnimatorDependencies.linkAnimationState = linkAnimationState;
        linkAnimatorDependencies.linkDamage = linkDamage;
        linkAnimatorDependencies.linkEntityEffects = linkEntityEffects;
        linkAnimatorDependencies.linkEquipment = linkEquipment;
        linkAnimatorDependencies.linkItemState = linkItemState;
        linkAnimatorDependencies.linkMotion = linkMotion;
        linkAnimatorDependencies.linkServices = linkServices;
        linkAnimatorDependencies.linkStateMachine = linkStateMachine;
        linkAnimator.bind(new LinkAnimator(linkAnimatorDependencies));
        LinkSideView.Dependencies linkSideViewDependencies = new LinkSideView.Dependencies();
        linkSideViewDependencies.linkAirborne = linkAirborne;
        linkSideViewDependencies.linkAnimationState = linkAnimationState;
        linkSideViewDependencies.linkAnimator = linkAnimator;
        linkSideViewDependencies.linkControls = linkControls;
        linkSideViewDependencies.linkDamage = linkDamage;
        linkSideViewDependencies.linkEquipment = linkEquipment;
        linkSideViewDependencies.linkMotion = linkMotion;
        linkSideViewDependencies.linkServices = linkServices;
        linkSideViewDependencies.linkStateMachine = linkStateMachine;
        linkSideView.bind(new LinkSideView(linkSideViewDependencies));
        LinkStateMachine.Dependencies linkStateMachineDependencies = new LinkStateMachine.Dependencies();
        linkStateMachineDependencies.dialogGotItemSequence = dialogGotItemSequence;
        linkStateMachineDependencies.linkAirborne = linkAirborne;
        linkStateMachineDependencies.linkAnimationState = linkAnimationState;
        linkStateMachineDependencies.linkAnimator = linkAnimator;
        linkStateMachineDependencies.linkControls = linkControls;
        linkStateMachineDependencies.linkDamage = linkDamage;
        linkStateMachineDependencies.linkEntityEffects = linkEntityEffects;
        linkStateMachineDependencies.linkEquipment = linkEquipment;
        linkStateMachineDependencies.linkGroundMotion = linkGroundMotion;
        linkStateMachineDependencies.linkItemState = linkItemState;
        linkStateMachineDependencies.linkMotion = linkMotion;
        linkStateMachineDependencies.linkServices = linkServices;
        linkStateMachineDependencies.linkSideView = linkSideView;
        linkStateMachineDependencies.linkSignals = linkSignals;
        linkStateMachineDependencies.linkSwimming = linkSwimming;
        linkStateMachine.bind(new LinkStateMachine(linkStateMachineDependencies));
        LinkEquipment.Dependencies linkEquipmentDependencies = new LinkEquipment.Dependencies();
        linkEquipmentDependencies.linkAirborne = linkAirborne;
        linkEquipmentDependencies.linkAnimationState = linkAnimationState;
        linkEquipmentDependencies.linkControls = linkControls;
        linkEquipmentDependencies.linkEntityEffects = linkEntityEffects;
        linkEquipmentDependencies.linkItemState = linkItemState;
        linkEquipmentDependencies.linkServices = linkServices;
        linkEquipmentDependencies.linkStateMachine = linkStateMachine;
        linkEquipment.bind(new LinkEquipment(linkEquipmentDependencies));
    }

    private void bindGroup1(LinkServices services) {
        LinkEntityEffects.Dependencies linkEntityEffectsDependencies = new LinkEntityEffects.Dependencies();
        linkEntityEffectsDependencies.linkAirborne = linkAirborne;
        linkEntityEffectsDependencies.linkAnimationState = linkAnimationState;
        linkEntityEffectsDependencies.linkAnimator = linkAnimator;
        linkEntityEffectsDependencies.linkDamage = linkDamage;
        linkEntityEffectsDependencies.linkGroundMotion = linkGroundMotion;
        linkEntityEffectsDependencies.linkItemState = linkItemState;
        linkEntityEffectsDependencies.linkMotion = linkMotion;
        linkEntityEffectsDependencies.linkServices = linkServices;
        linkEntityEffectsDependencies.linkStateMachine = linkStateMachine;
        linkEntityEffectsDependencies.linkSwimming = linkSwimming;
        linkEntityEffects.bind(new LinkEntityEffects(linkEntityEffectsDependencies));
        LinkMotion.Dependencies linkMotionDependencies = new LinkMotion.Dependencies();
        linkMotionDependencies.linkAirborne = linkAirborne;
        linkMotionDependencies.linkAnimationState = linkAnimationState;
        linkMotionDependencies.linkAnimator = linkAnimator;
        linkMotionDependencies.linkControls = linkControls;
        linkMotionDependencies.linkDamage = linkDamage;
        linkMotionDependencies.linkEntityEffects = linkEntityEffects;
        linkMotionDependencies.linkEquipment = linkEquipment;
        linkMotionDependencies.linkGroundMotion = linkGroundMotion;
        linkMotionDependencies.linkItemState = linkItemState;
        linkMotionDependencies.linkServices = linkServices;
        linkMotionDependencies.linkSideView = linkSideView;
        linkMotionDependencies.linkStateMachine = linkStateMachine;
        linkMotion.bind(new LinkMotion(linkMotionDependencies));
        LinkAirborne.Dependencies linkAirborneDependencies = new LinkAirborne.Dependencies();
        linkAirborneDependencies.dialogGotItemSequence = dialogGotItemSequence;
        linkAirborneDependencies.linkAnimationState = linkAnimationState;
        linkAirborneDependencies.linkAnimator = linkAnimator;
        linkAirborneDependencies.linkControls = linkControls;
        linkAirborneDependencies.linkDamage = linkDamage;
        linkAirborneDependencies.linkEntityEffects = linkEntityEffects;
        linkAirborneDependencies.linkItemState = linkItemState;
        linkAirborneDependencies.linkMotion = linkMotion;
        linkAirborneDependencies.linkServices = linkServices;
        linkAirborneDependencies.linkSideView = linkSideView;
        linkAirborneDependencies.linkSignals = linkSignals;
        linkAirborneDependencies.linkStateMachine = linkStateMachine;
        linkAirborneDependencies.linkSwimming = linkSwimming;
        linkAirborne.bind(new LinkAirborne(linkAirborneDependencies));
        LinkSwimming.Dependencies linkSwimmingDependencies = new LinkSwimming.Dependencies();
        linkSwimmingDependencies.linkAirborne = linkAirborne;
        linkSwimmingDependencies.linkAnimationState = linkAnimationState;
        linkSwimmingDependencies.linkAnimator = linkAnimator;
        linkSwimmingDependencies.linkControls = linkControls;
        linkSwimmingDependencies.linkEquipment = linkEquipment;
        linkSwimmingDependencies.linkMotion = linkMotion;
        linkSwimmingDependencies.linkServices = linkServices;
        linkSwimmingDependencies.linkSignals = linkSignals;
        linkSwimmingDependencies.linkStateMachine = linkStateMachine;
        linkSwimming.bind(new LinkSwimming(linkSwimmingDependencies));
    }

    private void bindGroup2(LinkServices services) {
        LinkDamage.Dependencies linkDamageDependencies = new LinkDamage.Dependencies();
        linkDamageDependencies.linkAirborne = linkAirborne;
        linkDamageDependencies.linkAnimationState = linkAnimationState;
        linkDamageDependencies.linkAnimator = linkAnimator;
        linkDamageDependencies.linkMotion = linkMotion;
        linkDamageDependencies.linkServices = linkServices;
        linkDamageDependencies.linkSideView = linkSideView;
        linkDamageDependencies.linkStateMachine = linkStateMachine;
        linkDamage.bind(new LinkDamage(linkDamageDependencies));
        LinkAnimationState.Dependencies linkAnimationStateDependencies = new LinkAnimationState.Dependencies();
        linkAnimationStateDependencies.linkAirborne = linkAirborne;
        linkAnimationStateDependencies.linkMotion = linkMotion;
        linkAnimationState.bind(new LinkAnimationState(linkAnimationStateDependencies));
        linkItemState.bind(new LinkItemState());
        LinkGroundMotion.Dependencies linkGroundMotionDependencies = new LinkGroundMotion.Dependencies();
        linkGroundMotionDependencies.linkAirborne = linkAirborne;
        linkGroundMotionDependencies.linkDamage = linkDamage;
        linkGroundMotionDependencies.linkMotion = linkMotion;
        linkGroundMotionDependencies.linkServices = linkServices;
        linkGroundMotionDependencies.linkSideView = linkSideView;
        linkGroundMotionDependencies.linkStateMachine = linkStateMachine;
        linkGroundMotion.bind(new LinkGroundMotion(linkGroundMotionDependencies));
    }

    private void bindGroup3(LinkServices services) {
        LinkSignals.Dependencies linkSignalsDependencies = new LinkSignals.Dependencies();
        linkSignalsDependencies.linkAirborne = linkAirborne;
        linkSignalsDependencies.linkStateMachine = linkStateMachine;
        linkSignals.bind(new LinkSignals(linkSignalsDependencies));
        LinkRecovery.Dependencies linkRecoveryDependencies = new LinkRecovery.Dependencies();
        linkRecoveryDependencies.linkAirborne = linkAirborne;
        linkRecoveryDependencies.linkAnimationState = linkAnimationState;
        linkRecoveryDependencies.linkAnimator = linkAnimator;
        linkRecoveryDependencies.linkDamage = linkDamage;
        linkRecoveryDependencies.linkMotion = linkMotion;
        linkRecoveryDependencies.linkServices = linkServices;
        linkRecoveryDependencies.linkStateMachine = linkStateMachine;
        linkRecovery.bind(new LinkRecovery(linkRecoveryDependencies));
        linkServices.bind(services);
        LinkControls.Dependencies linkControlsDependencies = new LinkControls.Dependencies();
        linkControlsDependencies.linkServices = linkServices;
        linkControls.bind(new LinkControls(linkControlsDependencies));
    }

    private void bindGroup4(LinkServices services) {
        dialogGotItemSequence.bind(new DialogGotItemSequence());
    }
}
