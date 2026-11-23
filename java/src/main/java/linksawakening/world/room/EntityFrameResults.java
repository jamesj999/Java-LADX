package linksawakening.world.room;

/** Frame outbox views. Drain at the original consumer boundaries: Link writes may load a room. */
public record EntityFrameResults(LinkOutputs link, RewardOutputs rewards, PresentationOutputs presentation) {}
